package dev.sol.accesshub.ui.screen.home

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.core.content.pm.PackageInfoCompat

@Immutable
data class AppVersion(
    val versionName: String,
    val versionCode: Long,
)

@Immutable
data class SystemInfo(
    val appVersion: String = "-",
    val androidVersion: String = "-",
    val deviceModel: String = "-",
    val fingerprint: String = "-",
)

/**
 * What the manager knows about the accessibility configuration of this device.
 *
 * Everything here is readable without Shizuku: the installed services come from
 * [android.view.accessibility.AccessibilityManager] and the enabled set from the public
 * `Settings.Secure` keys. Writing them still requires the shell (Shizuku) path.
 *
 * Note that [accessibilityEnabled] is not a user facing switch: the framework derives
 * `Settings.Secure.ACCESSIBILITY_ENABLED` from "some service is currently handling events",
 * so it is false on a device where the user has not enabled any service yet.
 */
@Immutable
data class AccessibilityStatus(
    val installedCount: Int = 0,
    val enabledCount: Int = 0,
    val accessibilityEnabled: Boolean = false,
    val viaShizuku: Boolean = false,
)

fun getAppVersion(context: Context): AppVersion {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)!!
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    return AppVersion(
        versionName = packageInfo.versionName!!,
        versionCode = versionCode
    )
}

fun resolveDeviceName(): String = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

fun resolveAndroidVersion(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
