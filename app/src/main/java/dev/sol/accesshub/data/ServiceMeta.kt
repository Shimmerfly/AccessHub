package dev.sol.accesshub.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/** One installed accessibility service discovered on the device. */
@Immutable
data class ServiceMeta(
    /** Flattened ComponentName, e.g. "com.pkg/com.pkg.MyService" — the key used by the system. */
    val id: String,
    val pkg: String,
    val cls: String,
    val label: String,
    val appName: String,
    val description: String?,
    val icon: ImageBitmap?,
    /** Optional per-service settings activity, when the developer declared one. */
    val settingsActivity: String?,
)
