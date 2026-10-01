# AccessHub · 无障碍管家（Shizuku + Material 3 Expressive）

一个通过 **Shizuku** 以 ADB / Root 权限读写系统「无障碍服务」列表的 Android 应用。
无需在系统设置里层层点击，即可一屏管理所有已安装的无障碍服务权限。

界面完全使用 **Material 3 Expressive 官方组件库**（`androidx.compose.material3:1.5.0-alpha22`）构建，
支持动态取色（Material You）、亮/暗色、弹簧动效与表现主义圆角。

---

## 功能

- **自动发现**：列出设备上全部已安装的无障碍服务（应用名、服务名、描述、图标）。
- **一键开关**：拨动开关即可启用 / 禁用任意无障碍服务；启用前有安全确认弹窗。
- **批量禁用**：顶栏菜单「全部禁用」，快速清空所有已授权服务。
- **状态横幅**：实时显示 Shizuku 连接状态（未安装 / 未运行 / 待授权 / 已就绪），并给出对应的下一步操作。
- **只读降级**：未连接 Shizuku 时仍可展示当前启用状态（读取 `Settings.Secure` 不需要权限）。
- **直达设置**：若服务自带设置页，可直接跳转。
- **搜索过滤**：按应用名 / 服务名 / 包名即时筛选。

---

## 工作原理

无障碍权限的真身是两个系统 Secure 设置：

| Key | 含义 |
| --- | --- |
| `enabled_accessibility_services` | 冒号分隔的 `包名/服务类名` 列表 |
| `accessibility_enabled` | 无障碍总开关（0/1） |

- **枚举**：`AccessibilityManager.getInstalledAccessibilityServiceList()`（公开 API，无需权限）。
- **读取**：优先经 Shizuku 执行 `settings get secure …`；无 Shizuku 时回退到 `Settings.Secure.getString`（只读）。
- **写入**：经 Shizuku 以 shell(uid 2000) 身份执行 `settings put secure …`。
  shell 用户持有 `WRITE_SECURE_SETTINGS`，这正是修改无障碍列表所需的权限——
  普通应用无法获得，而 Shizuku 恰好能代劳。

写入示例（应用内部拼接）：

```sh
settings put secure enabled_accessibility_services 'com.pkg/com.pkg.MyService:com.other/com.other.Svc'
settings put secure accessibility_enabled 1
```

---

## 前置条件

1. 安装并启动 **[Shizuku](https://shizuku.rikka.app/)**（通过「无线调试」或电脑 ADB 启动）。
2. 打开本应用，在状态横幅点击 **授权**，在 Shizuku 弹窗中允许。
3. 完成，即可管理无障碍权限。

> Root 设备也可直接用 Root 方式启动 Shizuku，效果一致。

---

## 构建

环境要求：JDK 21、Android SDK（`compileSdk 37` / Android 17、`build-tools 37.0.0`）。

```bash
# 命令行
./gradlew assembleDebug          # 产物：app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug           # 直接安装到已连接设备

# 或：用 Android Studio 打开本目录，Sync 后 Run
```

若 SDK 不在默认位置，编辑 `local.properties`：

```
sdk.dir=/path/to/Android/Sdk
```

---

## 技术栈

| 组件 | 版本 |
| --- | --- |
| AGP | 9.4.1 |
| Kotlin | 2.4.20 |
| Gradle Wrapper | 9.7.1 |
| Compose BOM | 2026.05.01 |
| **Material 3 (Expressive)** | **1.5.0-alpha22** |
| Shizuku api / provider | 13.1.5 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 24 |

用到的 MD3E 官方组件：`Scaffold`、`TopAppBar`、`ListItem`、`Card`、`Switch`、
`Button` / `FilledTonalButton`、`OutlinedTextField`、`AlertDialog`、`DropdownMenu`、
`CircularProgressIndicator`、`SnackbarHost`、动态取色 `MaterialTheme`。

---

## 项目结构

```
app/src/main/java/dev/sol/accesshub/
├── AccessHubApp.kt              # Application：初始化 Shizuku 监听
├── MainActivity.kt              # 入口 Activity，edge-to-edge + 授权引导
├── shizuku/
│   ├── ShizukuManager.kt        # 绑定/权限状态机（StateFlow<ShizukuState>）
│   └── ShizukuShell.kt          # 经 Shizuku 执行 shell（settings get/put）
├── data/
│   ├── ServiceMeta.kt           # 无障碍服务数据模型
│   └── AccessibilityRepository.kt  # 枚举 + 读取/写入启用集合
└── ui/
    ├── MainViewModel.kt         # UI 状态、开关、确认、批量禁用
    ├── HomeScreen.kt            # 主界面（列表/搜索/状态/对话框）
    ├── components/
    │   ├── ShizukuStatusCard.kt # 表现主义状态横幅
    │   └── DrawableIcon.kt      # 渲染应用图标
    └── theme/                   # MD3E 主题、字体、形状
```

---

## 安全提示

无障碍服务权限极高，可读取屏幕内容并代为操作。请只为你信任的服务开启权限。
本应用不会上传任何数据，所有操作均在本地通过 Shizuku 完成。
