# 安全设计

## 1. 威胁模型

XingClass 直接与超星通信，登录凭据和会话 Cookie 都只存在于用户设备上。需要防的主要风险：

- 设备丢失/被他人短暂接触时，读取本地凭据或会话；
- 日志、崩溃信息、备份、截图泄露敏感数据；
- 通过明文 HTTP、自建代理或第三方 SDK 外泄账号；
- 仓库或 CI 中误提交真实账号、Cookie、签名密钥；
- 会话失效后无限重试或错误地保留失效凭据。

不覆盖：已 root 设备、系统级恶意软件、用户主动导出私有目录等超出常规应用边界的情况。

## 2. 本地存储

| 数据 | 位置 | 加密 | 说明 |
|---|---|---|---|
| 账号提示 | `EncryptedSharedPreferences` | Android Keystore AES-256-GCM（值）/ AES-256-SIV（键） | 仅用于登录页预填和设置页脱敏展示 |
| 密码 | **不存储** | — | 只存在于登录 ViewModel 内存中，登录成功即清空 |
| 会话 Cookie | `EncryptedSharedPreferences` | 同上 | JSON 序列化后加密；`CookieJar` 统一读写 |
| 课程缓存 | `filesDir/course-cache.json` | 否 | 仅为课程名、地点、教师，不是认证信息 |
| 主题偏好 | 普通 SharedPreferences | 否 | 非敏感 |

禁止：

- 明文 SharedPreferences / JSON / 数据库保存账号、密码、Cookie、Token；
- 将密码写入 `SavedStateHandle`、Bundle 或日志；
- 将真实测试凭据写入源码、注释、测试、README、Issue。

Keystore 不可用或数据损坏时：

1. 记录 `SECURE_STORAGE_FAILED`（仅异常类名）；
2. 删除并重建加密文件；
3. 仍失败则退化为**进程内内存存储**（不落盘、退出即失），绝不回退到明文。

## 3. 网络

- 全部请求使用 HTTPS；`android:usesCleartextTraffic="false"`。
- 未集成任何第三方统计、广告、推送、崩溃上报 SDK。
- 不使用自建服务器/代理；请求直接从设备发往超星域名。
- 不实现 SSL Pinning，也不提供“忽略证书错误”选项：抓包资料中的 PinProxy 只用于逆向分析，
  不进入产品。
- 登录使用 `FormBody` 提交 AES 加密后的账号密码；HTTP 层不缓存请求体。
- OkHttp 仅启用其内置的有限重试（`retryOnConnectionFailure`），业务层没有重试循环。

## 4. Cookie / Session 生命周期

```text
登录成功
  → Set-Cookie 由 ChaoxingCookieJar 接收
  → 按 (domain, path, name) 合并、剔除过期项
  → 加密持久化
  → 后续请求按 OkHttp 匹配规则自动附加
  → Cookie 只发送给匹配的域（host-only Cookie 不会发往 kb.chaoxing.com）

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
`ChaoxingCookieJar.snapshot()` 仅供测试使用，且测试断言不打印值。

## 5. 日志脱敏

允许：

```text
LOGIN_REQUEST_STARTED / LOGIN_SUCCESS / LOGIN_FAILED
SESSION_CHECK_STARTED / SESSION_CHECK_VALID / SESSION_CHECK_INVALID / SESSION_CHECK_UNKNOWN
SESSION_CLEARED
COURSE_FETCH_STARTED / COURSE_FETCH_SUCCESS / COURSE_FETCH_FAILED
COURSE_CACHE_HIT / COURSE_CACHE_MISS / COURSE_CACHE_WRITTEN / COURSE_CACHE_CLEARED
SECURE_STORAGE_FAILED
```

禁止：账号、密码、Cookie、Token、Authorization、完整 URL 敏感参数、完整响应体。
异常只记录 `Throwable` 的**类名**（如 `UnknownHostException`），不记录 message（可能包含 URL 或响应片段）。
Release 构建中 `AndroidLogger` 不输出任何日志（仅 Debug 生效）。

## 6. 系统层保护

- `android:allowBackup="false"`，并额外提供 `data_extraction_rules.xml` 排除全部本地数据；
- 不导出除 `MainActivity` 以外的组件；
- 不申请定位、通讯录、存储等无关权限，仅 `INTERNET`；
- 不开启 `android:debuggable`（release 默认 false）；
- 截图/最近任务缩略图：Compose 界面可能出现在最近任务中，MVP 未额外加 `FLAG_SECURE`
  （会阻止用户截课表）；如学校有更高要求，可在 `MainActivity` 开启。

## 7. GitHub / CI 安全

- 仓库与 CI 日志中不得出现真实账号、密码、Cookie。
- Release 签名通过 GitHub Secrets 注入：

| Secret | 用途 |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | base64 后的 keystore 文件 |
| `RELEASE_KEYSTORE_PASSWORD` | keystore 密码 |
| `RELEASE_KEY_ALIAS` | key alias |
| `RELEASE_KEY_PASSWORD` | key 密码 |

- `.gitignore` 排除 `*.jks`、`*.keystore`、`keystore.properties`、`local.properties`。
- 本地签名使用 `android/keystore.properties`（git 忽略），不得提交。
- 未配置签名 Secrets 时 Release APK 使用 debug key 签名，workflow 会显示警告；
  该产物只能用于内测，不能上传应用商店。
- 真实接口联调只在临时进程环境中使用测试账号；凭据不写文件、不提交、不进日志。
  联调后原始响应已从临时目录删除，提交到仓库的只有脱敏后的结构样本。
- Release workflow 只能手动触发；普通 push/PR 永远不会创建 Release。

## 8. 凭据泄露后的处置

1. 立即修改超星密码（会同时使旧会话失效）；
2. 在设备上退出登录，或在系统设置中清除应用数据（会删除加密 Cookie 与缓存）；
3. 如泄露的是签名密钥：更换 keystore、更新 Secrets，并作废旧版本分发；
4. 检查 GitHub Actions 日志与仓库历史，确认没有凭据被提交；
5. 不要通过“隐藏日志”来处理已经泄露的凭据——密码必须重置。

## 9. 已知限制

- 没有生物识别二次验证（超出 MVP 范围）。
- 没有 `FLAG_SECURE`，因此系统截图/录屏可捕获课表（课表本身非敏感，但含教师与教室）。
- 账户提示（非密码）保存在本机，若设备被解锁且应用数据可读，攻击者可见脱敏后的账号。
- 超星接口的完整响应结构仍未独立验证，解析失败时应视为“获取失败”，不会破坏现有缓存。
