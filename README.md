# EarRove 聆途

EarRove 是一款面向视障用户的 Android 辅助应用，聚焦「无障碍出行 + 视觉信息获取」两类核心场景。  
项目基于 Jetpack Compose 构建 UI，集成百度地图能力进行步行导航，并结合语音识别、本地 OCR 与语音播报，提供语音 + 震动的多模态交互体验。

## 核心功能

- **智能导航**：点击麦克风语音输入目的地，步行导航过程中播报距离与转向等信息。
- **提示模式（设置，默认关闭）**：开启后，在导航进行中会**模拟**前方障碍物与路口红绿灯的语音与震动提示，用于体验播报优先级与震动编码；**并非**真实路况检测。关闭后仅使用常规步行导航播报。
- **文字识别（OCR）**：CameraX 预览识别文字并朗读；支持相册选图；提供**复制**、**重播**与闪光灯辅助暗光拍摄。
- **设置**：朗读速度、震动总开关、提示模式、「家」地址（与导航推荐联动）、撤回隐私同意等。
- **帮助中心**：应用内说明导航、OCR 操作与震动反馈含义（与实现一致：障碍物两次短震、转向长震一次、红绿灯三次短震）。
- **无障碍**：TalkBack 语义、高对比界面；**隐私同意**与**启动自检**（占位密钥时给出可读修复指引）。

## 近期更新

- 设置页新增「**提示模式**」开关（持久化，默认关），控制导航中是否启用模拟红绿灯与避障流。
- 帮助中心文案更新：麦克风输入、提示模式说明、OCR 复制/重播/闪光灯，以及震动说明与代码行为对齐。

## 技术栈

- 平台：Android（Kotlin）
- UI：Jetpack Compose
- 构建：Gradle 8.13 + AGP 8.13.2
- 地图与导航：百度地图相关 SDK（本地 `app/libs`）
- 相机与图像：CameraX；OCR：Google ML Kit（本地 bundled）
- 语音与模型：百度语音/TTS、DashScope、Volcengine Ark 等（通过 `local.properties` / BuildConfig 注入）

## 环境要求

- Android Studio（建议最新稳定版）
- JDK 11
- Android SDK
  - `compileSdk = 36`
  - `targetSdk = 36`
  - `minSdk = 26`
- 可联网的 Android 真机（推荐，便于权限/定位/语音链路验证）

## 快速开始

### 1) 克隆项目

```bash
git clone https://github.com/Mingfree/EarRove1.git
cd EarRove1
```

### 2) 配置密钥（必做）

项目通过 `local.properties` 注入敏感配置，避免密钥硬编码到仓库。

1. 在项目根目录复制模板：

```bash
cp config.sample.properties local.properties
```

Windows PowerShell 可用：

```powershell
Copy-Item config.sample.properties local.properties
```

2. 打开 `local.properties`，填入真实值：

- `DASHSCOPE_API_KEY`
- `BAIDU_MAP_API_KEY`
- `BAIDU_SPEECH_APP_ID`
- `BAIDU_SPEECH_API_KEY`
- `BAIDU_SPEECH_SECRET_KEY`
- `BAIDU_TTS_API_KEY`
- `BAIDU_TTS_SECRET_KEY`
- `ARK_API_KEY`

> 注意：`local.properties` 不应提交到版本库。

### 3) 打开并同步工程

1. 使用 Android Studio 打开项目根目录。
2. 等待 Gradle Sync 完成。
3. 确认 `app/libs` 下本地 SDK 依赖被正确识别（项目已通过 `fileTree` 引入）。

### 4) 构建与运行

- IDE 方式：直接运行 `app` 模块到设备/模拟器。
- 命令行方式：

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

## 首次启动说明

- 首次进入会显示隐私协议页面，需要勾选应用隐私与百度地图隐私后继续。
- 应用会进行启动自检：如果仍使用占位符密钥，会提示你修复配置并重新构建。
- 使用导航/OCR 前，请授予定位、相机、麦克风等必要权限。

## 文档

| 文档 | 路径 | 说明 |
| --- | --- | --- |
| 用户手册（面向最终用户） | [`docs/用户手册.md`](docs/用户手册.md) | 功能说明与操作步骤，体例参考 `docs/sample.md` |
| 设计与开发文档（面向评审/归档） | [`docs/设计与开发文档.md`](docs/设计与开发文档.md) | 需求—设计—测试—安装—总结，体例参考 `docs/Design and Development Documentation Sample.md` |
| 详细使用说明（技术向） | [`docs/使用说明-详细版.md`](docs/使用说明-详细版.md) | 架构、模块、门禁与排障 |
| 产品需求 | [`docs/PRD.md`](docs/PRD.md) | PRD |
| 测试用例 | [`docs/测试用例-EarRove.md`](docs/测试用例-EarRove.md) | 功能与场景用例表 |
| 任务清单 | [`docs/tasks.md`](docs/tasks.md) | 分阶段改进 |
| 无障碍验收 | [`docs/accessibility-checklist.md`](docs/accessibility-checklist.md) | 发布前检查 |
| 模板样例（勿当正式用户文档） | `docs/sample.md`、`docs/Design and Development Documentation Sample.md` | 软著/大赛格式参考 |

## 项目结构

```text
<项目根>/
├─ app/                         # 主应用模块（Compose UI、导航、OCR、设置等）
│  ├─ src/main/java/com/example/earrove/
│  │  ├─ data/                  # 数据层（settings/privacy repository）
│  │  ├─ domain/                # 领域层（usecase/arbitration）
│  │  ├─ di/                    # 轻量手动 DI 容器（AppContainer）
│  │  ├─ ui/                    # 页面与组件（home/navigation/ocr/settings/help）
│  │  ├─ navigation/            # 导航相关服务与逻辑（含模拟避障/红绿灯服务）
│  │  └─ utils/                 # 配置、权限、仲裁、TTS、隐私等工具
│  └─ libs/                     # 本地 AAR/SO（百度相关依赖）
├─ core/                        # 历史/补充能力模块（当前默认未在 settings 中 include）
├─ docs/                        # 文档（用户手册、设计与开发、PRD、测试用例等）
├─ README.md                    # 本文件
└─ config.sample.properties     # 本地配置模板
```

## 常用命令

```bash
# 编译 Debug 包
./gradlew :app:assembleDebug

# 单元测试
./gradlew :app:testDebugUnitTest

# 仪器测试（需连接设备）
./gradlew :app:connectedDebugAndroidTest

# 代码风格检查（ktlint）
./gradlew ktlintCheck
./gradlew ktlintFormat
```

## 最近架构改动（P3/P4）

- P3：完成 `settings/privacy` 的 data/domain 抽离，UI 不再直接操作 SharedPreferences。
- P3：`SettingsScreen` 与 `OcrScreen` 已引入 ViewModel，状态管理从 Composable 中解耦。
- P3：仲裁器统一为 `AccessibilityEvent` 输入，优先级规则集中处理。
- P3：新增 `AppContainer`（轻量手动 DI），关键依赖可被 fake 替换。
- P4：新增单测（`PrivacyConsentInteractor`、`Arbitrator` 相关）与 UI 冒烟测试。
- P4：新增发布前无障碍验收清单：`docs/accessibility-checklist.md`。

## 常见问题

- 地图/语音能力不可用  
  优先检查 `local.properties` 是否填写真实密钥，并重新构建安装。

- 启动卡在初始化页  
  检查隐私协议是否完整同意；检查网络、定位服务与权限状态。

- OCR 无法工作  
  确认相机权限已授权，设备相机可用，并在光线不足时使用闪光灯。

- Gradle 构建失败  
  先确认 JDK 11、Android SDK 版本与网络环境，再执行 `gradlew clean` 后重试。

- `testDebugUnitTest` 出现 `GradleWorkerMain` / `Could not write standard input`  
  通常是本地 Gradle 测试执行器缓存损坏或守护进程异常。建议依次执行：
  `./gradlew --stop`、`./gradlew clean`、删除 `~/.gradle/caches` 中损坏条目后重试。

- `connectedDebugAndroidTest` 出现 TLS handshake 失败  
  常见于代理/证书链/企业网关问题。请检查系统代理、证书信任链、以及对
  `repo.maven.apache.org`、`dl.google.com` 的 HTTPS 连通性。

## 安全与合规建议

- 严禁提交任何真实 API Key/Secret。
- 所有密钥仅通过本地配置注入（`local.properties` / CI Secret）。
- 发布前请做权限路径与无障碍路径回归（TalkBack、拒权场景、隐私撤回场景）。

---
