## 0. 给 AI Agent 的执行指令

你正在开发一个名为 **XingClass** 的 Android 应用。

本项目不是普通课程表 Demo，而是一个：

-   Android 原生应用
-   Kotlin + Jetpack Compose
-   Android 26+
-   仅支持 `arm64-v8a`
-   Android 客户端直接访问超星接口
-   不部署独立后端服务器
-   登录信息仅保存在用户设备本地
-   本地缓存课程数据
-   以"一屏全览课表"为核心体验
-   以《工作指导前端》作为主要 UI/UX 规范
-   以《工作指导后端》的超星接口分析作为网络层实现依据
-   以《新拟物》作为视觉补充规范
-   使用 GitHub Actions 自动构建
-   普通代码提交自动执行 Debug 构建/检查
-   **只有人工手动触发 Release workflow 时才创建 GitHub Release 并上传
    Release APK**

### 0.1 核心原则

1.  不要擅自改变已经确定的技术架构。
2.  不要为了方便引入独立服务器。
3.  不要引入二维码登录。
4.  不要把账号、密码、Cookie、Token、测试凭据写入源码。
5.  不要把课表主界面做成普通可滚动列表。
6.  不要为了"现代化"破坏新拟物的核心光影规律。
7.  不要把 Web 设计规范机械照搬成 HTML/CSS；最终 UI 必须是 Jetpack
    Compose 原生 Android 实现。
8.  可以使用成熟第三方库，但每个库都必须有明确理由。
9.  优先简单、可维护、可测试的实现。
10. 如果实现与本文档发生冲突，以本文档为准。
11. 如果本文档没有规定某个细节，不要擅自扩大产品范围；选择最小实现并记录技术决策。

------------------------------------------------------------------------

# 1. 产品目标

## 1.1 产品定位

XingClass 是一个面向学生的超星课程表客户端。

第一版 MVP 的核心任务：

1.  用户输入超星账号密码。
2.  App 直接调用超星登录接口。
3.  登录成功后保持 Session/Cookie。
4.  获取用户课程。
5.  将课程映射成统一的数据模型。
6.  本地缓存课程数据。
7.  主界面以"一屏全览"的方式展示课表。
8.  用户可以切换星期。
9.  用户可以打开单日课程抽屉。
10. 支持自动 / 白天 / 黑夜三态主题。
11. 网络不可用时优先显示最近一次缓存课表。
12. 用户可以手动刷新课表。
13. 登录信息只保存在本地安全存储中。

## 1.2 第一版明确不做

以下功能不属于 MVP：

-   二维码登录
-   独立后端服务器
-   用户注册
-   云同步
-   多账号管理
-   社交功能
-   课程统计分析
-   成绩分析
-   复杂个人中心
-   推送系统
-   复杂通知系统
-   课程提醒系统
-   Web 管理后台

不要为了"以后可能用到"提前实现上述功能。

------------------------------------------------------------------------

# 2. 已确认的技术架构

## 2.1 客户端

必须使用：

-   Kotlin
-   Android
-   Jetpack Compose
-   Android SDK 26+
-   ARM64 `arm64-v8a`

推荐采用清晰的分层结构：

``` text
android/
└── app/
    └── src/
        └── main/
            ├── java/
            │   └── .../
            │       ├── data/
            │       ├── domain/
            │       ├── ui/
            │       ├── security/
            │       └── util/
            └── res/
```

可以根据实际项目调整包结构，但必须保持：

``` text
UI
 ↓
ViewModel / State
 ↓
Domain
 ↓
Repository
 ↓
Remote / Local
```

UI 不得直接操作 HTTP。

## 2.2 网络架构

最终架构：

``` text
Android App
    │
    │ HTTPS
    ▼
超星服务
```

不要创建：

``` text
Android
   ↓
自建 Backend
   ↓
超星
```

因为当前产品明确不需要独立服务器。

原《工作指导后端》中描述的登录、Cookie、AES 和接口访问逻辑，应在 Android
的 `data/remote`、`security` 和 `repository` 层实现。

------------------------------------------------------------------------

# 3. 超星接口实现指导

> 本章节基于项目提供的《工作指导后端》资料。不要自行假定资料中没有明确验证过的接口行为。

## 3.1 登录

接口：

``` text
POST https://passport2.chaoxing.com/fanyalogin
```

Content-Type：

``` text
application/x-www-form-urlencoded; charset=UTF-8
```

核心参数：

``` text
fid
uname
password
refer
t
forbidotherlogin
validate
doubleFactorLogin
independentId
independentNameId
```

资料中已验证用户名和密码不能直接以明文提交，需要先进行加密。

已分析的加密方式：

``` text
AES
CBC
PKCS7
Base64
```

密钥：

``` text
u2oh6Vu^HWe4_AES
```

IV：

``` text
密钥的前 16 字节
```

实现必须封装为独立组件，例如：

``` text
ChaoxingCrypto
```

不要把 AES 实现散落在登录 ViewModel 或 Activity 中。

## 3.2 登录成功

成功响应示例：

``` json
{
  "url": "https://www.chaoxing.com/",
  "status": true
}
```

登录成功之后必须保存 HTTP Session/Cookie 上下文。

不能：

``` text
登录请求结束
↓
丢弃 Cookie
↓
再请求课程
```

必须：

``` text
登录
↓
CookieJar / Session
↓
后续请求复用同一 Session
```

原资料明确指出所有请求需要保持相同的 Session-Cookie 上下文。

## 3.3 鉴权状态

资料提供：

``` text
GET https://passport2.chaoxing.com/getauthstatus/v2
```

该接口用于查询当前用户鉴权状态。

如果实际接口行为与文档存在差异：

1.  不要硬编码假设。
2.  使用日志/测试确认真实响应。
3.  将确认结果记录到 API 文档。
4.  不要泄露账号、密码、Cookie。

## 3.4 Cookie

资料提供：

``` text
POST https://www.chaoxing.com/entry/cookie/get
```

用于读取指定 Cookie。

登录后的 Cookie 必须由网络层统一管理。

推荐抽象：

``` text
ChaoxingCookieJar
```

职责：

-   保存 Cookie
-   为后续请求附加 Cookie
-   清理 Session
-   登录失效时通知上层
-   避免 Cookie 被日志输出

## 3.5 获取课程

核心接口：

``` text
GET https://kb.chaoxing.com/pc/curriculum/getMyLessons
```

参数：

``` text
curTime
```

返回课程数据。

项目内部不要直接使用超星返回 JSON 作为 UI 数据。

必须经过：

``` text
ChaoxingLessonDto
        ↓
CourseMapper
        ↓
Course
        ↓
UI
```

## 3.6 其他接口

资料中还捕获到：

``` text
GET https://kb.chaoxing.com/pc/curriculum/getCurriculumHistory
GET https://kb.chaoxing.com/pc/school/getOpeningStatus
GET https://kb.chaoxing.com/pc/curriculum/getLocationHistoryList
```

第一版可以不实现。

不要因为接口已经发现就自动把它们加入产品。

------------------------------------------------------------------------

# 4. 登录与安全规范

## 4.1 本地账号存储

用户已经确认：

> 登录信息仅保存在本地。

必须使用 Android Keystore 体系保护敏感数据。

推荐：

``` text
Android Keystore
    ↓
加密密钥
    ↓
Encrypted local storage
```

禁止：

-   明文 SharedPreferences
-   明文 JSON
-   明文数据库字段
-   Logcat 输出密码
-   Logcat 输出 Cookie
-   Logcat 输出 Token
-   将账号写入 Git
-   将密码写入 Git
-   将测试凭据写入源码

## 4.2 Session

Cookie 属于敏感认证信息。

必须：

-   本地保存
-   不输出日志
-   不上传第三方服务
-   登录退出时清理
-   Session 失效时触发重新登录

## 4.3 日志

开发日志可以记录：

``` text
LOGIN_REQUEST_STARTED
LOGIN_SUCCESS
LOGIN_FAILED
COURSE_FETCH_STARTED
COURSE_FETCH_SUCCESS
COURSE_CACHE_HIT
```

不得记录：

``` text
password
cookie
session
token
authorization
完整请求 URL 中的敏感参数
完整响应中的敏感字段
```

------------------------------------------------------------------------

# 5. 本地缓存策略

用户已经确认需要本地课表缓存。

## 5.1 目标

App 启动时：

``` text
读取本地缓存
      ↓
立即展示课表
      ↓
尝试刷新远程数据
      ↓
刷新成功 → 更新缓存
刷新失败 → 保留旧缓存
```

因此用户即使暂时没有网络，也可以看到最近一次成功同步的课表。

## 5.2 Repository

推荐：

``` text
CourseRepository
```

统一负责：

``` text
getCourses()
refreshCourses()
getCachedCourses()
clearCourses()
```

UI 不允许直接调用 HTTP。

## 5.3 数据优先级

正常打开：

``` text
Cache First
+
Remote Refresh
```

手动刷新：

``` text
Remote First
+
成功后更新 Cache
```

网络失败：

``` text
显示 Cache
+
提示“使用最近同步的数据”
```

不要因为网络请求失败而清空已有课表。

------------------------------------------------------------------------

# 6. 数据模型

内部统一课程模型：

``` kotlin
data class Course(
    val id: String,
    val day: Int,
    val start: Int,
    val span: Int,
    val title: String,
    val location: String?,
    val teacher: String?,
    val color: CourseColor
)
```

核心字段来源于原前端工作指导：

``` text
day    = 0..6
start  = 开始节次
span   = 跨越节数
title  = 课程名称
loc    = 地点
color  = 颜色标识
```

课程网格必须由：

``` text
day
start
span
```

计算 Grid/Compose 布局位置。

不要通过大量空白占位 View 模拟课程。

------------------------------------------------------------------------

# 7. UI 页面范围

第一版只需要：

``` text
LoginScreen
CourseScheduleScreen
DayDrawer
SettingsScreen
```

## 7.1 LoginScreen

功能：

-   手机号/账号输入
-   密码输入
-   登录按钮
-   登录状态
-   错误提示
-   加载状态

视觉：

-   新拟物输入框
-   新拟物按钮
-   柔和背景
-   不使用传统 Material 卡片的强阴影

## 7.2 CourseScheduleScreen

这是整个产品最重要的页面。

必须优先满足：

> 一屏完整展示课表。

包含：

``` text
顶部
├── XingClass
├── 日期/星期
└── 主题 / 刷新入口

主体
└── 课程网格

底部/覆盖层
└── 单日课程抽屉
```

## 7.3 DayDrawer

点击：

-   星期 Tab
-   任意课程

都可以打开单日课程抽屉。

抽屉：

``` text
全局背景
    ↓
内部大凹槽
    ↓
课程卡片凸起
```

关闭后恢复普通状态。

------------------------------------------------------------------------

# 8. 主课表布局硬性规则

原设计明确要求：

-   课表主体占满剩余屏幕高度
-   10 节课均分
-   不出现纵向/横向滚动
-   移动端缩小字号
-   必要时只显示节数而隐藏具体时间范围

Compose 实现时不要机械使用 Web CSS。

将其转译为：

``` text
BoxWithConstraints
    ↓
计算可用高度
    ↓
10 个等高 Row
    ↓
课程按 day/start/span 放置
```

推荐使用：

``` text
ConstraintLayout for Compose
```

或：

``` text
Custom Layout
```

或者经过充分验证的 Compose Grid 实现。

重点不是具体库，而是最终视觉必须满足：

``` text
一屏
无主滚动
10 节均分
课程正确跨节
星期正确定位
```

## 8.1 禁止

主课表不得使用：

``` text
LazyColumn
LazyRow
verticalScroll()
horizontalScroll()
```

作为主要课表容器。

如果某些设置页需要滚动，可以正常滚动。

"禁止滚动"只针对核心课表全览体验。

------------------------------------------------------------------------

# 9. 文本截断

原设计对文本截断要求非常明确。

课程名称：

``` text
单行或指定行数
超出 → ...
```

地点：

``` text
单行
超出 → ...
```

Compose 使用：

``` kotlin
maxLines = 1
overflow = TextOverflow.Ellipsis
```

课程详情可以允许更大的显示空间。

禁止：

-   文字溢出卡片
-   文字撑破布局
-   突然截断
-   文字贴边
-   因超长课程名导致整个 Grid 变形

------------------------------------------------------------------------

# 10. 视觉设计总原则

## 10.1 优先级

视觉规范优先级：

``` text
工作指导前端
      ↓
新拟物设计规范
      ↓
Android / Compose 实现方式
```

也就是说：

> 新拟物是补充，不允许为了追求"纯新拟物"破坏课程表的信息架构和可读性。

## 10.2 新拟物核心

必须遵守：

``` text
左上 = 光
右下 = 暗
```

外凸：

``` text
dark +x +y
light -x -y
```

内凹：

``` text
inset dark +x +y
inset light -x -y
```

## 10.3 背景

浅色：

``` text
#e0e5ec
```

深色：

``` text
#2b3038
```

不要使用纯白作为主要背景。

不要使用纯黑作为主要背景。

## 10.4 边框

新拟物组件：

``` text
border = none
```

禁止使用粗实线边框表达组件边界。

## 10.5 圆角

推荐：

``` text
16dp ~ 20dp
```

禁止直角卡片。

## 10.6 阴影

禁止使用普通 Material 风格：

``` text
shadow-sm
shadow
shadow-md
shadow-lg
shadow-xl
```

Compose 中必须建立统一的：

``` text
NeumorphicElevation
```

或：

``` text
NeumorphicShadow
```

系统。

不要让每个组件自行写不同的阴影参数。

------------------------------------------------------------------------

# 11. Compose 新拟物实现

推荐建立统一 Modifier：

``` kotlin
Modifier.neumorphic(...)
```

支持：

``` text
Raised
Pressed
Inset
Flat
```

例如概念上：

``` text
Raised
    ↓
外凸双阴影

Inset
    ↓
内凹双阴影

Pressed
    ↓
Raised → Inset
```

## 11.1 Active 状态

所有主要交互元素：

``` text
Normal
    ↓
Raised

Pressed
    ↓
Inset
```

不要使用位移模拟按压：

``` text
禁止 translateY
禁止 translateX
```

按压效果应该主要来自阴影变化。

## 11.2 Hover

移动端没有传统鼠标 Hover。

因此：

-   不需要为了 Hover 强行添加复杂状态。
-   如果未来平板/桌面端需要 Hover，可以遵循"阴影减小"而不是"阴影增大"。

## 11.3 动画

过渡：

``` text
300ms
ease-in-out
```

Compose 可以使用：

``` text
animateDpAsState
animateColorAsState
updateTransition
```

但动画不得影响课表布局稳定性。

------------------------------------------------------------------------

# 12. 主题

必须提供：

``` text
AUTO
LIGHT
DARK
```

## 12.1 自动模式

自动模式：

``` text
08:00 - 22:00
    = Light

22:00 - 08:00
    = Dark
```

每分钟检查一次是否跨越时间节点。

不要每秒更新。

## 12.2 深色模式

深色模式不是简单：

``` text
#ffffff → #000000
```

必须重新映射：

-   背景
-   高光
-   暗阴影
-   文字
-   课程颜色

保持柔和。

------------------------------------------------------------------------

# 13. 课程颜色

课程颜色必须：

-   低饱和
-   与背景协调
-   保持足够文字对比度
-   不破坏新拟物空间感

禁止使用大量高饱和纯色卡片。

课程颜色只用于区分课程，不用于制造视觉噪音。

------------------------------------------------------------------------

# 14. UI 组件设计

推荐建立：

``` text
NeumorphicButton
NeumorphicCard
NeumorphicInput
NeumorphicTab
NeumorphicBadge
CourseCard
ScheduleGrid
DayTab
DayDrawer
ThemeSelector
```

所有这些组件必须尽量复用设计 Token。

不要出现：

``` text
CourseCard A 阴影一套
CourseCard B 阴影另一套
Button C 再写一套
```

应该统一由：

``` text
DesignTokens
```

控制。

------------------------------------------------------------------------

# 15. Design Tokens

必须集中定义：

``` text
backgroundLight
backgroundDark

lightShadowLight
lightShadowDark

textPrimaryLight
textPrimaryDark

accent

courseColor1
courseColor2
courseColor3
...
```

以及：

``` text
cornerSmall
cornerMedium
cornerLarge

shadowSmall
shadowMedium
shadowLarge

animationFast
animationNormal
```

不要在组件中散落大量硬编码颜色。

------------------------------------------------------------------------

# 16. 页面状态

所有页面必须明确处理：

``` text
Loading
Success
Empty
Error
Offline
```

登录页：

``` text
Idle
Loading
Success
Error
```

课表：

``` text
Loading
Cached
Refreshing
Success
Empty
Error
Offline
```

禁止只实现 Happy Path。

------------------------------------------------------------------------

# 17. 网络错误

用户可理解的错误：

``` text
网络不可用
登录失败
账号或密码错误
Session 已失效
课程获取失败
```

不要直接把：

``` text
HTTP 500
IOException
JSONException
SocketTimeoutException
```

显示给普通用户。

底层错误可以保留在开发日志，但 UI 必须转成友好提示。

------------------------------------------------------------------------

# 18. Session 生命周期

应用启动：

``` text
读取本地凭据
      ↓
检查本地 Session
      ↓
有效 → 进入课表
无效 → 登录页
```

请求课程：

``` text
请求
 ↓
成功
```

如果认证失败：

``` text
认证失败
 ↓
清理无效 Session
 ↓
提示重新登录
```

不要无限自动重试。

------------------------------------------------------------------------

# 19. Repository 规则

Repository 是唯一的数据入口。

例如：

``` kotlin
interface CourseRepository {
    suspend fun getCourses(): Result<List<Course>>
    suspend fun refreshCourses(): Result<List<Course>>
    suspend fun getCachedCourses(): List<Course>
}
```

ViewModel 不允许直接：

``` kotlin
OkHttp(...)
Retrofit(...)
HttpURLConnection(...)
```

所有网络访问必须经过：

``` text
Repository
```

------------------------------------------------------------------------

# 20. ViewModel 规则

ViewModel 负责：

-   UI 状态
-   用户动作
-   调用 UseCase / Repository
-   将 Domain 数据转换为 UI State

ViewModel 不负责：

-   AES 实现
-   Cookie 管理
-   JSON 解析
-   HTTP 请求细节
-   数据库存储细节

------------------------------------------------------------------------

# 21. 测试要求

至少覆盖：

## 21.1 Crypto

测试：

``` text
固定输入
→
AES
→
Base64
```

确保与已验证结果一致。

## 21.2 Mapper

测试：

``` text
Chaoxing DTO
→
Course
```

包括：

-   正常课程
-   缺少地点
-   跨节课程
-   不同星期
-   空数据

## 21.3 Schedule Layout

测试：

``` text
day
start
span
```

是否转换成正确的位置。

## 21.4 Repository

测试：

``` text
Remote Success
Remote Failure
Cache Hit
Cache Fallback
Session Expired
```

------------------------------------------------------------------------

# 22. Git 仓库结构

推荐：

``` text
XingClass/
├── android/
│   ├── app/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradle.properties
│
├── docs/
│   ├── ARCHITECTURE.md
│   ├── API.md
│   ├── SECURITY.md
│   ├── UI_GUIDE.md
│   ├── BUILD.md
│   └── DECISIONS.md
│
├── .github/
│   └── workflows/
│       ├── debug.yml
│       └── release.yml
│
├── AGENT_GUIDE.md
├── README.md
├── .gitignore
└── LICENSE
```

------------------------------------------------------------------------

# 23. GitHub Actions

## 23.1 Debug

普通 Push / Pull Request：

``` text
Checkout
 ↓
Setup JDK
 ↓
Gradle
 ↓
Lint
 ↓
Unit Test
 ↓
assembleDebug
 ↓
上传 Debug APK Artifact
```

Debug 构建不得发布 GitHub Release。

## 23.2 Release

Release Workflow：

``` text
workflow_dispatch
      ↓
Checkout
      ↓
Setup JDK
      ↓
Lint
      ↓
Test
      ↓
assembleRelease
      ↓
验证 APK
      ↓
创建 GitHub Release
      ↓
上传 APK
```

**Release 必须由人工手动触发。**

普通：

``` text
push main
```

不能创建 Release。

## 23.3 Release Tag

推荐人工触发时输入：

``` text
version
```

例如：

``` text
v1.0.0
```

Action 使用该版本生成 Release。

------------------------------------------------------------------------

# 24. APK 架构

Gradle 必须限制：

``` text
arm64-v8a
```

最终 Release APK：

``` text
XingClass-arm64-v8a-release.apk
```

不要输出：

``` text
armeabi-v7a
x86
x86_64
```

除非后续需求明确改变。

------------------------------------------------------------------------

# 25. Build 验收

AI Agent 完成代码后必须执行：

``` text
./gradlew lint
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
```

然后检查：

``` text
APK 是否存在
APK 是否为 Android APK
APK 是否只包含 arm64-v8a
```

如果环境无法联网导致 Gradle 无法下载依赖：

-   不要声称构建成功。
-   明确报告阻塞原因。
-   保留完整构建配置。
-   不要伪造 APK。

------------------------------------------------------------------------

# 26. README 最低要求

README 必须说明：

``` text
XingClass 是什么
如何构建
如何运行
如何登录
支持 Android 版本
APK 架构
如何运行测试
如何运行 GitHub Actions
如何手动创建 Release
安全注意事项
```

不得在 README 中出现真实账号密码。

------------------------------------------------------------------------

# 27. API 文档要求

`docs/API.md` 必须包含：

``` text
登录接口
鉴权状态接口
Cookie 接口
课程接口
```

每个接口至少记录：

``` text
Method
URL
Content-Type
参数
响应
认证要求
Session 要求
备注
```

对于尚未完全验证的行为必须标记：

``` text
UNVERIFIED
```

禁止把推测写成事实。

------------------------------------------------------------------------

# 28. 安全文档要求

`docs/SECURITY.md` 必须说明：

-   账号本地存储
-   Android Keystore
-   Cookie 管理
-   日志脱敏
-   GitHub Secrets
-   禁止提交凭据
-   Session 生命周期
-   用户退出登录后的清理行为

------------------------------------------------------------------------

# 29. AI Agent 工作流程

每次开发任务按照：

``` text
1. 阅读 AGENT_GUIDE.md
        ↓
2. 阅读相关 docs
        ↓
3. 检查现有代码
        ↓
4. 判断影响范围
        ↓
5. 实现最小变更
        ↓
6. 编写/修改测试
        ↓
7. 执行 lint
        ↓
8. 执行 test
        ↓
9. 执行 APK build
        ↓
10. 汇报结果
```

不要：

``` text
直接大规模重写
```

除非用户明确要求。

------------------------------------------------------------------------

# 30. AI Agent 输出规范

完成任务后必须报告：

``` text
## 修改内容

- ...
- ...

## 测试

- lint: PASS/FAIL
- unit test: PASS/FAIL
- debug build: PASS/FAIL
- release build: PASS/FAIL

## APK

路径：
...

架构：
arm64-v8a

## 已知问题

- ...
```

如果某一步未执行，必须明确写：

``` text
NOT RUN
```

禁止把"理论上应该成功"写成 PASS。

------------------------------------------------------------------------

# 31. 禁止事项总表

## 产品

-   不做二维码登录
-   不增加独立后端
-   不增加无需求功能
-   不增加复杂首页
-   不增加复杂个人中心

## UI

-   不让主课表滚动
-   不使用直角卡片
-   不使用普通扁平阴影
-   不使用粗边框
-   不使用纯黑/纯白作为新拟物主体背景
-   不让文字溢出
-   不让课程卡片破坏网格
-   不用位移模拟新拟物按压

## 网络

-   不明文提交未经加密的登录凭据
-   不丢失 Cookie Session
-   不在日志输出 Cookie
-   不无限重试
-   不假设未验证 API 行为

## 安全

-   不提交密码
-   不提交 Token
-   不提交 Cookie
-   不提交 Keystore
-   不提交真实测试凭据

## GitHub

-   Push 不自动发布 Release
-   Release 必须手动触发
-   Release APK 必须 ARM64
-   构建失败不能伪造成功

------------------------------------------------------------------------

# 32. 最终验收标准

只有同时满足以下条件，才可以认为 XingClass MVP 完成：

### 登录

-   [ ] 用户可以输入账号密码
-   [ ] AES 加密流程正确
-   [ ] 登录成功
-   [ ] Cookie/Session 正确保持
-   [ ] 登录失败有友好提示
-   [ ] 敏感数据不进入日志

### 数据

-   [ ] 可以获取课程
-   [ ] 课程正确映射
-   [ ] 本地缓存成功
-   [ ] 网络失败可以显示缓存
-   [ ] Session 失效可以重新登录

### UI

-   [ ] 一屏展示课表
-   [ ] 无主课表滚动
-   [ ] 10 节课均分
-   [ ] 课程支持跨节
-   [ ] 星期切换正常
-   [ ] 单日抽屉正常
-   [ ] 文本正确省略
-   [ ] Light 正常
-   [ ] Dark 正常
-   [ ] Auto 正常
-   [ ] 新拟物光影方向正确
-   [ ] 按压状态由外凸变内凹

### 工程

-   [ ] Kotlin
-   [ ] Jetpack Compose
-   [ ] Android 26+
-   [ ] arm64-v8a
-   [ ] Repository 分层
-   [ ] 单元测试
-   [ ] Lint
-   [ ] Debug APK
-   [ ] Release APK
-   [ ] GitHub Actions Debug
-   [ ] GitHub Actions 手动 Release

------------------------------------------------------------------------

# 33. 发生冲突时的优先级

如果不同资料之间发生冲突，按照：

``` text
1. 用户当前明确确认的架构决策
2. 本 AGENT_GUIDE.md
3. 工作指导前端
4. 工作指导后端
5. 新拟物视觉补充
6. AI Agent 自行推断
```

但不得擅自把"推断"写成"已经验证"。

------------------------------------------------------------------------

# 34. 当前明确的架构结论

最终架构固定为：

``` text
                    XingClass
                       │
              ┌────────┴────────┐
              │                 │
           UI Layer         Data Layer
              │                 │
       Jetpack Compose     Repository
              │             ┌───┴────┐
          ViewModel       Remote    Local
                              │        │
                              ▼        ▼
                           超星 API   本地缓存
                              │
                              ▼
                         Cookie/Session

Security Layer
      │
      ├── Android Keystore
      ├── Credential Storage
      └── AES Crypto
```

没有独立服务器。

没有二维码登录。

没有云端账号系统。

------------------------------------------------------------------------

# 35. 给 AI Agent 的最终一句话

**请把 XingClass 当成一个真实可维护的 Android 产品，而不是一次性
Demo。**

核心体验只有一个：

> **打开
> App，用户应该能快速、清晰、无需滚动地看到今天/本周所有课程，并且整个界面具有统一、柔和、克制的新拟物空间感。**

实现过程中优先保证：

``` text
正确性
>
可维护性
>
安全性
>
一屏课表体验
>
视觉一致性
>
额外功能
```

不要为了增加功能而破坏核心课表体验。
