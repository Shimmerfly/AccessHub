# Keep Shizuku entry points.
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# Keep accessibility service metadata parsing intact.
-keep class android.accessibilityservice.** { *; }
