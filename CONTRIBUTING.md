## 工作原理

无障碍权限的真身是两个系统 Secure 设置：

| Key | 含义 |
| --- | --- |
| `enabled_accessibility_services` | 冒号分隔的 `包名/服务类名` 列表（真正决定哪些服务被启用） |
| `accessibility_enabled` | 无障碍层是否在跑（**由系统推导**，见下方说明） |

- **枚举**：`AccessibilityManager.getInstalledAccessibilityServiceList()`，公开 API，无需任何权限
- **读取**：直接读公开的 `Settings.Secure`，所以没有特权时也能显示真实状态
- **写入**：经 Shizuku / Sui 以 shell（uid 2000）或 root（uid 0）身份执行 `settings put secure …`。
  shell 用户持有 `WRITE_SECURE_SETTINGS`，而普通应用拿不到这个权限 —— 这正是需要 Shizuku / Sui 的原因

写入示例（应用内部拼接后交给特权进程执行）：

```bash
settings put secure enabled_accessibility_services 'com.pkg/com.pkg.MyService:com.other/com.other.Svc'
settings put secure accessibility_enabled 1
```

### 写路径：Shizuku UserService

Shizuku API 13 移除了 `Shizuku#newProcess`，官方推荐用 **`UserService`**：由 Shizuku 在自己的进程里加载本应用的
APK 并实例化服务类，代码会以 Shell / Root 身份运行。

```
App 进程 ── bindUserService ──▶ Shizuku 进程（Shell uid 2000 / Root uid 0）
                                    └── ShellUserService : IShellService.Stub
                                            └── /system/bin/sh -c "settings put secure …"
```

因为 UserService 进程**不是常规的应用进程**（`getContentResolver` 之类不可用），这里走的是执行 shell 命令，
而不是直接调用 `Settings.Secure.put*`。相关实现于
[`IShellService.aidl`](app/src/main/aidl/dev/sol/accesshub/shizuku/IShellService.aidl)、
[`ShellUserService.kt`](app/src/main/java/dev/sol/accesshub/shizuku/ShellUserService.kt)、
[`ShizukuShell.kt`](app/src/main/java/dev/sol/accesshub/shizuku/ShizukuShell.kt)。

> `accessibility_enabled` 不是设置里的「总开关」，而是 AOSP 的
> `AccessibilityManagerService` 根据「当前有没有服务在接管事件」自己写入的结果
> （`isHandlingAccessibilityEventsLocked()`），所以一个服务都没开时它是 `0`，属于正常现象。

---

## 构建

环境要求：**JDK 21**、Android SDK（`compileSdk 37` / Android 17、`build-tools 37.0.0`）。

```bash
# 命令行
./gradlew :app:assembleDebug     # 产物：app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease   # 本地用 debug key 签名（便于覆盖安装调试）
./gradlew :app:installDebug      # 安装到已连接设备

# 或用 Android Studio 打开本目录，Sync 后 Run
```

若 SDK 不在默认位置，编辑 `local.properties`：

```properties
sdk.dir=/path/to/Android/Sdk
```

### 版本戳

`versionName` 与 `versionCode` 都由 git 自动生成，不需要手动维护：

| 字段 | 取值 | 示例 |
| --- | --- | --- |
| `versionName` | `git+<7 位短 SHA>` | `git+af71520` |
| `versionCode` | 当前分支的 commit 数 | `4` |

实现见 [`app/build.gradle.kts`](app/build.gradle.kts)（用惰性 `providers.exec`，不会因为每次提交而使配置缓存失效）。
因此**每次提交都会让 versionCode +1**；反过来，如果做过 squash / rebase 导致 commit 数变小，
新包的 versionCode 会低于手机上已安装的版本而无法覆盖安装，需要先卸载。

---

## 技术栈

| 组件 | 版本 |
| --- | --- |
| AGP | 9.4.1 |
| Kotlin | 2.4.20 |
| Gradle Wrapper | 9.7.1 |
| Compose BOM | 2026.05.01 |
| Material 3 (Expressive) | 1.5.0-alpha22 |
| Miuix (KMP) | 0.9.2（实现保留，未启用）|
| Navigation 3 | 1.1.2 |
| MaterialKolor | 4.1.1 |
| Shizuku api / provider | 13.1.5 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 24 |

---
