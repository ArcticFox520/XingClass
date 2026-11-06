# XingClass 架构

## 1. 总览

```text
┌──────────────────────────── UI (Compose) ───────────────────────────┐
│  LoginScreen        ScheduleScreen / DayDrawer       SettingsScreen │
│        │                    │                              │       │
│  LoginViewModel      ScheduleViewModel               SettingsViewModel
│        │                    │                              │       │
│  RootViewModel（会话门禁 / 主题 / 目的地状态机）                     │
└────────┼────────────────────┼──────────────────────────────┼───────┘
         ▼                    ▼                              ▼
┌──────────────────────── Domain（接口 + 模型） ───────────────────────┐
│ AuthRepository      CourseRepository                 SettingsRepository
└────────┼────────────────────┼──────────────────────────────┘
         ▼                    ▼
┌──────────────────────── Data ────────────────────────────────────────┐
│ AuthRepositoryImpl   CourseRepositoryImpl   SharedPreferencesSettings │
│      │                 │           │                                  │
│ ChaoxingApi      CourseRemote    CourseCacheStore（本地 JSON）        │
│      │              │                                                 │
│ ChaoxingApiClient + ChaoxingCookieJar + 容错解析 Mapper               │
│      │                                                                │
│ 超星 HTTPS  ───────────────────────────────────────────────          │
└──────────────────────────────────────────────────────────────────────┘
         ▲
┌────────┴──────── Security ───────────────────────────────────────────┐
│ ChaoxingCrypto（AES/CBC/PKCS7+Base64）                                │
│ KeystoreSecureStorage（EncryptedSharedPreferences）                    │
│ CredentialStore（仅账号提示） / EncryptedCookieStore（Cookie）         │
└──────────────────────────────────────────────────────────────────────┘
```

依赖方向严格单向：`UI → ViewModel → Domain → Data → Remote/Local`。
UI 不出现 `OkHttp`、`JSON`、AES、Cookie；ViewModel 不出现 HTTP/存储实现。

## 2. 包结构

```text
com.xingclass.app
├── XingClassApp.kt              Application，构建 AppContainer
├── MainActivity.kt              单 Activity + edge-to-edge
├── di/AppContainer.kt           手动依赖图
├── core/                        AppResult / AppError / Logger / AppClock
├── domain/
│   ├── model/                   Course、CourseColor、ThemeMode、SessionStatus
│   └── repository/              AuthRepository、CourseRepository、SettingsRepository
├── data/
│   ├── remote/                  Api 接口与实现、Endpoints、CookieJar、远端数据源
│   │   └── dto/                 ChaoxingLessonDto、容错 Parser
│   ├── mapper/                  CourseTimeParser、CourseMapper
│   ├── local/                   CourseCacheStore / FileCourseCacheStore
│   └── repository/              AuthRepositoryImpl、CourseRepositoryImpl、Settings
├── security/                    Crypto、SecureStorage、Keystore 实现、CredentialStore
└── ui/
    ├── theme/                   DesignTokens、XingClassTheme
    ├── components/neumorphic/   统一新拟物绘制与基础组件
    ├── components/schedule/     ScheduleHeader、PeriodColumn、DayTabs、Grid、Drawer
    ├── schedule/                ScheduleLayoutCalculator、ScheduleUiState（纯逻辑）
    ├── screens/                 login / schedule / settings（Screen + ViewModel）
    ├── root/                    RootViewModel、XingClassRoot（导航状态机）
    └── util/                    UiText、日期文案、账号脱敏
```

## 3. 会话与启动流程

```text
App 启动
  │
  ├─ RootViewModel.bootstrap()
  │      ├─ 无本地 Cookie ───────────────→ Login
  │      ├─ 有 Cookie → GET getauthstatus/v2
  │      │      ├─ VALID  ───────────────→ Schedule
  │      │      ├─ INVALID → 清 Cookie ──→ Login（提示会话失效）
  │      │      └─ UNKNOWN（无法判断/断网）→ Schedule（走缓存）
  │      └─ 兜底：异常不崩溃，进入 Login 或 Schedule
  │
  ├─ ScheduleViewModel.init()
  │      ├─ getCachedCourses() → 立即渲染缓存（Cached）
  │      └─ refreshCourses() → 成功更新；失败保留缓存并标记 Offline
```

`UNKNOWN` 的处理是刻意的，并已被真实联调验证：即使 Cookie 会话完全有效，
`getauthstatus/v2` 仍返回 `{"mes":"参数为空","status":false}`。因此不能据此清会话；
宁可进入课表由课程请求再判定，也不错误地把用户登出。

## 4. 数据流

```text
Chaoxing JSON
  → ChaoxingLessonParser（真实容器 data.lessonArray；保留多字段别名兜底）
  → ChaoxingLessonDto（规范化中间态）
  → CourseMapper（day/start/span 校验、时间文本兜底、颜色、去重、排序）
  → Course（domain）
  → ScheduleLayoutCalculator（day/start/span → 网格位置）
  → ScheduleGrid（Compose 自绘布局）
```

UI 永远不直接使用超星 JSON。解析器的容错策略与未验证字段见 `docs/API.md`。

## 5. 缓存

- 位置：`filesDir/course-cache.json`（私有目录，非敏感数据）
- 结构：`{version, savedAtMillis, courses[]}`
- 写入：临时文件 + rename，避免进程中断产生半个缓存
- 读取：反序列化失败或课程越界 → 视为 cache miss 并删除坏文件
- 失败策略：远程失败**绝不**清空缓存；只有明确“成功且为空”才缓存空列表
- 缓存与账号无关字段，但退出登录时会清理，避免换账号后看到旧课表

选择文件而非 Room 的理由见 `docs/DECISIONS.md`。

## 6. 错误模型

| `AppError` | 触发 | UI 文案 |
|---|---|---|
| `Network` | IOException / 连接失败 / 超时 | 网络不可用，请检查网络连接 |
| `InvalidCredentials` | 登录响应含密码/账号错误 | 账号或密码错误 |
| `LoginFailed` | 其他登录失败（验证码等） | 登录失败，请稍后重试 |
| `SessionExpired` | 401/403、登录页 HTML、鉴权失败文案 | 登录已失效，请重新登录 |
| `CourseFetch` | 响应无法解析出课程结构 | 课程获取失败，请稍后重试 |
| `Server` | 非 2xx | 服务暂时不可用，请稍后重试 |
| `Unknown` | 兜底 | 出了点问题，请稍后重试 |

底层异常类型只进入 Debug 日志（且只记录异常类名），不进入 UI。

## 7. 线程与生命周期

- 网络：OkHttp 同步调用包在 `Dispatchers.IO`，`callTimeout=30s`，无自定义重试循环
- 缓存 IO：`Dispatchers.IO`，可注入测试调度器
- 自动主题：`RootViewModel` 每 60 秒检查一次当前小时，不按秒刷新
- 日期：`ScheduleViewModel` 每 60 秒检查跨天，更新“今天”列
- ViewModel 按会话 key 隔离（`sessionKey`），登出/换号后会创建全新实例
- 所有页面状态在 `StateFlow` 中，通过 `collectAsStateWithLifecycle` 收集

## 8. 依赖及引入理由

| 依赖 | 理由 |
|---|---|
| Compose BOM / UI / Material3 | 原生声明式 UI；Material3 只用于 Text/Icon 的语义与 `LocalContentColor`，不使用其阴影/卡片 |
| activity-compose, lifecycle-* | 标准单 Activity 与 ViewModel/状态收集 |
| OkHttp | 成熟 HTTP 客户端，内置 CookieJar 抽象与超时控制 |
| kotlinx.serialization | 编译期序列化，缓存与 JSON 解析均可在 JVM 单元测试中运行 |
| androidX security-crypto | Keystore 主密钥 + AES-GCM 加密 SharedPreferences，避免手写密码学 |
| material-icons-core | 刷新/设置/关闭/返回四个内置矢量图标，免去自绘资产 |
| JUnit4 + coroutines-test + MockWebServer | 单元测试、协程调度、真实 HTTP 行为验证 |

**未引入**：Hilt（依赖少，手动 DI 更直接）、Room（单表课表无需数据库）、Navigation（4 个目的地用状态机足够）、Retrofit（接口少，OkHttp + 手写解析更可控）、二维码/推送等。

## 9. 测试策略

- 纯逻辑（Crypto、Parser、Mapper、Layout、AutoTheme、Mask）→ 快速 JVM 单元测试
- 真实结构回归：把 2026-11-06 实网响应脱敏后固化进 `ChaoxingLessonParserTest`
- 实网集成：`LiveChaoxingApiTest` 默认跳过，凭据仅通过环境变量注入
- HTTP 行为 → MockWebServer 验证请求方法/表单/加密/Cookie 回放/错误映射
- Repository → 假远端 + 假缓存，覆盖成功、失败、缓存回退、会话失效
- 缓存 → JUnit TemporaryFolder 验证原子写、坏文件清理
- 未做 instrumented/UI 测试（构建环境无 KVM，无法启动模拟器），在 README 与最终报告中注明
