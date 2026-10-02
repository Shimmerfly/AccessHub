<div align="center">

# AccessHub · 无障碍管家

**用 Shizuku / Sui 一屏管理所有无障碍服务，不用再钻系统设置**

[![License](https://img.shields.io/github/license/Shimmerfly/AccessHub?label=License&color=blue)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B-3DDC84?logo=android&logoColor=white)](#环境要求)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-7F52FF?logo=kotlin&logoColor=white)](#技术栈)
[![compileSdk](https://img.shields.io/badge/compileSdk-37-3DDC84?logo=android&logoColor=white)](#技术栈)
[![Shizuku API](https://img.shields.io/badge/Shizuku%20API-13.1.5-5A45FF)](https://shizuku.rikka.app/)
[![Sui](https://img.shields.io/badge/Sui-supported-5A45FF)](https://github.com/RikkaApps/Sui)
[![UI](https://img.shields.io/badge/UI-Material%203%20Expressive-6750A4)](#界面)

[![Stars](https://img.shields.io/github/stars/Shimmerfly/AccessHub?style=social)](https://github.com/Shimmerfly/AccessHub/stargazers)
[![Forks](https://img.shields.io/github/forks/Shimmerfly/AccessHub?style=social)](https://github.com/Shimmerfly/AccessHub/network/members)
[![Issues](https://img.shields.io/github/issues/Shimmerfly/AccessHub)](https://github.com/Shimmerfly/AccessHub/issues)
[![Downloads](https://img.shields.io/github/downloads/Shimmerfly/AccessHub/total?label=Downloads&color=yellow)](https://github.com/Shimmerfly/AccessHub/releases)

</div>

AccessHub 是一个通过 **[Shizuku](https://shizuku.rikka.app/) / [Sui](https://github.com/RikkaApps/Sui)**
以 ADB（shell）或 Root 身份读写系统无障碍服务列表的 Android 应用喵。
它把散落在「设置 → 无障碍 → 已安装的服务」里的开关集中到一屏，拨一下就能启用 / 禁用任意服务。

- 界面结构参考 **[KernelSU](https://github.com/tiann/KernelSU) 管理器**，界面统一采用 **Material 3 Expressive**
- **不需要 root**：Shizuku 可以用「无线调试」启动；有 root 的话也可以用 Sui（Magisk 模块）
- **没授权也能看**：枚举服务与读取当前状态用的是公开 API，写入才需要特权

> 目前还没有发布 Release，想用请[从源码构建](#构建)喵。

---

## 项目结构图

[![Architecture diagram of shimmerfly/accesshub](https://gitdiagram.com/shimmerfly/accesshub/diagram.png)](https://gitdiagram.com/shimmerfly/accesshub?utm_source=readme&utm_medium=picture)

---

## 功能

### 主页

- **Shizuku / Sui 状态大卡**：未安装 / 未运行 / 未授权 / 已就绪四种状态，就绪时显示版本、UID 与 `ADB` / `ROOT` 标识，点击即可去授权、启动服务或查看说明
- **更新提示**：设置里开启检查更新后，有新版本会在顶部显示可点的提示卡
- **信息卡**：App 版本、Android 版本、设备型号、系统指纹；无障碍服务「已启用 / 共」，以及当前提供特权的是 Shizuku 还是 Sui
- **快捷链接**：项目仓库、了解 Shizuku / Sui、应用权限

### 服务（管理页）

- **自动发现**：列出设备上全部已安装的无障碍服务（应用图标、服务名、描述）
- **一键开关**：拨动开关即启用 / 禁用，写入失败会自动回滚并提示原因
- **只读降级**：没有 Shizuku / Sui 时照样列出服务并显示真实启用状态，只是开关不可拨
- 未授权时顶部会有一张提示卡，点一下直接申请权限

### 设置

- 主题模式、Monet 动态取色、强调色、色彩标准与风格
- 界面缩放、顶栏/底栏模糊、Apple 风格悬浮底栏、液态玻璃、预测性返回手势
- 检查更新开关

### 其它

- 中文 / 英文文案完整，另附带移植自上游的多语言资源（新功能的文案尚未翻译）
- 纯本地运行：无障碍相关操作全部通过本机 shell 完成，不上传任何数据

---

## 界面

底栏（横屏时为侧栏）共三页：

| 页面 | 内容 |
| --- | --- |
| **主页** | Shizuku / Sui 状态、更新提示、设备与无障碍信息、快捷链接 |
| **服务** | 无障碍服务列表 + 开关（本应用的核心功能） |
| **设置** | 主题、动画与更新等选项 |

界面统一采用 **Material 3 Expressive**：`androidx.compose.material3` 1.5.0-alpha22，配合动态取色与表现主义圆角。
Miuix（`top.yukonga.miuix.kmp`）的整套实现仍保留在源码中，但设置里已不再提供切换，应用固定以 Material 3 呈现喵。

---

## 工作原理

无障碍权限的真身是两个系统 Secure 设置喵：

| Key | 含义 |
| --- | --- |
| `enabled_accessibility_services` | 冒号分隔的 `包名/服务类名` 列表（真正决定哪些服务被启用） |
| `accessibility_enabled` | 无障碍层是否在跑（**由系统推导**，见下方说明） |

- **枚举**：`AccessibilityManager.getInstalledAccessibilityServiceList()`，公开 API，无需任何权限
- **读取**：直接读公开的 `Settings.Secure`，所以没有特权时也能显示真实状态
- **写入**：经 Shizuku / Sui 以 shell（uid 2000）或 root（uid 0）身份执行 `settings put secure …`。
  shell 用户持有 `WRITE_SECURE_SETTINGS`，而普通应用拿不到这个权限 —— 这正是需要 Shizuku / Sui 的原因

写入示例（应用内部拼接后交给特权进程执行）：

```sh
settings put secure enabled_accessibility_services 'com.pkg/com.pkg.MyService:com.other/com.other.Svc'
settings put secure accessibility_enabled 1
```

### 写路径：Shizuku UserService

Shizuku 13 已移除 `Shizuku#newProcess`，官方推荐用 **UserService**：由 Shizuku 在自己的进程里加载本应用的
APK 并实例化我们的服务类，代码就以 shell / root 身份运行了。

```
App 进程 ──bindUserService──▶ Shizuku 进程（shell uid 2000 / root uid 0）
                                    └── ShellUserService : IShellService.Stub
                                            └── /system/bin/sh -c "settings put secure …"
```

因为 UserService 进程**不是常规的应用进程**（`getContentResolver` 之类不可用），这里走的是执行 shell 命令，
而不是直接调用 `Settings.Secure.put*` 喵。相关实现在
[`IShellService.aidl`](app/src/main/aidl/dev/sol/accesshub/shizuku/IShellService.aidl)、
[`ShellUserService.kt`](app/src/main/java/dev/sol/accesshub/shizuku/ShellUserService.kt)、
[`ShizukuShell.kt`](app/src/main/java/dev/sol/accesshub/shizuku/ShizukuShell.kt)。

> `accessibility_enabled` 不是设置里的「总开关」，而是 AOSP 的
> `AccessibilityManagerService` 根据「当前有没有服务在接管事件」自己写入的结果
> （`isHandlingAccessibilityEventsLocked()`），所以一个服务都没开时它是 `0`，属于正常现象喵。

---

## 环境要求

| 项目 | 要求 |
| --- | --- |
| Android | 7.0（API 24）及以上 |
| 特权方案 | [Shizuku](https://shizuku.rikka.app/)（无线调试 / 电脑 ADB 启动）或 [Sui](https://github.com/RikkaApps/Sui)（Magisk 模块） |
| 权限 | 无需 root；无需 `QUERY_ALL_PACKAGES` |

使用步骤：

1. 安装并启动 Shizuku（或刷入 Sui 模块）
2. 打开 AccessHub → 主页大卡点「授权」→ 在 Shizuku / Sui 弹窗里允许
3. 切到「服务」页拨开关即可；没授权时也能先查看当前状态

> Sui 的授权界面可以在系统设置里长按调出快捷方式，或在拨号盘输入 `*#*#784784#*#*` 打开喵。

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

`versionName` 与 `versionCode` 都由 git 自动生成，不需要手动维护喵：

| 字段 | 取值 | 示例 |
| --- | --- | --- |
| `versionName` | `git+<7 位短 SHA>` | `git+af71520` |
| `versionCode` | 当前分支的 commit 数 | `4` |

实现见 [`app/build.gradle.kts`](app/build.gradle.kts)（用惰性 `providers.exec`，不会因为每次提交而使配置缓存失效）。
因此**每次提交都会让 versionCode +1**；反过来，如果做过 squash / rebase 导致 commit 数变小，
新包的 versionCode 会低于手机上已安装的版本而无法覆盖安装，需要先卸载喵。

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

## 项目结构

```
app/src/main/
├── aidl/dev/sol/accesshub/shizuku/
│   └── IShellService.aidl              # 特权进程里的执行器接口（exec / destroy）
├── java/dev/sol/accesshub/
│   ├── AccessHubApp.kt                 # Application：初始化 Shizuku 监听、网络客户端
│   ├── data/
│   │   ├── ServiceMeta.kt              # 无障碍服务数据模型
│   │   └── repository/
│   │       ├── AccessibilityRepository.kt  # 枚举服务、读取与写入启用集合
│   │       └── SettingsRepository*.kt      # 设置项持久化
│   ├── permission/                     # 运行时权限（通知 / 存储 / 电池白名单…）
│   ├── shizuku/
│   │   ├── ShizukuStatus.kt            # 状态模型 + 提供方（Shizuku / Sui）
│   │   ├── ShizukuManager.kt           # binder / 授权监听，暴露 StateFlow
│   │   ├── ShizukuShell.kt             # 绑定 UserService 并执行 shell
│   │   └── ShellUserService.kt         # 跑在 shell(uid 2000) 里的实现
│   └── ui/
│       ├── MainActivity.kt             # 入口：三页 Pager + 底栏 / 侧栏
│       ├── navigation3/                # Navigation 3 路由与 Navigator
│       ├── screen/
│       │   ├── home/                   # 主页：KSU 风格状态页（Miuix / Material 双实现）
│       │   ├── accessibility/          # 服务页：列表 + 开关（双实现）
│       │   ├── settings/               # 设置页
│       │   ├── about/ colorpalette/ permission/
│       ├── component/                  # 底栏、对话框、miuix / material 组件、模糊与液态玻璃
│       ├── theme/ util/ viewmodel/
└── res/                                # 字符串（中 / 英）、主题、图标、多语言资源
```

---

## 常见问题

**Q：为什么「服务」页的开关是灰的？**
没有拿到特权。装好 Shizuku / Sui 并在主页大卡点授权后，开关就能拨了；在此之前仍然可以查看真实状态。

**Q：需要 root 吗？**
不需要。Shizuku 用「无线调试」就能启动，拿到的是 shell（uid 2000）身份；有 root 的设备也可以刷 Sui，效果一致。

**Q：支持 Dhizuku 吗？**
暂时不支持。Dhizuku 分享的是 **DeviceOwner** 权限，而 AOSP 的 `DevicePolicyManagerService` 中
`setSecureSetting` 的白名单只有 `DEFAULT_INPUT_METHOD` / `SKIP_FIRST_USE_HINTS` / `INSTALL_NON_MARKET_APPS`
等少数键（**不含无障碍设置**），`setPermissionGrantState` 又只能授予运行时权限，
所以 DeviceOwner 无法开关无障碍服务喵。

**Q：会在系统设置里乱改东西吗？**
只会写上面那两个与无障碍相关的 Secure 键，且每次写入都是「把当前开关状态整体同步过去」，
不会碰其它设置，也不会联网上传任何数据。

---

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=Shimmerfly/AccessHub&type=Date)](https://star-history.com/#Shimmerfly/AccessHub&Date)

---

## 致谢

- [tiann/KernelSU](https://github.com/tiann/KernelSU) —— 界面结构与交互的主要参考
- [chenaizhang/KernelSU-Style-UI-Kit](https://github.com/chenaizhang/KernelSU-Style-UI-Kit) —— 本项目界面的移植来源
- [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) 与 [RikkaApps/Sui](https://github.com/RikkaApps/Sui) —— 特权接口
- [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) —— Miuix 组件库
- [material-kolor](https://github.com/jordond/materialkolor) —— 动态取色

---

## 许可

[GNU General Public License v3.0](LICENSE)

无障碍服务权限极高（可读取屏幕内容并代为操作），请只为信任的服务开启喵。
