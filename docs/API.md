# 超星接口

> 本文描述 XingClass 实际使用的超星接口、请求参数与响应字段。
>
> 文中用“已验证”表示字段或行为来自真实响应，用“未验证”表示按防御性逻辑处理、尚未捕获真实样本。
> 提交到测试中的真实结构样本已脱敏：字段名与类型保留，值替换为占位符。

---

## 1. 登录加密

| 项目 | 值 |
|---|---|
| 算法 | AES / CBC / PKCS#7 填充 |
| 密钥 | 默认 `u2oh6Vu^HWe4_AES`，可在设置页或登录页修改；固定 16 个字符，AES 取前 16 个 UTF-8 字节 |
| IV | 与密钥相同（前 16 字节） |
| 输出 | 标准 Base64（带 `=` 填充） |
| 实现 | `domain/model/ChaoxingKey.kt`（默认值与校验）、`security/ChaoxingCrypto.kt`（加密） |
| 测试 | `ChaoxingCryptoTest` 使用独立实现生成的向量比对；`ChaoxingKeyTest` 覆盖默认值与长度校验 |

说明：JCE 中写作 `AES/CBC/PKCS5Padding`；对 16 字节分组而言 PKCS#5 填充与 PKCS#7 等价，
向量测试已确认。表单提交时由 OkHttp `FormBody` 做 URL 编码（`+` → `%2B`，`=` → `%3D`）。
登录成功响应验证了该加密方式可用。

默认值集中在 `ChaoxingKey.DEFAULT`。设置页与登录页弹窗保存的密钥由 `SettingsRepository` 持久化，
`ChaoxingApiClient` 在每次登录时读取当前值；修改后**下一次登录**生效，不需要重启应用。
输入必须是 trim 后**恰好 16 个字符**（默认值即 16 字符）；密钥是公开常量而非用户凭据，
因此不使用 Keystore 加密存储。

---

## 2. 登录

### `POST https://passport2.chaoxing.com/fanyalogin`

| 项目 | 说明 |
|---|---|
| Method | POST |
| URL | `https://passport2.chaoxing.com/fanyalogin` |
| 请求 Content-Type | `application/x-www-form-urlencoded; charset=UTF-8` |
| 响应 Content-Type | 真实返回为 `text/html;charset=utf-8`，但 body 是 JSON（不要按 Content-Type 拒绝） |
| 认证要求 | 无 |
| Session 要求 | 无；响应 `Set-Cookie` 必须保存 |
| 状态 | 成功路径已验证；失败分支（错误密码、验证码、双因子）未验证 |

请求参数：

| 参数 | 值 | 说明 |
|---|---|---|
| `fid` | `-1` | 学校 ID；`-1` 表示按账号自动识别 |
| `uname` | AES(Base64) 后的账号 | 必须先加密 |
| `password` | AES(Base64) 后的密码 | 必须先加密 |
| `refer` | `https://www.chaoxing.com/` | 已验证可用；另有 `http%3A%2F%2Fi.mooc.chaoxing.com` 形态，未对比 |
| `t` | `true` | 登录标识 |
| `forbidotherlogin` | `0` | 不禁止其他设备登录 |
| `validate` | 空串 | 验证码标识 |
| `doubleFactorLogin` | `0` | 双因子登录标识 |
| `independentId` | `0` | 独立 ID 标识 |
| `independentNameId` | `0` | 独立名称 ID 标识 |

真实成功响应（HTTP 200）：

```json
{ "status": true, "url": "https://www.chaoxing.com/" }
```

响应行为：

- 响应 body 长度约 49 字节，`status` 为布尔 `true`。
- 服务端下发 **12 个 Cookie**，域包括 `.chaoxing.com` 与 `passport2.chaoxing.com`。
  后续对 `kb.chaoxing.com` 的课程请求复用同一 CookieJar 成功。
- 登录失败响应未捕获；代码按 `status == false` + `msg2/msg/message` 分类为
  `InvalidCredentials` / `LoginFailed`，该分支未验证。

实现行为：

- `status == true` → 成功；随后由共享 CookieJar 保存所有 `Set-Cookie`。
- `status == false` → 读取 `msg2` / `msg` / `message` **仅用于分类**，不展示原文；
  包含“密码/账号/账户” → `InvalidCredentials`，否则 `LoginFailed`。
- 非 2xx → `Server`；`IOException` → `Network`。

---

## 3. 鉴权状态

### `GET https://passport2.chaoxing.com/getauthstatus/v2`

| 项目 | 说明 |
|---|---|
| Method | GET |
| 参数 | 无（资料如此描述） |
| 认证要求 | 依赖 Cookie |
| 状态 | 已验证：登录态下仍返回“参数为空”，不能用于正向确认会话 |

真实响应（登录成功、Cookie 会话有效时）：

```json
{ "mes": "参数为空", "type": "1", "status": false }
```

这一点非常重要：

- 即使会话完全有效，该接口在无参数调用时也会返回 `status: false` + “参数为空”。
- 因此 **不能** 把 `status != true` 当作“会话失效”并清 Cookie，否则用户每次启动都会被登出。
- 这是 `AuthRepository` / `RootViewModel` 采用 `VALID / INVALID / UNKNOWN` 三态，
  并把“参数为空”归为 `UNKNOWN` 的原因。

实现判定：

| 判定 | 条件 |
|---|---|
| `VALID` | `status == true` |
| `INVALID` | HTTP 401/403；响应是登录页 HTML；`status == false` 且文案含“未登录/失效/过期/没有权限” |
| `UNKNOWN` | 其他一切（含“参数为空”、无法解析、网络失败） |

`UNKNOWN` 时进入课表并依赖课程请求做最终判定。会话过期的真实响应未捕获，
代码对 401/403、登录页 HTML、失效文案的防御性处理未验证，但不会误登出。

---

## 4. 读取 Cookie（未实现）

### `POST https://www.chaoxing.com/entry/cookie/get`

资料记录该接口用于读取指定 Cookie，返回 `{code, data:{...}, status:200}`。
**MVP 不需要它**：Cookie 由本地 CookieJar 管理，因此没有实现，也不在 UI 暴露。

---

## 5. 课程

### `GET https://kb.chaoxing.com/pc/curriculum/getMyLessons`

| 项目 | 说明 |
|---|---|
| Method | GET |
| 参数 | `curTime`（毫秒时间戳，缓存时间戳；**不控制周次**）、`week`（1-based 教学周，控制返回哪一周） |
| 认证要求 | 依赖 Cookie/Session |
| 响应 Content-Type | `application/json` |
| 状态 | 结构已验证；过期会话分支未验证 |

真实响应结构（值已替换为占位符，字段名/类型来自真实响应）：

```json
{
  "data": {
    "schools": [],
    "sysTime": 1793946884693,
    "curriculum": {
      "currentWeek": 10,
      "realCurrentWeek": 10,
      "maxWeek": 22,
      "schoolYear": "2026",
      "semester": 1,
      "sectionTime": 45,
      "lessonLength": 170,
      "morningTime": "08:30",
      "afternoonTime": "14:00",
      "eveningTime": "18:30",
      "lessonTimeConfigArray": ["08:30-09:15", "09:20-10:05", "10:25-11:10"]
    },
    "config": { "conflictStyle": 1 },
    "lessonArray": [
      {
        "lessonId": "唯一的上课实例 ID",
        "courseId": 0,
        "name": "课程名称",
        "teacherName": "教师姓名",
        "location": "上课地点",
        "dayOfWeek": 5,
        "beginNumber": 5,
        "length": 2,
        "weeks": "10",
        "day": "11-06",
        "wholeDay": "2026-11-06",
        "className": "班级",
        "courseNo": "课程号",
        "classNo": "教学班号",
        "extend": "{\"campus\":\"校区\",\"fbzt\":0}",
        "conflictUuids": []
      }
    ]
  },
  "msg": null,
  "result": 1
}
```

### 5.1 关键结构说明

- 顶层是 `{data, msg, result}`，**没有 `status` 字段**；`result` 为整数 `1`，`msg` 为 `null`。
  不要用顶层 `status` 判断成功。
- 课程列表位于 `data.lessonArray`，不是 `data.data` / `data.list`。
- `data.schools` 为 `[]`，**不能**把它当作“本周没有课”的空课表；真正的课表是 `lessonArray`。
- `data.curriculum` 携带学期/周次元数据；`lessonTimeConfigArray` 是 **节次时间字符串数组**，
  不是课程数组，长度 11（含 08:30-09:15 等）。App 用前 10 个解析为 `PeriodTimes`，
  驱动左侧节数栏的上/下课时间与抽屉的起止时间。
- 真实响应样本包含 **14 条** `lessonArray`：`dayOfWeek` 为 1–5（周一至周五），
  `beginNumber` 为 1/3/5/7/9，`length` 为 2，`weeks` 为 "10"，日期覆盖 2026-11-02 至 2026-11-06。
  这些条目可全部映射进 7×10 网格；`CourseMerger` 仅在内容相同且时间连续时合并。
- 接口默认返回**真实本周**的课程实例；指定 `week` 时返回该周的实例（详见 5.2）。
  它返回的是单周课表，不是整学期列表。
- `currentWeek` / `realCurrentWeek` 始终表示**真实本周**，不会因为请求了第 9 周而变成 9。

### 5.2 周次切换

关键结论：**`week` 才是周次参数，`curTime` 不改变返回的周次。**

探测结果（同一个有效会话）：

| 请求 | `lessonArray` 数量 | `wholeDay` 范围 | 课程 `weeks` |
|---|---|---|---|
| 不带 `week` | 14 | 2026-11-02..11-06 | `"10"` |
| `week=9` | 16 | 2026-10-26..11-01 | `"9"` |
| `week=11` | 14 | 2026-11-09..11-14 | `"11"` |
| `week=1` / `2` / `22` | 0（该周无课） | — | — |
| `week=0` / `23` / `999` | 0（HTTP 200，不报错） | — | — |
| `week=abc` | 404 | — | — |

- 无论 `week` 传什么，响应里的 `currentWeek` 与 `realCurrentWeek` **始终是真实本周**（示例数据为 10）。
  客户端必须自己记录“当前查看第几周”，不能拿响应的 `currentWeek` 当选中周。
- 每条 lesson 的 `weeks` 字段与请求的 `week` 一致（示例为 `"9"`/`"10"`/`"11"`），
  这是验证周次切换是否生效的直接依据。
- 合法范围是 `1..maxWeek`。越界不报错而是返回空数组；客户端仍应限制在合法范围内。
- `week=abc` 返回 404；客户端只发送数字。
- App 的 Repository/ViewModel 用 `week` 参数切换，并以“选中周 / 真实本周 / 最大周”三个值维护状态；
  本地缓存也保存这三个值，离线时至少能继续展示最近一次同步的周。

### 5.3 已验证字段映射

| 内部字段 | 真实来源 | 说明 |
|---|---|---|
| 标题 | `name` | 必填；缺失则丢弃该条 |
| 教师 | `teacherName` | 可为空 |
| 地点 | `location` | 可为空 |
| 实例 ID | `lessonId` | 唯一；`courseId` 是课程级 ID，同一门课一周内重复，**不能用于去重** |
| 星期 | `dayOfWeek` | **1–7，1=周一**；转换为内部 0–6 |
| 开始节 | `beginNumber` | 1–10 |
| 跨节数 | `length` | 真实样本中为 2 |
| 周次 | `weeks` | 字符串；与请求的 `week` 一致，用于验证周次切换，不参与网格定位 |
| 日期 | `day` / `wholeDay` | `11-06` / `2026-11-06`；不参与网格定位 |
| 校区 | `extend` | JSON 字符串；MVP 不使用 |

### 5.4 容错解析策略

`ChaoxingLessonParser` 以真实结构为准，同时保留对旧/变体结构的容错：

1. 容器查找顺序：`lessonArray` → `lessons` → `lessonList` → `data` → `list` → `rows` →
   `result` → `records` → `courseList`；也接受 `data` 是 JSON 字符串的情况。
2. 只有在上述键都找不到时，才扫描对象中的数组作为兜底，并且要求数组**非空**且“像课程”。
   这样 `data.schools: []` 不会被误判为空课表。
3. 字段别名仍保留（例如标题也接受 `title`/`courseName`），以应对未来结构变化。
4. 无法确定 `day`（0–6）或 `start`（1–10）的条目直接丢弃，不猜测、不挪到别的格子。
5. `span` 超过第 10 节时裁剪到 `11 - start`。
6. 空 `lessonArray` 是合法的空课表，映射为 0 门课并进入 Empty 状态，不是错误。

### 5.5 内部 Course ID

`Course.id` 不是裸 `lessonId`，而是：

```text
<lessonId 或 cx>@<day>-<start>-<span>-<titleHash>
```

原因：真实数据的 `courseId` 会在同一门课的多次上课中重复；即使解析到课程级 ID，
不同网格位置的实例也必须拥有不同的 `id`（Compose key 与缓存都需要唯一）。
完整相同的条目（同 ID、同位置、同标题）仍会在 Mapper 去重。

### 5.6 会话失效判定

- HTTP 401/403 → `SessionExpired`
- 响应是登录页 HTML → `SessionExpired`
- 顶层 `msg/message` 含失效文案 → `SessionExpired`
- 无课程容器且存在失效文案 → `SessionExpired`，否则 `CourseFetch`
- 空 `lessonArray` → 合法的“没有课程”

过期会话的真实响应未捕获，因此上述分支的**具体服务端表现未验证**；
但错误不会破坏已有缓存，且会清理本地会话并回到登录页。

---

## 6. 未实现但已捕获的接口

以下接口在资料中出现，但**不属于第一版范围**，未实现：

- `GET https://kb.chaoxing.com/pc/curriculum/getCurriculumHistory`
- `GET https://kb.chaoxing.com/pc/school/getOpeningStatus`
- `GET https://kb.chaoxing.com/pc/curriculum/getLocationHistoryList`

不要因为接口被发现就自动加入产品。

---

## 7. 验证方式

`LiveChaoxingApiTest` 使用真实的 `ChaoxingApiClient` + `ChaoxingCookieJar` +
`ChaoxingLessonParser` + `CourseMapper` 执行，断言：

- 登录成功；
- CookieJar 中产生了会话 Cookie；
- 当前周课程请求成功且非空；
- **解析出的每一条课程都能映射进 7×10 网格**；
- **切换到上一周后返回的课程 `weeks` 与目标周一致**，且 `currentWeek` 仍为真实本周。

运行（凭据只放在环境变量中，不写入任何文件）：

```bash
export XINGCLASS_TEST_ACCOUNT=你的账号
export XINGCLASS_TEST_PASSWORD=你的密码
cd android
./gradlew testDebugUnitTest --tests '*LiveChaoxingApiTest' --no-daemon
```

该测试默认跳过（`Assume.assumeTrue`），不会因为缺少凭据失败，也不会在仓库或日志中
留下账号密码。提交到仓库的真实结构样本已脱敏。

---

## 8. 安全与日志

- 登录表单包含加密后的账号密码；Cookie 是会话凭证。两者都不进入日志、异常信息或缓存文件。
- `Logger` 只接受固定事件名（`LOGIN_REQUEST_STARTED`、`COURSE_FETCH_SUCCESS` 等），
  异常只记录**类名**。
- 所有接口仅通过 HTTPS；`usesCleartextTraffic=false`。

## 9. 当真实响应再次变化时

1. 不要改 UI 去适配猜测，先用一次真实请求确认结构（不要把响应体提交到仓库）；
2. 更新 `ChaoxingLessonParser` 的容器键/字段别名，并补充脱敏结构测试；
3. 如果再次出现“空数组抢占课程列表”之类的结构，优先在 Parser 中收紧条件，而不是在 Mapper 中兜底；
4. 更新本文档的验证状态；
5. 不要在源码、文档、Issue 或日志中留下账号、密码、Cookie 或真实课程数据。
