# EarRove 项目改进计划（可执行任务清单）

> ✅ **已完成归档（2026-10）**：本文档所列任务已基本全部落地，现作为完成记录归档。少量「有意未修/残余」项与 DoD 偏差已在各任务条目内标注。

本文档基于当前仓库实现现状（`app` + `core`、Compose UI、`utils` 工具类）整理。  
目标：在不大改架构的前提下，分阶段提升**功能完整性、无障碍体验、界面一致性、代码模块化与可维护性**。

---

## 使用说明

- 每个任务都遵循：**单一关注点**、**开始条件明确**、**结束条件明确（DoD）**、**可独立测试**。
- 推荐按阶段顺序执行；每完成一个任务就提交一次小 PR。
- 任务编号格式：`P{阶段}-T{任务号}`。

---

## 阶段 P0：稳定与安全先行（必须先做）

### P0-T1 移除硬编码密钥并改为本地配置注入
- **关注点**：安全与配置管理
- **开始**：
  - 识别硬编码密钥位置（如 `AppConfig.kt`、`AndroidManifest.xml` meta-data）
- **结束（DoD）**：
  - 代码库中不再出现真实 API Key/Secret 明文
  - 使用 `local.properties` 或 `gradle.properties` + `BuildConfig` 注入
  - README 补充配置步骤
- **测试**：
  - 全局搜索 `sk-` / API key 关键字不再命中真实值
  - Debug 构建成功，关键功能可读到配置值

### P0-T2 增加“配置缺失”启动自检与可读错误提示
- **关注点**：可诊断性
- **开始**：
  - 在 `MainActivity` 或 `MyApplication` 添加启动检查入口
- **结束（DoD）**：
  - 当关键配置缺失时，UI 明确提示“缺少配置项 + 处理建议”
  - 不再出现静默失败（例如 TTS 初始化失败无引导）
- **测试**：
  - 手动置空配置，验证错误提示可见
  - 恢复配置后可正常进入应用

### P0-T3 为隐私同意流程补充“设置中可撤回”入口
- **关注点**：合规与用户控制权
- **开始**：
  - 在设置页新增“隐私设置”项
- **结束（DoD）**：
  - 可查看当前同意状态
  - 可撤回同意并触发对应能力降级（如禁用导航）
- **测试**：
  - 同意后进入导航；撤回后再次进入导航时被正确拦截

---

## 阶段 P1：功能完善（当前缺口优先）

### P1-T1 让“设置页朗读速度”真正作用于全局 TTS
- **关注点**：功能闭环
- **开始**：
  - 抽取 `AppSettingsRepository`（先用 SharedPreferences）
- **结束（DoD）**：
  - `SettingsScreen` 调整语速会持久化
  - `TTSManager` 初始化与变更时读取并应用语速
- **测试**：
  - 重启应用后语速保持
  - 语速变化可通过两段固定播报听感验证

### P1-T2 让“震动开关”真正控制所有震动输出
- **关注点**：功能闭环
- **开始**：
  - 在 `VibrationManager` 增加 `enabled` 判断（由设置仓库提供）
- **结束（DoD）**：
  - 开关关闭后，障碍/转向/红绿灯都不再振动
  - 开关开启后恢复
- **测试**：
  - 三类场景分别验证振动是否按开关生效

### P1-T3 导航页增加“权限被永久拒绝”的专门引导
- **关注点**：异常路径体验
- **开始**：
  - 在权限请求回调中识别 `shouldShowRequestPermissionRationale = false` 场景
- **结束（DoD）**：
  - 出现永久拒绝时展示“去系统设置开启权限”按钮
  - 避免反复弹窗但无法继续
- **测试**：
  - 手动永久拒绝权限后，页面给出可执行引导

### P1-T4 为 OCR 增加“识别结果复制/重播”操作
- **关注点**：使用效率
- **开始**：
  - 在 OCR 结果区增加两个按钮：复制文本、重新朗读
- **结束（DoD）**：
  - 用户可复制识别结果到剪贴板
  - 可一键重新播放识别文本
- **测试**：
  - 剪贴板内容与结果一致
  - 重播可在 TTS 播报中听到

### P1-T5 实现“推荐目的地”动态算法与“家”的功能联动
- **关注点**：算法优化与位置服务
- **状态**：✅ 已完成（DoD 第 2 条按产品决定调整，见下）
- **实现说明**：推荐位为**固定类别入口**，按钮文案恒定（家/附近超市/附近地铁站/附近公交站/附近学校/附近医院）；点击后按**当前定位**调用 `searchNearbyPoiCandidates`（按距离排序）检索该类别 POI，在候选面板中列出**真实地点名称与距离**供选择，从而满足「与当前定位契合」。类别改由 `RecommendCategory` 枚举承载，替代原先按按钮文案字符串比较来判定「家」的脆弱写法。
- **DoD 偏差说明**：原 DoD 要求「若未设置『家』则不显示该按钮」。产品决定**保持现状**——「家」始终显示，未设置时点击会播报提示并跳转设置页（该行为未改动）。
- **开始**：
  - 调用地图 SDK 获取当前周边 POI（地标、超市等生活场景）
  - 在设置页增加“预设家地址”的存储逻辑
- **结束（DoD）**：
  - 推荐列表数量不超过 8 个，且与当前定位契合
  - 若已设置“家”，推荐位首项固定显示“家”；若未设置则不显示
- **测试**：
  - 切换不同地点定位，验证推荐列表刷新
  - 清除/设置家地址，验证按钮显示状态

### P1-T6 为文字识别增加语音引导流程
- **关注点**：交互引导
- **状态**：✅ 已完成（播报位置与 DoD 字面描述不一致，见实现说明）
- **实现说明**：播报位置采用**进入 OCR 页时**（而非首页按钮点击时）。经评估，**引导播报与相机权限申请保持各自独立的 `LaunchedEffect`、互不等待**——若让权限申请等待播报结束，慢语速（设置可至 0.5x）下引导语可达十几秒，会明显拖慢进入识别流程；两者并发虽有短暂重叠，但流程更顺畅。TTS 实例由 `EarRoveApp` 持有并传入。
- **DoD 偏差说明**：DoD 第 2 条「播报结束后进入原有识别流程」**未按字面实现**，改为并发执行（理由同上）。DoD 第 1 条「点击后先播报引导语」的实质已满足——进入页面即播报正确文案。
- **开始**：
  - 在视觉识别页面的“文字识别”按钮点击事件中注入播报逻辑
- **结束（DoD）**：
  - 点击后先播报：“请将手机对准想要识别的物品或选择相册图片，如路牌、药品说明书、衣物等”
  - 播报结束后进入原有识别流程
- **测试**：
  - 验证点击按钮后语音播报的完整性和触发顺序

---

## 阶段 P2：界面与无障碍体验优化

### P2-T1 建立统一文案资源（移除硬编码中文字符串）
- **关注点**：国际化与维护成本
- **状态**：✅ 已完成
- **实现说明**：UI 层文案此前已迁移；本次补齐**服务层**——`ObstacleDetectionService` / `TrafficLightService` 原先各自返回硬编码中文（`getObstacleDescription` / `getTrafficLightDescription`），现改为在仲裁层传递**枚举**，由本地化边界 `AndroidArbitrationTextProvider` 与 `ArbitrationLabels` 统一映射到 `strings.xml`：
  - 新增 `ObstacleType` / `TrafficLightStatus` / `TurnDirection` 于 `domain/arbitration`（**领域层不依赖 `R`**，枚举→文案映射收口在 `utils/ArbitrationLabels.kt`）
  - `NavigationStep.turnType` 由「未声明的字符串枚举」（`"LEFT"|"RIGHT"|…`）改为真正的 `TurnDirection`
  - `AccessibilityEvent` 三个字段由 `String` 改为枚举；`Destination`/`RouteStart`/`Ocr`/`Info` 为真自由文本，保持 `String`
  - 顺带修掉 `TrafficLightStatus.NONE -> ""` 会产出「，还有12秒」残缺播报的隐患
- **残余（有意保留）**：`NavigationService` 解析百度返回中文指令用的 `contains("左转")` 属**输入解析**而非输出文案；`instruction`/`destination` 的兜底默认值（`"继续前进"`/`"目的地"`）作用于地图 SDK 数据，且该服务无 `Context`，暂不改动。
- **开始**：
  - 扫描 `ui/*` 与 `utils/*` 中直接写死的文案
- **结束（DoD）**：
  - 所有用户可见文本迁移到 `res/values/strings.xml`
  - 语义描述文本也统一资源化
- **测试**：
  - 编译无缺失 string 资源
  - UI 文案显示一致，TalkBack 可正常朗读

### P2-T2 统一页面间距/字号/控件尺寸设计规范
- **关注点**：视觉一致性
- **状态**：✅ 已完成
- **实现说明**：以 `DesignTokens.kt` 的 `AppSpacing`（4/8/12/16/24/32dp）与 `AppSize`（icon 20/28/36/48、fab 56/80、cardCorner 12dp）为唯一来源，替换命中 token 值的裸 `.dp`。本次补齐 `StartupGate.kt`（此前 0 token）、`NavigationScreen.kt`、`OcrScreen.kt`、`SettingsScreen.kt`；`HomeScreen`/`HelpScreen` 此前已 token 化。**有意保留**确有语义的一次性尺寸（如麦克风圆 120dp、控制按钮高 68dp、动画圆 200dp、按钮高 64dp、图标 60/100dp 等）——目标是「≥80% 页面统一 token」，非消灭所有 magic number。字号沿用 `MaterialTheme.typography`（个别 `fontSize = X.sp` 覆盖未纳入本次）。
- **开始**：
  - 提取 `Spacing`、`Sizes`、`Typography` 规范常量
- **结束（DoD）**：
  - 主页、导航、OCR、设置、帮助至少 80% 页面使用统一 token
  - 不再散落大量 magic number
- **测试**：
  - 关键页面截图对比（前后）
  - 深浅色/大字体下不溢出

### P2-T3 优化导航与 OCR 页面焦点顺序（TalkBack）
- **关注点**：读屏可操作性
- **开始**：
  - 识别当前关键控件的读屏顺序问题
- **结束（DoD）**：
  - 关键流程（开始导航、暂停/继续、拍照识别）焦点顺序自然
  - 动态状态变化有可感知提示（必要时使用 live region）
- **测试**：
  - 开启 TalkBack 逐项滑动，路径可闭环完成任务

### P2-T4 ~~增加“高对比 + 大字体”预设开关~~（已移除）
- **说明**：曾计划在设置页提供「视觉辅助模式」开关（更强对比与更大字号）。产品决定取消该开关，应用**固定**使用原先「关闭视觉辅助模式」时的默认主题与字号（`DarkColorScheme` + `EarRoveTypography`）。
- **状态**：不再实现；相关 UI、持久化与文档已删除。

### P2-T5 全局 UI 分辨率自适应优化
- **关注点**：布局鲁棒性
- **开始**：
  - 检查隐私协议页、设置页及导航页的硬编码尺寸
- **结束（DoD）**：
  - 按钮尺寸和屏幕比例使用 Compose 权重（`weight`）或百分比布局
  - 确保小屏手机不溢出，长屏手机不留白
- **测试**：
  - 使用最小屏模拟器和超长屏手机进行 UI 边界测试

### P2-T6 补全核心界面无文字控件的 contentDescription
- **关注点**：无障碍闭环
- **开始**：
  - 重点排查“视觉识别”拍照/相册按钮及“导航内部界面”的图标按钮
- **结束（DoD）**：
  - 启动页权限确认、导航内所有返回/功能图标均有准确描述
  - 确保 TalkBack 能正确朗读按钮当前的“状态”（如：开启/关闭）
- **测试**：
  - 开启 TalkBack 遍历所有页面，不应出现“未加标签的按钮”

---

## 阶段 P3：代码模块化与架构治理

### P3-T1 抽离 `settings` 与 `privacy` 为独立 data/domain 层
- **关注点**：模块边界
- **状态**：✅ 已完成
- **实现说明**：`PrivacyRepositoryImpl` 此前是纯转发，`PrivacyUtils` 混了 SharedPreferences 存取 / 百度 SDK 生命周期 / 政策文本三件事。现将 prefs 文件名与 4 个 key 的读写下沉到 `PrivacyRepositoryImpl`（`by lazy` 建 prefs），`PrivacyUtils` 退化为纯文本 + SDK 生命周期工具（新增回调式 `initializeBaiduSdk` 原语）。顺带：`clearAllPrivacyAgreements` 现一并清除 `KEY_FIRST_LAUNCH`；移除 `PrivacyUtils` 中零引用的 `CheckPrivacyAgreements` / `CheckBaiduSDKInitialization` / `PrivacyAgreementStatus`。**存储契约保持不变**（文件名 `earrove_privacy_preferences` 与 key 名未改，androidTest 预置依赖），改动限定在「读写位置」而非「存储契约」。
- **开始**：
  - 新建 `data/settings`、`data/privacy`、`domain/usecase` 包
- **结束（DoD）**：
  - UI 不再直接操作 SharedPreferences
  - 通过 repository + usecase 访问状态
- **测试**：
  - 现有设置与隐私流程行为无回归
  - 单元测试可 mock repository

### P3-T2 引入 ViewModel 承载页面状态（先改 Settings/OCR）
- **关注点**：状态管理
- **开始**：
  - 为 `SettingsScreen`、`OcrScreen` 新建对应 ViewModel
- **结束（DoD）**：
  - 业务状态与 UI 渲染分离
  - Compose 页面减少直接副作用调用
- **测试**：
  - ViewModel 单测覆盖状态变更
  - 页面旋转/重组后状态保持符合预期

### P3-T3 将仲裁器改造为“统一事件总线输入”
- **关注点**：扩展性
- **开始**：
  - 定义 `AccessibilityEvent`（Obstacle/Nav/Ocr/Info）
- **结束（DoD）**：
  - 业务侧统一发送事件给仲裁器
  - 仲裁规则集中配置，减少分散调用
- **测试**：
  - 构造并发事件序列，验证抢占顺序符合预期

### P3-T4 规范依赖注入（先轻量手动 DI，后可迁移 Hilt）
- **关注点**：可测试性
- **开始**：
  - 定义 `AppContainer` 管理单例（TTS/SettingsRepo/Arbitrator）
- **结束（DoD）**：
  - 页面不再直接 `remember` 构建复杂依赖链
  - 关键依赖可替换为 fake 实现
- **测试**：
  - 使用 fake TTS 运行 UI 流程，不触发真实语音

---

## 阶段 P4：质量保障与工程效率

### P4-T1 为核心工具类补单元测试（Privacy/Arbitrator）
- **关注点**：回归保护
- **开始**：
  - 新增 `test` 下的 `PrivacyUtils` 与 `Arbitrator` 测试类
- **结束（DoD）**：
  - 覆盖：同意状态读写、仲裁优先级抢占逻辑
- **测试**：
  - 本地 `testDebugUnitTest` 通过

### P4-T2 新增 UI 冒烟测试（主页跳转 + 设置保存）
- **关注点**：关键路径可用性
- **状态**：✅ 已完成（代码落地；`connectedAndroidTest` 需真机/模拟器执行）
- **实现说明**：`SmokeNavigationAndSettingsTest` 由 1 个 `@Test` 扩展为 3 个——`home_to_settings_save_home_address_persists`（既有，顺带把硬编码「保存家地址」改为 `R.string.settings_home_address_save`）、`home_to_navigation_screen_jumps`、`home_to_ocr_screen_jumps`。门禁处理抽为 `dismissStartupGates()` 复用；`grantRuntimePermissionsForTest()` 由「从未调用」改为 `@Before` 预授权（Activity 启动后执行，避免进入导航/OCR 时触发系统权限弹窗）。
- **开始**：
  - 在 `androidTest` 增加 Compose UI test
- **结束（DoD）**：
  - 覆盖：主页到导航/OCR/设置跳转
  - 覆盖：设置修改后重开页面值保持
- **测试**：
  - 连接设备/模拟器运行 `connectedAndroidTest` 通过

### P4-T3 接入静态检查与格式化基线
- **关注点**：代码一致性
- **开始**：
  - 引入 `ktlint` 或 `detekt`（二选一先落地）
- **结束（DoD）**：
  - CI 或本地脚本可一键检查
  - 新增代码遵循同一风格
- **测试**：
  - 故意引入样式问题，检查能被识别

### P4-T4 建立“发布前无障碍验收清单”
- **关注点**：流程化质量
- **开始**：
  - 新建 `docs/accessibility-checklist.md`
- **结束（DoD）**：
  - 清单至少包含：TalkBack 路径、权限拒绝路径、隐私同意流程、关键语音反馈
  - 每次发版按清单打勾
- **测试**：
  - 用最新安装包走完整清单一次并记录结果

---

## 建议执行节奏（两周一个迭代）

- **迭代 1（稳定与闭环）**：P0-T1 ~ P1-T2
- **迭代 2（体验优化）**：P1-T3 ~ P2-T4
- **迭代 3（架构治理）**：P3-T1 ~ P3-T4
- **迭代 4（质量固化）**：P4-T1 ~ P4-T4

---

## 每个任务的 PR 模板（建议）

- **变更内容**：只做一个任务
- **影响范围**：列文件
- **测试说明**：按任务内测试步骤截图/日志
- **回归风险**：1-2 条
- **回滚方案**：保持小步提交，可直接回退单 PR

