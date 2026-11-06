# XingClass

XingClass 是一个面向学生的**超星课程表 Android 客户端**。它不是网页壳，也不是课程表 Demo：
整个界面由 Kotlin + Jetpack Compose 原生绘制，采用**一屏全览**的新拟物（Neumorphism）课表，
课程数据由 App **直接**访问超星接口获取，没有自建后端服务器。

> 核心体验：打开 App，不滚动、不停顿地看到今天/本周全部课程；登录状态、课程缓存都留在本机。

---

## 1. 功能范围（MVP）

已实现：

- 超星账号密码登录（AES/CBC + Base64 加密后提交，与应用内逆向分析结果一致）
- 会话 Cookie 由统一 `CookieJar` 管理，并在 Android Keystore 加密后持久化
- 登录状态检查与失效处理（失效自动回到登录页）
- 拉取课程 → 映射为统一 `Course` 模型 → 本地 JSON 缓存
- **Cache First + Remote Refresh**：先展示缓存，再后台刷新；刷新失败保留旧课表
- 一屏 7×10 课表：无主滚动、10 节均分、课程跨节、星期定位、今日高亮
- 单日课程抽屉（点击星期 Tab 或任意课程打开）
- 自动 / 白天 / 黑夜三态主题（自动模式 08:00–22:00 为白天，每分钟检查一次）
- 设置页：主题、账号（脱敏）、最近同步时间、清除缓存、退出登录
- 空 / 加载 / 缓存 / 刷新中 / 成功 / 失败 / 离线全部页面状态
- 单元测试覆盖加密、解析、映射、布局、Cookie、Repository、自动主题等
- GitHub Actions：Push/PR 自动 Debug 构建；**仅手动触发**的 Release workflow

第一版明确**不做**：二维码登录、独立后端、注册、云同步、多账号、社交、成绩分析、推送、课程提醒。

## 2. 技术栈

- Kotlin 2.0.21 / Jetpack Compose（BOM 2024.12.01）/ Material3（仅用基础文本与图标语义）
- minSdk **26**，targetSdk / compileSdk **35**
- 仅 **arm64-v8a**（Gradle `abiFilters` + 产物命名；APK 内不含任何第三方 ABI 原生库）
- OkHttp 4.12（网络 + CookieJar）、kotlinx.serialization（缓存 / JSON 解析）
- AndroidX Security Crypto（Keystore 支撑的加密存储）
- 手动依赖注入（`AppContainer`），无 Hilt/KSP

架构与依赖理由见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) 与 [docs/DECISIONS.md](docs/DECISIONS.md)。

## 3. 如何构建

前置条件：JDK 17、Android SDK Platform 35、Build-Tools 35.0.0。项目自带 Gradle Wrapper。

```bash
cd android
./gradlew lint          # Android Lint
./gradlew test          # JVM 单元测试
./gradlew assembleDebug # Debug APK
./gradlew assembleRelease # Release APK
```

产物：

```text
android/app/build/outputs/apk/debug/XingClass-arm64-v8a-debug.apk
android/app/build/outputs/apk/release/XingClass-arm64-v8a-release.apk
```

更完整的环境说明、签名配置和排错见 [docs/BUILD.md](docs/BUILD.md)。

## 4. 如何运行

1. 安装上面的 APK（`adb install -r <apk>`）。
2. 打开 XingClass，输入**超星账号**（手机号或学号）和密码。
3. 登录成功后进入课表；之后再次打开会优先显示本地缓存并自动刷新。

建议使用真实账号在真机验证。仓库与 CI 中**没有任何测试账号或密码**。

## 5. 支持的 Android 版本与架构

| 项目 | 值 |
|---|---|
| 最低版本 | Android 8.0（API 26） |
| 编译/目标版本 | Android 15（API 35） |
| CPU 架构 | arm64-v8a |
| 屏幕 | 竖屏为主，横屏同样是一屏布局（课程卡自适应宽度） |

## 6. 如何登录

- 账号和密码先经 `ChaoxingCrypto` 做 AES/CBC/PKCS7 + Base64 加密，再以
  `application/x-www-form-urlencoded` 提交到 `https://passport2.chaoxing.com/fanyalogin`。
- 登录成功后 Cookie 立即保存在内存与加密存储中，后续请求复用同一会话。
- **密码不会被持久化**；本机只保存加密后的账号提示和加密后的 Cookie。

算法、接口字段与尚未验证的部分见 [docs/API.md](docs/API.md)。

## 7. 如何运行测试

```bash
cd android
./gradlew test
# 报告：app/build/reports/tests/testDebugUnitTest/index.html
```

测试覆盖：

- `ChaoxingCryptoTest`：与**独立实现**（Python cryptography）生成的 AES 向量比对
- `ChaoxingLessonParserTest` / `CourseMapperTest`：多种响应包装、字段别名、跨节、缺失地点、时间文本
- `ScheduleLayoutCalculatorTest`：day/start/span → 网格位置、重叠分列、越界裁剪
- `ChaoxingCookieJarTest`：域隔离、过期、持久化、清理
- `ChaoxingApiClientTest`：MockWebServer 验证登录表单、加密、Cookie 复用、会话失效、解析
- `CourseRepositoryImplTest`：远程成功/失败、缓存命中/保留、会话失效
- `AuthRepositoryImplTest`：登录、账号保存、Session 状态机
- `AutoThemeTest` / `AccountMaskTest` / `CourseCacheStoreTest`
- `LiveChaoxingApiTest`：**opt-in 实网集成测试**，默认跳过；使用真实 `ChaoxingApiClient`
  完成登录 → Cookie 会话 → 拉取课表 → 映射网格，断言解析数 == 映射数。

实网测试（凭据只放在环境变量，不写入仓库）：

```bash
export XINGCLASS_TEST_ACCOUNT=你的账号
export XINGCLASS_TEST_PASSWORD=你的密码
cd android
./gradlew testDebugUnitTest --tests '*LiveChaoxingApiTest' --no-daemon
```

## 8. GitHub Actions

| Workflow | 触发 | 行为 |
|---|---|---|
| `.github/workflows/debug.yml` | push / pull_request / 手动 | lint → test → assembleDebug → 上传 Debug APK artifact；**不创建 Release** |
| `.github/workflows/release.yml` | **仅 `workflow_dispatch`** | lint → test → assembleRelease → 校验 APK → 创建 GitHub Release 并上传 APK |

Debug 流程永远不会发布 Release。Release 只能由人工手动触发。

### 如何手动创建 Release

1. 进入 GitHub → Actions → **Release** → Run workflow。
2. 输入版本号，例如 `v1.0.0`（必须匹配 `vX.Y.Z`）。
3. （可选，推荐）配置签名 Secrets：`RELEASE_KEYSTORE_BASE64`、`RELEASE_KEYSTORE_PASSWORD`、
   `RELEASE_KEY_ALIAS`、`RELEASE_KEY_PASSWORD`。
4. 运行结束后会创建 tag `v1.0.0` 的 Release，并上传
   `XingClass-arm64-v8a-release.apk` 与对应的 SHA-256 校验文件。

**未配置签名 Secrets 时，Release APK 会用 debug key 签名**，仅适合内测安装，不可用于正式分发；
workflow 会在 Summary 中给出警告。详见 [docs/SECURITY.md](docs/SECURITY.md)。

## 9. 安全注意事项

- 只保存本地登录状态：账号提示与 Cookie 经 Android Keystore（AES-256-GCM）加密后落盘。
- 不保存明文密码；不打印密码、Cookie、Token 或完整敏感 URL。
- 关闭系统备份（`allowBackup=false`），避免凭据被云备份带出。
- 仅 HTTPS（`usesCleartextTraffic=false`），不信任自定义 CA，不做 SSL Pinning 绕过。
- 退出登录会清除 Cookie、账号提示和课表缓存。
- 仓库内不存在任何真实账号、密码、Cookie、Keystore；测试凭据不得提交。

完整说明见 [docs/SECURITY.md](docs/SECURITY.md)。

## 10. 构建验证结果（本仓库实测）

在 Debian 12 / JDK 17.0.21 / Gradle 8.11.1 / AGP 8.7.3 / Android SDK 35 环境中执行：

| 命令 | 结果 |
|---|---|
| `./gradlew lint` | **PASS**（0 issues） |
| `./gradlew test` | **PASS**（88 tests，0 failures；1 个 opt-in 实网测试默认跳过） |
| `./gradlew assembleDebug` | **PASS** |
| `./gradlew assembleRelease` | **PASS** |

| 产物 | 大小 | ABI | 签名 |
|---|---|---|---|
| `XingClass-arm64-v8a-debug.apk` | ~11 MB | `native-code: arm64-v8a` | debug |
| `XingClass-arm64-v8a-release.apk` | ~1.3 MB | `native-code: arm64-v8a` | debug（未配置 Release Secrets） |

APK 内唯一的原生库是 `lib/arm64-v8a/libandroidx.graphics.path.so`（Compose 依赖），
没有 `x86` / `x86_64` / `armeabi-v7a`。Release APK 使用 debug key 签名，
上传公开 Release 前必须配置签名 Secrets（见第 8 节）。

### 实网接口验证（2026-11-06）

使用项目提供的测试账号完成了一次真实联调（凭据仅存在于临时环境变量/进程，未写入仓库或日志）：

| 验证项 | 结果 |
|---|---|
| AES 登录真实接口 | 成功，返回 `{"status": true, "url": "..."}` |
| Cookie 会话复用 | 成功，12 个 Cookie，后续课程请求复用同一会话 |
| `getauthstatus/v2` | **登录有效时仍返回“参数为空”**，确认不能用于正向判定会话 |
| `getMyLessons` 真实响应 | 200 JSON，结构为 `data.lessonArray`，共 14 条 |
| 真实课程 → 网格映射 | 14/14 全部映射，无丢失（`LiveChaoxingApiTest`） |
| 真实结构回归样本 | 已脱敏后加入单元测试，锁定 `lessonArray`/`dayOfWeek`/`beginNumber`/`length` |

> 环境没有 `/dev/kvm`，无法运行 Android 模拟器，因此没有执行 instrumented / Compose UI 测试，
> 也没有真机视觉验收。实网验证覆盖了成功路径；错误密码、验证码、会话过期等服务端失败分支
> 未主动触发，代码按防御性逻辑处理并标记 `UNVERIFIED`。

## 11. 已知限制

- 真实接口的成功路径已验证；错误密码、验证码、双因子、会话过期等失败分支未主动触发，
  仍标记 `UNVERIFIED`，代码采用防御性处理（详见 [docs/API.md](docs/API.md)）。
- `getMyLessons` 返回的是 `curTime` 所在周的课程实例，MVP 不提供周次切换。
- 接口已返回节次时间（`lessonTimeConfigArray`），但按移动端 UI 规范只在主课表显示节数，
  未展示具体时钟时间。
- 未做国际化：当前仅简体中文。
- 尚未在真机上做视觉验收；一屏布局、无滚动、文字截断由代码约束与单元测试保证布局数学，
  UI 观感仍建议在真机/模拟器上确认。
- Release APK 在未配置签名 Secrets 时为 debug 签名，只能内测安装，不能正式分发。
- 未在真机上运行 Android 客户端完成端到端操作；网络层已用同一套 Kotlin 代码做过实网验证。
- MVP 不包含二维码登录、多账号、提醒等需求范围外功能。

## 12. 目录结构

```text
XingClass/
├── android/
│   ├── app/                    # 单模块应用
│   ├── gradle/                 # 版本目录 + wrapper
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── docs/
│   ├── ARCHITECTURE.md
│   ├── API.md
│   ├── SECURITY.md
│   ├── UI_GUIDE.md
│   ├── BUILD.md
│   └── DECISIONS.md
├── .github/workflows/
│   ├── debug.yml
│   └── release.yml
├── AGENT_GUIDE.md
├── README.md
├── LICENSE
└── .gitignore
```

## 13. 免责声明

XingClass 是第三方客户端，与超星学习平台无隶属关系。课程数据来自超星接口；请遵守学校与平台的使用条款。
