# XingClass

XingClass 是一个面向学生的**超星课程表 Android 客户端**。整个界面由 Kotlin + Jetpack Compose
原生绘制，采用**一屏全览**的新拟物（Neumorphism）课表；课程数据由 App **直接**访问超星接口获取，
没有自建后端服务器。

> 核心体验：打开 App，不滚动、不停顿地看到本周全部课程；登录状态、课程缓存都留在本机。

---

## 1. 功能范围

- 超星账号密码登录（AES/CBC + Base64 加密后提交）
- 会话 Cookie 由统一 `CookieJar` 管理，并在 Android Keystore 加密后持久化
- 登录状态检查与失效处理（失效自动回到登录页）
- 拉取课程 → 映射为统一 `Course` 模型 → 本地 JSON 缓存
- **Cache First + Remote Refresh**：先展示缓存，再后台刷新；刷新失败保留旧课表
- 一屏 7×10 课表：无主滚动、10 节均分、课程跨节、星期定位、今日高亮
- **周次切换**：上一周 / 下一周 / 回到本周；区分“正在查看的周”与“真实本周”
- 课程卡按实际可用高度自适应：标题优先，空间足够时楼栋与教室分两行
- 统一新拟物光影：外凸/内凹双阴影，按压 300ms 由外凸变内凹（无位移）
- **课程合并**：同一天、同标题/地点/教师且节次连续（间隔 ≤30 分钟）的课程合并为一个方块
- 单日课程抽屉：内部大凹槽 + 外凸课程卡；楼栋 · 教室 · 教师单行展示，
  左侧显示上课/下课时间
- 左侧节数栏：节数下方显示该节的上课/下课时间
- **诊断日志与导出**：持久化运行事件日志；设置页可导出运行日志与课程数据（JSON），
  通过系统分享面板保存/发送；日志不含密码、Cookie、Token 或响应体
- 自动 / 白天 / 黑夜三态主题（自动模式 08:00–22:00 为白天，每分钟检查一次）
- 设置页：主题、账号（脱敏）、登录加密密钥（默认 `u2oh6Vu^HWe4_AES`，可修改/恢复默认）、
  最近同步时间、清除缓存、退出登录；关于页含作者主页链接
- 登录页底部提供“登录密钥”入口：内置密钥失效或填错时，可在登录前修改/恢复默认密钥
- 空 / 加载 / 缓存 / 刷新中 / 成功 / 失败 / 离线页面状态
- 单元测试覆盖加密、解析、映射、布局、Cookie、Repository、自动主题等
- GitHub Actions：Push/PR 自动 Debug 构建；仅手动触发的 Release workflow

第一版明确**不做**：二维码登录、独立后端、注册、云同步、多账号、社交、成绩分析、推送、课程提醒。

## 2. 技术栈

- Kotlin 2.0.21 / Jetpack Compose（BOM 2024.12.01）/ Material3（仅用 Text/Icon 语义与主题色）
- minSdk **26**，targetSdk / compileSdk **35**
- 仅 **arm64-v8a**；APK 内的原生库只有 AndroidX 的
  `lib/arm64-v8a/libandroidx.graphics.path.so`，没有其他 ABI
- OkHttp 4.12（网络 + CookieJar）、kotlinx.serialization（缓存 / JSON 解析）
- AndroidX Security Crypto（Keystore 支撑的加密存储）
- 手动依赖注入（`AppContainer`），无 Hilt/KSP

## 3. 架构

依赖方向：

```text
UI (Compose) ──> ViewModel ──> domain.repository（接口）
                                      ▲
                                      │ 实现
                          data.repository ──> remote（OkHttp / CookieJar）
                                      │      └─> local（JSON 缓存）
                                      └─> security（Keystore / AES）
```

- UI 不出现 OkHttp、JSON、AES、Cookie；ViewModel 不出现 HTTP/存储实现。
- `di/AppContainer` 是唯一装配点，`XingClassApp` 持有应用级实例。
- UI 通过 `viewModelFactory { initializer { … } }` 获取依赖，不直接访问 data 层。

包结构（`com.xingclass.app`）：

```text
core/             AppResult / AppError / Logger / AppClock / DiagnosticsLog
domain/model/     Course、CourseColor、CourseWeek、CourseCacheSnapshot、SessionStatus、
                  PeriodTime、GridSpec、CourseMerger、ChaoxingKey、ThemeMode
domain/repository/ AuthRepository、CourseRepository、SettingsRepository、DiagnosticsRepository
data/remote/      ChaoxingApi、ChaoxingApiClient、ChaoxingEndpoints、CookieJar、远端数据源、DTO/Parser
data/mapper/      CourseMapper、CourseTimeParser
data/local/       CourseCacheStore / FileCourseCacheStore
data/repository/  Repository 的 data 层实现
security/         ChaoxingCrypto、KeystoreSecureStorage、CredentialStore
ui/theme/         DesignTokens、XingClassTheme
ui/components/    neumorphic 基础组件、品牌/密钥编辑器、schedule 课表组件
ui/schedule/      ScheduleLayoutCalculator、ScheduleUiState（纯逻辑）
ui/screens/       login / schedule / settings
ui/root/          RootViewModel、XingClassRoot（目的地状态机）
ui/util/          UiText、日期/地点/账号等格式化
```

设计取舍：

- **手动 DI**：依赖图小，避免 KSP/注解处理；对象增多时再评估框架。
- **JSON 文件缓存**：单份课表无需数据库；临时文件 + rename 原子写，坏文件按 cache miss 处理。
- **security-crypto**：`EncryptedSharedPreferences` 管理 Keystore 主密钥；该库处于维护模式，
  已封装在 `KeystoreSecureStorage`，替换只影响一个类。
- **不持久化密码**：只保存账号提示与 Cookie；密码仅存在于登录 ViewModel 内存中。
- **容错解析**：以 `data.lessonArray` 真实结构优先，保留字段别名；无法定位的条目丢弃而不是猜测。
- **三态鉴权**：`getauthstatus/v2` 在有效会话下也可能返回“参数为空”，因此使用
  `VALID / INVALID / UNKNOWN`，只有明确失效才登出。
- **自定义 Layout**：7×10 固定网格，无 Lazy/Scroll；重叠课程按区间着色分列。
- **单 Activity 状态机**：登录 / 课表 / 设置 / 抽屉四个目的地，无 Navigation 依赖。
- **Material3 限量使用**：仅 Text/Icon 语义与主题色；可见表面全部由 `NeumorphicSurface` 绘制。
- **发布必须显式签名**：不提供 debug 签名回退，缺少 keystore 时 release 打包直接失败。

## 4. 会话与数据流

```text
App 启动
  └─ RootViewModel.bootstrap()
       ├─ 无本地 Cookie ─────────────────→ Login
       ├─ 有 Cookie → GET getauthstatus/v2
       │    ├─ VALID   ─────────────────→ Schedule
       │    ├─ INVALID → 清 Cookie ─────→ Login（提示会话失效）
       │    └─ UNKNOWN（无法判断/断网）──→ Schedule（显示缓存）
       └─ 异常不崩溃，进入 Login 或 Schedule

ScheduleViewModel.init()
  ├─ getCachedCourses() → 立即渲染缓存
  └─ refreshCourses(null) → 成功更新；失败保留缓存并标记 Offline
```

- 缓存位置：`filesDir/course-cache.json`，结构
  `{version, savedAtMillis, courses[], week, realCurrentWeek, maxWeek, lessonTimes[]}`，当前 version 3。
- 远程失败不清空缓存；只有“成功且为空”才缓存空列表。
- 周次切换：`week` 为 1-based 参数；响应中的 `currentWeek` 始终是真实本周，
  客户端单独维护“选中周 / 真实本周 / 最大周”。
- 会话失效：401/403、登录页 HTML、失效文案 → `SessionExpired` → 清理本地会话并回到登录页。
- 诊断：`AndroidLogger` 只写固定事件名、异常类名与周次/数量；
  导出由 `DiagnosticsRepository` 生成，经 FileProvider 交给系统分享面板。
- 线程：网络调用在 `Dispatchers.IO`（OkHttp `callTimeout` 30s，无业务重试）；缓存 IO 同样在
  `Dispatchers.IO`；自动主题与“今天”列每 60 秒检查一次。
- ViewModel 按 `sessionKey` 隔离，登出/换号后重建；状态通过 `StateFlow` +
  `collectAsStateWithLifecycle` 收集。
- 诊断日志上限 256KB、内存保留 500 条；导出文件写入 `cache/exports`，经 FileProvider
  临时授权给分享目标；下次导出时会清理超过 24 小时的旧文件。

错误模型：

| `AppError` | 触发 | UI 文案 |
|---|---|---|
| `Network` | IOException / 连接失败 / 超时 | 网络不可用，请检查网络连接 |
| `InvalidCredentials` | 登录响应含密码/账号错误 | 账号或密码错误 |
| `LoginFailed` | 其他登录失败（验证码等） | 登录失败，请稍后重试 |
| `SessionExpired` | 401/403、登录页 HTML、鉴权失败文案 | 登录已失效，请重新登录 |
| `CourseFetch` | 响应无法解析出课程结构 | 课程获取失败，请稍后重试 |
| `Server` | 非 2xx | 服务暂时不可用，请稍后重试 |
| `NoData` | 导出时没有缓存课程 | 暂无可导出的课程数据 |
| `Unknown` | 兜底 | 出了点问题，请稍后重试 |

底层异常类型只进入 Debug 日志（且只记录异常类名），不进入 UI。

## 5. UI 规范

优先级：

```text
信息架构 / 一屏可读性  >  新拟物空间感  >  动效
```

新拟物是补充，不允许为了“纯新拟物”破坏课表可读性。课程卡使用低饱和色底 + 高对比文字，
而不是和背景完全同色。

### 5.1 光源与阴影

固定光源：**左上亮、右下暗**，任何组件都不得反向。

| 状态 | 表现 | 实现 |
|---|---|---|
| Raised | 暗阴影 +offset/+offset，亮阴影 −offset/−offset | `setShadowLayer` 两次绘制轮廓 |
| Inset | 内凹环：暗偏移进入左上内缘，亮偏移进入右下内缘 | 外轮廓 − 内缩轮廓 = 环，带 shadow layer 并裁剪 |
| Pressed | Raised 阴影淡出、Inset 淡入，**无 translate** | `pressProgress` 0→1，300ms |

阴影档位：

| 档位 | offset | blur | inset depth |
|---|---|---|---|
| SMALL | 4dp | 9dp | 4dp |
| MEDIUM | 7dp | 14dp | 7dp |
| LARGE | 10dp | 22dp | 10dp |

统一入口：

- `Modifier.neumorphic(shape, style, backgroundColor, lightShadow, darkShadow, elevation, pressProgress, shadowScale)`
- `NeumorphicSurface`、`NeumorphicButton`、`NeumorphicIconButton`、`NeumorphicTab`、`NeumorphicBadge`
- 档位来自 `NeumorphicElevation`，颜色/间距/圆角来自 `DesignTokens.kt`

### 5.2 颜色

| Token | Light | Dark |
|---|---|---|
| background / surface | `#E0E5EC` | `#2B3038` |
| lightShadow | `#FFFFFF` | `#434D5A` |
| darkShadow | `#A6ADB8` | `#15191F` |
| textPrimary | `#39414F` | `#E3E8EF` |
| textSecondary | `#586172` | `#A3ADBB` |
| accent | `#5949D2` | `#9B90FF` |
| danger | `#9E3E3A` | `#E08580` |
| scrim | `#2A3040` @40% | `#0B0D10` @55% |

课程色 8 组（低饱和底色 + 高对比文字）：

| # | Light bg / text | Dark bg / text |
|---|---|---|
| 1 Lavender | `#DBD8EC` / `#564A96` | `#3A3550` / `#C0B8EE` |
| 2 Mint | `#D4E4DA` / `#356A4E` | `#2F3E37` / `#9CCFB1` |
| 3 Peach | `#EADBD2` / `#82452E` | `#43342E` / `#E0AF98` |
| 4 Sky | `#D5E1EB` / `#315D79` | `#2C3A45` / `#A3C6E0` |
| 5 Sand | `#E7E1D1` / `#6A582A` | `#3E392D` / `#D4C49B` |
| 6 Rose | `#EAD7DE` / `#82405A` | `#43313B` / `#E0ACBF` |
| 7 Teal | `#D1E3E3` / `#2C6060` | `#2D3F3F` / `#9BCCCC` |
| 8 Olive | `#E0E4CE` / `#545E2B` | `#3A3D2E` / `#C3CDA2` |

课程颜色由课程标题哈希决定，同一门课在刷新后保持同一颜色；正文对比度 ≥ 4.5:1。

### 5.3 圆角与字号

| Token | 值 |
|---|---|
| small | 12dp |
| medium | 16dp |
| large | 20dp |
| xlarge | 28dp（抽屉顶部） |
| pill | 100dp（胶囊/徽标） |

字号：wordmark 26sp、页面标题 20sp、正文 14sp、说明 11sp、网格课程名 10sp（紧凑 9sp）、
网格地点 9sp、节数 12sp、节次时间 8sp。主课表禁止滚动，网格字号必须小到能容纳 7×10。

### 5.4 三层嵌套

```text
左侧节数方块：独立 Raised
课表底座：整块 Inset
课程卡片：从底座中 Raised
单日抽屉：外壳同背景色（无阴影）→ 内部大 Inset 槽 → 课程 Raised
```

对应组件：

- `PeriodColumn`：10 个独立 Raised 方块
- `ScheduleGrid`：外层 `INSET` + `LARGE`，内部裁剪后放卡片
- `CourseCard`：Raised + SMALL
- `DayDrawer`：`background = colors.background`（无阴影），内部 inset 容器，行 Raised

### 5.5 主课表布局

- 根：`Column`，标题栏、周次栏、星期栏高度固定，网格行 `weight(1f)` 占满剩余高度
- 网格：**自定义 `Layout`**，高度均分 10 行、宽度均分 7 列
- 课程位置完全由 `day/start/span` 计算（`ScheduleLayoutCalculator`，纯 Kotlin，已单测）
- 左侧节数栏：节数下方显示上课/下课时间；极小高度下自动隐藏时间，只保留节数
- 课程抽屉：左侧时间列显示整段课程的上课/下课时间，不再显示“1-2 节”
- 相同内容且时间连贯（相邻节次、实际间隔 ≤30 分钟）的课程由数据层合并
- 不使用 `LazyColumn` / `LazyRow` / `verticalScroll` 作为主课表容器
- 重叠课程自动分列（贪心区间着色），不会互相覆盖
- 卡片高度由跨节数决定，文字行数根据实际测量高度动态限制
- 网格内边距 8dp，抽屉内凹槽内边距 12dp、行间距 10dp

### 5.6 文本截断

- 课程卡标题：`maxLines = 动态(1..5)`，`overflow = Ellipsis`
- 课程卡地点：按实际测量高度与实际行高（含 fontScale）决定显示：
  - 空间足够：楼栋一行 + 教室一行；
  - 只够一行：显示完整地点（省略号）；
  - 完全不够：隐藏地点，但至少保留 1 行标题。
- 抽屉课程：标题一行；第二行为“楼栋 · 教室 · 教师”单行（`maxLines = 1` + `Ellipsis`）；
  左列宽 54dp，显示上课/下课时间（无时间时显示“—”）
- 任何文字都不贴边：卡片内边距 4dp/3dp，抽屉行 12dp/11dp

### 5.7 按压与动效

- 所有可点击元素：Raised → Inset，300ms，ease-in-out
- 输入框聚焦：内凹**变浅**（`shadowScale 1.0 → 0.55`），表示“通道打开”
- 不可用的操作按钮：`insetWhenDisabled = true` 时直接呈内凹（文字用 `textTertiary`）
- 登录密钥弹窗外壳使用 `NeumorphicStyle.FLAT`（无外阴影），避免在 Dialog 遮罩上产生散光
- 抽屉从底部滑入并淡出遮罩；刷新图标仅刷新中旋转，900ms/圈
- 加载态：三个呼吸圆点，不使用 Material 转圈
- 动画不影响布局：阴影/透明度动画不改变尺寸

### 5.8 主题与状态

- 三态：自动 / 白天 / 黑夜，持久化在设置中
- 自动：08:00–22:00 白天，其余黑夜；`RootViewModel` 每 60 秒检查一次
- 深色不是简单反色：背景、亮/暗阴影、文字、课程色、accent 全部重新映射
- 主题切换后系统状态栏图标同步

| 页面 | 状态 |
|---|---|
| 登录 | Idle / Loading / Success / Error |
| 课表 | Loading / Cached / Refreshing / Success / Empty / Error / Offline |
| 抽屉 | Open（选中 Tab 内凹）/ Closed（取消高亮）/ 空日 |
| 设置 | 同步时间 / 清除缓存反馈 / 密钥保存反馈 / 退出确认 |

设置页分组：外观 → 账号 → 登录密钥 → 数据 → 诊断 → 关于。登录密钥输入必须是 trim 后
恰好 16 个字符；设置页与登录页弹窗共用同一套校验与按钮状态规则。

### 5.9 可访问性

- 图标按钮提供 content description；按钮/标签控件使用 `Role.Button` / `Role.Tab` 语义
- 文字对比度：正文与课程名按 WCAG AA（≥ 4.5:1）选取配色
- 点击区域：图标按钮 30–40dp，Tab 高 38dp，课程卡占满网格单元
- 不使用颜色作为唯一信息：今日列同时有圆点标记，离线同时有文字徽标

### 5.10 禁止清单

- 主课表不使用滚动容器
- 不使用直角卡片、粗边框、纯黑/纯白主体背景
- 不使用单层扁平阴影或位移模拟按压
- 阴影参数不散落在组件内，统一走 `NeumorphicElevation` 与 `DesignTokens`

## 6. 安全

### 6.1 威胁模型

XingClass 直接与超星通信，登录凭据和会话 Cookie 只存在于用户设备上。需要防的主要风险：

- 设备丢失/被他人短暂接触时，读取本地凭据或会话；
- 日志、崩溃信息、备份、截图泄露敏感数据；
- 通过明文 HTTP、自建代理或第三方 SDK 外泄账号；
- 仓库或 CI 中误提交真实账号、Cookie、签名密钥；
- 会话失效后无限重试或错误地保留失效凭据。

不覆盖：已 root 设备、系统级恶意软件、用户主动导出私有目录等超出常规应用边界的情况。

### 6.2 本地存储

| 数据 | 位置 | 加密 | 说明 |
|---|---|---|---|
| 账号提示 | `EncryptedSharedPreferences` | Android Keystore AES-256-GCM（值）/ AES-256-SIV（键） | 仅用于登录页预填和设置页脱敏展示 |
| 密码 | **不存储** | — | 只存在于登录 ViewModel 内存中，登录成功即清空 |
| 会话 Cookie | `EncryptedSharedPreferences` | 同上 | JSON 序列化后加密；`CookieJar` 统一读写 |
| 课程缓存 | `filesDir/course-cache.json` | 否 | 仅为课程名、地点、教师，不是认证信息 |
| 主题偏好 | 普通 SharedPreferences | 否 | 非敏感 |
| 登录加密密钥（可配置） | 普通 SharedPreferences | 否 | 超星前端公开 AES 混淆串，不是认证信息；默认值见 `ChaoxingKey.DEFAULT` |

登录加密密钥只影响登录表单的 AES 加密，不参与本地认证；它不写入日志，也不进入导出文件。
密钥非法（trim 后不是 16 个字符）时不会落盘，设置页/登录页回退显示当前有效值。

禁止：

- 明文 SharedPreferences / JSON / 数据库保存账号、密码、Cookie、Token；
- 将密码写入 `SavedStateHandle`、Bundle 或日志；
- 将真实测试凭据写入源码、注释、测试、README、Issue。

Keystore 不可用或数据损坏时：

1. 记录 `SECURE_STORAGE_FAILED`（仅异常类名）；
2. 删除并重建加密文件；
3. 仍失败则退化为**进程内内存存储**（不落盘、退出即失），绝不回退到明文。

### 6.3 网络

- 全部请求使用 HTTPS；`android:usesCleartextTraffic="false"`。
- 未集成任何第三方统计、广告、推送、崩溃上报 SDK。
- 不使用自建服务器/代理；请求直接从设备发往超星域名。
- 不实现 SSL Pinning，也不提供“忽略证书错误”选项。
- 登录使用 `FormBody` 提交 AES 加密后的账号密码；HTTP 层不缓存请求体。
- OkHttp 仅启用其内置的有限重试（`retryOnConnectionFailure`），业务层没有重试循环。

### 6.4 Cookie / Session 生命周期

```text
登录成功
  → Set-Cookie 由 ChaoxingCookieJar 接收
  → 按 (domain, path, name) 合并、剔除过期项
  → 加密持久化
  → 后续请求按 OkHttp 匹配规则自动附加
  → Cookie 只发送给匹配的域

启动
  → 有 Cookie：调用 getauthstatus/v2
      VALID   → 课表
      INVALID → 清除 Cookie → 登录页（提示已失效）
      UNKNOWN → 课表（断网可用缓存，不误登出）

请求课程返回 401/403 或登录页 HTML
  → SessionExpired → 清除 Cookie → 提示重新登录
  → 不自动重试登录，不无限循环

退出登录
  → 清除 Cookie + 账号提示 + 课表缓存 → 登录页
```

Cookie 日志策略：**任何级别都不输出 Cookie 名称、值、请求头**。

### 6.5 日志脱敏

允许：

```text
LOGIN_REQUEST_STARTED / LOGIN_SUCCESS / LOGIN_FAILED
SESSION_CHECK_STARTED / SESSION_CHECK_VALID / SESSION_CHECK_INVALID / SESSION_CHECK_UNKNOWN
SESSION_CLEARED
COURSE_FETCH_STARTED / COURSE_FETCH_SUCCESS / COURSE_FETCH_FAILED
COURSE_CACHE_HIT / COURSE_CACHE_MISS / COURSE_CACHE_WRITTEN / COURSE_CACHE_CLEARED
SECURE_STORAGE_FAILED
APP_STARTED / EXPORT_LOGS / EXPORT_COURSES / EXPORT_FAILED
```

事件可附加安全细节，如 `week=10 count=14`（仅周次与数量，不含课程名）。

禁止：账号、密码、Cookie、Token、Authorization、完整 URL 敏感参数、完整响应体。
异常只记录 `Throwable` 的**类名**（如 `UnknownHostException`），不记录 message
（可能包含 URL 或响应片段）。Debug 构建同时输出 Logcat；所有构建都会把上述安全事件写入
本地诊断日志，Release 不输出 Logcat。

### 6.6 系统层保护

- `android:allowBackup="false"`，并额外提供 `data_extraction_rules.xml` 排除全部本地数据；
- 不导出除 `MainActivity` 以外的组件；
- 不申请定位、通讯录、存储等无关权限，仅 `INTERNET`；
- 不开启 `android:debuggable`（release 默认 false）；
- 未添加 `FLAG_SECURE`：Compose 界面可能出现在最近任务/截图中。课表本身非敏感，
  但包含教师与教室；如学校有更高要求，可在 `MainActivity` 开启。

### 6.7 CI 与签名

- 仓库与 CI 日志中不得出现真实账号、密码、Cookie。
- Release 签名通过 GitHub Secrets 注入：

| Secret | 用途 |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | base64 后的 keystore 文件 |
| `RELEASE_KEYSTORE_PASSWORD` | keystore 密码 |
| `RELEASE_KEY_ALIAS` | key alias |
| `RELEASE_KEY_PASSWORD` | key 密码 |

- 四个 Secret 缺一不可；缺少时 Release workflow 在构建前失败。
- Release 构建不再回退 debug 签名；本地 `assembleRelease` 缺少 keystore 同样失败。
- `.gitignore` 排除 `*.jks`、`*.keystore`、`keystore.properties`、`local.properties`。
- Release workflow 只能手动触发，并使用 `environment: release` 作为审批卡点。

### 6.8 凭据泄露后的处置

1. 立即修改超星密码（会同时使旧会话失效）；
2. 在设备上退出登录，或在系统设置中清除应用数据；
3. 如泄露的是签名密钥：更换 keystore、更新 Secrets，并作废旧版本分发；
4. 检查 GitHub Actions 日志与仓库历史，确认没有凭据被提交；
5. 不要通过“隐藏日志”来处理已经泄露的凭据——密码必须重置。

### 6.9 已知限制

- 没有生物识别二次验证（超出 MVP 范围）。
- 没有 `FLAG_SECURE`，系统截图/录屏可捕获课表（含教师与教室）。
- 账户提示（非密码）保存在本机，若设备被解锁且应用数据可读，攻击者可见脱敏后的账号。
- 超星接口的成功路径已验证；失败分支（错误密码、验证码、会话过期等）尚未捕获真实响应，
  代码按防御性逻辑处理。

## 7. 构建、运行与测试

前置条件：JDK 17、Android SDK Platform 35、Build-Tools 35.0.0。项目自带 Gradle Wrapper。

```bash
cd android
./gradlew lint            # Android Lint
./gradlew test            # JVM 单元测试
./gradlew assembleDebug   # Debug APK
./gradlew assembleRelease # Release APK（必须配置签名）
```

产物：

```text
android/app/build/outputs/apk/debug/XingClass-arm64-v8a-debug.apk
android/app/build/outputs/apk/release/XingClass-arm64-v8a-release.apk
```

运行：安装 APK（`adb install -r <apk>`），输入超星账号（手机号或学号）与密码。
登录成功后进入课表；之后再次打开会优先显示本地缓存并自动刷新。

测试：140 个离线 JVM 测试，另有 1 个 opt-in 实网测试（`LiveChaoxingApiTest`，默认跳过）。
实网测试通过环境变量 `XINGCLASS_TEST_ACCOUNT` / `XINGCLASS_TEST_PASSWORD` 启用，
凭据不写入仓库或日志。

测试策略：纯逻辑（加密、解析、映射、布局、主题、脱敏、日期、地点）走快速 JVM 单测；
HTTP 行为用 MockWebServer 验证请求方法、表单、加密与 Cookie 回放；Repository 用假远端/假缓存
覆盖成功、失败、缓存回退与会话失效；缓存用临时目录验证原子写与坏文件清理。
完整环境说明、签名与排错见 [docs/BUILD.md](docs/BUILD.md)。

## 8. GitHub Actions

| Workflow | 触发 | 行为 |
|---|---|---|
| `.github/workflows/debug.yml` | push / pull_request / 手动 | lint → test → assembleDebug → 校验 APK 只含 arm64-v8a → 上传 artifact；**不创建 Release** |
| `.github/workflows/release.yml` | **仅 `workflow_dispatch`** | 校验签名 Secrets 与版本 → lint / test / assembleRelease → 校验 APK → 创建 GitHub Release 并上传 APK + SHA-256 |

Release 规则：

1. 手动输入版本号，必须等于 `v` + `versionName`（当前 `versionName` 为 `1.2.0`，即 `v1.2.0`）。
2. `versionCode` 必须大于最近一个 `v*` tag 中的 `versionCode`；没有历史 tag 时跳过该检查。
3. 必须配置四个签名 Secrets，且仓库已创建 `release` environment（建议配置审批人）。
4. 缺少签名时 Release 直接失败，不会产出 debug 签名的发行包。

Debug 流程永远不会发布 Release；Release 只能由人工手动触发。

## 9. 目录结构

```text
XingClass/
├── android/
│   ├── app/                    # 单模块应用
│   ├── gradle/                 # 版本目录 + wrapper
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── docs/
│   ├── API.md                  # 超星接口契约
│   └── BUILD.md                # 构建、签名与 CI
├── .github/workflows/
│   ├── debug.yml
│   └── release.yml
├── README.md
├── LICENSE
└── .gitignore
```

## 10. 已知限制

- 未做国际化：当前仅简体中文。
- 未做 instrumented / Compose UI 测试，也未在真机上完成视觉验收；
  一屏布局、无滚动、文字截断由代码约束与单元测试保证布局数学。
- 超星接口的成功路径已验证；错误密码、验证码、双因子、会话过期等失败分支尚未捕获真实响应，
  代码采用防御性处理（详见 [docs/API.md](docs/API.md)）。
- 仅支持 arm64-v8a；无 x86 / x86_64 / armeabi-v7a 产物。
- 未在真机上完成端到端操作验证；网络层已通过实网测试验证。
- “导出课程数据”导出的是解析后的课表数据（周次、节次时间、课程/地点/教师），
  不是原始 HTTP 响应；原始 body 可能含身份字段，按安全规范不做保留。
- MVP 不包含二维码登录、多账号、提醒等需求范围外功能。

## 11. 免责声明

XingClass 是第三方客户端，与超星学习平台无隶属关系。课程数据来自超星接口；
请遵守学校与平台的使用条款。
