package dev.sol.accesshub.data.repository

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import org.xmlpull.v1.XmlPullParser
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dev.sol.accesshub.data.ServiceMeta
import dev.sol.accesshub.shizuku.ShellResult
import dev.sol.accesshub.shizuku.ShizukuManager
import dev.sol.accesshub.shizuku.ShizukuShell

/** Result of reading the enabled set: the ids, the framework flag, and how it was read. */
data class EnabledState(
    val services: Set<String>,
    val accessibilityEnabled: Boolean,
    val viaShizuku: Boolean,
)

/**
 * Bridges the public accessibility APIs (enumeration + read-only state) with the privileged
 * write path through Shizuku (or Sui, which speaks the same API).
 *
 * - Enumeration uses [AccessibilityManager.getInstalledAccessibilityServiceList], which needs no
 *   special permission and already resolves labels / descriptions.
 * - Reading the enabled list uses the public `Settings.Secure` keys, so the UI still shows the
 *   real state in read-only mode.
 * - Writing REQUIRES the privileged binder (shell uid) because it calls `settings put secure`.
 */
class AccessibilityRepository(private val app: Application) {

    private val pm get() = app.packageManager
    private val am: AccessibilityManager? get() = app.getSystemService(AccessibilityManager::class.java)

    fun installedServices(): List<ServiceMeta> {
        // am.installedAccessibilityServiceList queries the system, but since Android 11 we also
        // need <queries> in AndroidManifest.xml to see their package info.
        val infos: List<AccessibilityServiceInfo> =
            runCatching { am?.installedAccessibilityServiceList }.getOrNull().orEmpty()

        return infos.mapNotNull { info ->
            val id = info.id ?: return@mapNotNull null
            val cn = ComponentName.unflattenFromString(id) ?: return@mapNotNull null
            val pkg = cn.packageName
            val appInfo = runCatching {
                pm.getApplicationInfo(pkg, PackageManager.GET_META_DATA)
            }.getOrNull()
            val appName = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg
            val icon = appInfo
                ?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() }
                ?.let { runCatching { it.toBitmap().asImageBitmap() }.getOrNull() }
            // AccessibilityServiceInfo has no loadLabel(); resolve it via its ResolveInfo.
            val label = runCatching { info.resolveInfo?.loadLabel(pm)?.toString() }.getOrNull()
                ?.takeIf { it.isNotBlank() } ?: appName
            val description = runCatching { info.loadDescription(pm) }.getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: readServiceDescriptionFromXml(pm, info, appInfo, pkg)
            val isSystemApp = appInfo?.let {
                (it.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0
            } ?: false
            ServiceMeta(
                id = id,
                pkg = pkg,
                cls = cn.className,
                label = label,
                appName = appName,
                description = description,
                icon = icon,
                isSystemApp = isSystemApp,
                settingsActivity = info.settingsActivityName?.takeIf { it.isNotBlank() },
            )
        }.sortedWith(compareBy({ it.appName.lowercase() }, { it.label.lowercase() }))
    }

    fun readEnabled(): EnabledState {
        val cr = app.contentResolver
        val raw = runCatching {
            Settings.Secure.getString(cr, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        }.getOrNull()
        val accessibilityEnabled = runCatching {
            Settings.Secure.getInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        }.getOrDefault(0) == 1
        return EnabledState(
            services = parseSet(raw.orEmpty()),
            accessibilityEnabled = accessibilityEnabled,
            viaShizuku = ShizukuManager.isReady,
        )
    }

    /** Writes the full enabled set; the framework then derives its own ACCESSIBILITY_ENABLED. */
    suspend fun writeEnabled(services: Set<String>): ShellResult {
        val value = services.joinToString(":")
        val script = buildString {
            append("settings put secure enabled_accessibility_services ")
            append(ShizukuShell.quote(value))
            append('\n')
            append("settings put secure accessibility_enabled ")
            append(if (services.isNotEmpty()) "1" else "0")
        }
        return ShizukuShell.exec(script)
    }

    private fun parseSet(raw: String): Set<String> =
        raw.split(':')
            .map { it.trim() }
            .filter { it.isNotEmpty() && it != "null" }
            .toSet()

    /**
     * [AccessibilityServiceInfo.loadDescription] can hand back nothing, so read
     * `android:description` straight out of the service's `android.accessibilityservice` XML, the
     * same place the framework takes it from.
     */
    private fun readServiceDescriptionFromXml(
        pm: PackageManager,
        info: AccessibilityServiceInfo,
        appInfo: ApplicationInfo?,
        pkg: String,
    ): String? {
        if (appInfo == null) return null
        val serviceInfo = info.resolveInfo?.serviceInfo ?: return null

        return runCatching {
            serviceInfo.loadXmlMetaData(pm, META_DATA_ACCESSIBILITY_SERVICE)?.use { parser ->
                var type = parser.eventType
                while (type != XmlPullParser.END_DOCUMENT) {
                    if (type == XmlPullParser.START_TAG && parser.name == TAG_ACCESSIBILITY_SERVICE) {
                        val resId =
                            parser.getAttributeResourceValue(ANDROID_NAMESPACE, "description", 0)
                        return@use if (resId == 0) {
                            null
                        } else {
                            pm.getText(pkg, resId, appInfo)?.toString()?.takeIf { it.isNotBlank() }
                        }
                    }
                    type = parser.next()
                }
                null
            }
        }.getOrNull()
    }

    private companion object {
        const val META_DATA_ACCESSIBILITY_SERVICE = "android.accessibilityservice"
        const val TAG_ACCESSIBILITY_SERVICE = "accessibility-service"
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
