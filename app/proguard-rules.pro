# Keep Shizuku entry points.
-keep class rikka.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# Shizuku loads ShellUserService by name inside its own process (bindUserService), so R8 must
# keep the class and its AIDL Stub exactly as written.
-keep class dev.sol.accesshub.shizuku.ShellUserService { *; }
-keep interface dev.sol.accesshub.shizuku.IShellService { *; }
-keep class dev.sol.accesshub.shizuku.IShellService$* { *; }

# Keep accessibility service metadata parsing intact.
-keep class android.accessibilityservice.** { *; }
