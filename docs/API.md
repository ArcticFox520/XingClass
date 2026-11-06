# 超星接口与实现记录

> 本文只描述 XingClass 实际使用的接口。所有结论来自项目提供的《工作指导后端》抓包资料，
> 未额外使用任何真实账号进行线上验证。**未经验证的行为一律标记 `UNVERIFIED`，不把推测写成事实。**

所有请求都经过同一个 `OkHttpClient` / `ChaoxingCookieJar`，以保持 Session-Cookie 上下文。
请求头统一携带浏览器 UA（超星 Web 接口未提供官方原生 API），不启用任何代理或自建服务端。

---

## 1. 登录加密（VERIFIED）

| 项目 | 值 |
|---|---|
| 算法 | AES / CBC / PKCS#7 填充 |
| 密钥 | `u2oh6Vu^HWe4_AES`（超星前端公开混淆串，取前 16 字节） |
| IV | 与密钥相同（前 16 字节） |
| 输出 | 标准 Base64（带 `=` 填充） |
| 实现 | `security/ChaoxingCrypto.kt` |
| 测试 | `ChaoxingCryptoTest` 使用独立 Python 实现生成的向量比对 |

说明：JCE 中写作 `AES/CBC/PKCS5Padding`；对 16 字节分组而言 PKCS#5 填充与 PKCS#7 等价，
向量测试已确认。表单提交时由 OkHttp `FormBody` 做 URL 编码（`+` → `%2B`，`=` → `%3D`）。

---

## 2. 登录

### `POST https://passport2.chaoxing.com/fanyalogin`

| 项目 | 说明 |
|---|---|
| Method | POST |
| URL | `https://passport2.chaoxing.com/fanyalogin` |
| Content-Type | `application/x-www-form-urlencoded; charset=UTF-8` |
| 认证要求 | 无（登录入口） |
| Session 要求 | 无；响应 `Set-Cookie` 必须保存 |
| 状态 | 请求字段与成功响应来自抓包（VERIFIED）；服务端其余分支 `UNVERIFIED` |

请求参数：

| 参数 | 值 | 说明 |
|---|---|---|
| `fid` | `-1` | 学校 ID；`-1` 表示按账号自动识别 |
| `uname` | AES(Base64) 后的账号 | 必须先加密 |
| `password` | AES(Base64) 后的密码 | 必须先加密 |
| `refer` | `https://www.chaoxing.com/` | 跳转地址；抓包中另有 `http%3A%2F%2Fi.mooc.chaoxing.com` 的形态，`UNVERIFIED` 哪一个更通用 |
| `t` | `true` | 登录标识 |
| `forbidotherlogin` | `0` | 不禁止其他设备登录 |
| `validate` | 空串 | 验证码标识 |
| `doubleFactorLogin` | `0` | 双因子登录标识 |
| `independentId` | `0` | 独立 ID 标识 |
| `independentNameId` | `0` | 独立名称 ID 标识 |

成功响应（资料记录）：

```json
{ "url": "https://www.chaoxing.com/", "status": true }
```

失败响应（资料记录）：

```json
{ "msg2": "参数为空", "status": false }
```

实现行为：

- `status == true` → 成功；随后由共享 CookieJar 保存所有 `Set-Cookie`。
- `status == false` → 读取 `msg2` / `msg` / `message` **仅用于分类**，不展示原文；
  包含“密码/账号/账户” → `InvalidCredentials`，否则 `LoginFailed`。
- 非 2xx → `Server`；`IOException` → `Network`。
- 失败响应分支、验证码、双因子流程 `UNVERIFIED`，当前最小实现不支持验证码交互。

---

## 3. 鉴权状态

### `GET https://passport2.chaoxing.com/getauthstatus/v2`

| 项目 | 说明 |
|---|---|
| Method | GET |
| 参数 | 无（资料如此描述） |
| 认证要求 | 依赖 Cookie |
| 状态 | 只有失败样例被捕获，成功响应 `UNVERIFIED` |

资料中记录的失败响应：

```json
{ "mes": "参数为空", "type": "1", "status": false }
```

实现采用三态解释，而不是“非 true 即失效”：

| 判定 | 条件 |
|---|---|
| `VALID` | `status == true` |
| `INVALID` | HTTP 401/403；响应是登录页 HTML；`status == false` 且文案含“未登录/失效/过期/没有权限” |
| `UNKNOWN` | 其他一切（含“参数为空”、无法解析、网络失败） |

`UNKNOWN` 时 App 进入课表并依赖课程请求判定会话，避免误登出；网络失败也归为 `UNKNOWN`。
`INVALID` 会立即清除本地 Cookie。

---

## 4. 读取 Cookie（未实现）

### `POST https://www.chaoxing.com/entry/cookie/get`

资料记录该接口用于读取指定 Cookie（`name=UID,current_page_id,...`），返回
`{code, data:{...}, status:200}`。**MVP 不需要它**：Cookie 由本地 CookieJar 管理，
因此没有实现，也不在 UI 暴露。保留此记录仅为文档完整性。

---

## 5. 课程

### `GET https://kb.chaoxing.com/pc/curriculum/getMyLessons`

| 项目 | 说明 |
|---|---|
| Method | GET |
| 参数 | `curTime`（毫秒时间戳） |
| 认证要求 | 依赖 Cookie/Session |
| 状态 | 参数与部分字段来自抓包（VERIFIED）；**完整响应结构 `UNVERIFIED`** |

资料给出的部分字段示例：

```json
{
  "status": 200,
  "data": [
    {
      "id": "课程ID",
      "name": "课程名称",
      "teacher": "教师姓名",
      "time": "上课时间",
      "location": "上课地点",
      "status": "课程状态"
    }
  ]
}
```

### 5.1 容错解析策略

由于完整结构未验证，`ChaoxingLessonParser` 做**受控容错**，绝不臆造数据：

1. 在 JSON 树中寻找课程数组：
   - 顶层是数组 → 直接使用；
   - 对象中按 `lessons` → `lessonList` → `data` → `list` → `rows` → `result` → `records` → `courseList` 查找；
   - `data` 是**字符串**时再次解析为 JSON；
   - 都没有时，才在对象中寻找“看起来像课程”的数组（首元素含标题字段）。
2. 字段别名（仅列出已实现的）：

| 内部字段 | 接受的来源字段 |
|---|---|
| 标题 | `name`, `title`, `courseName`, `lessonName`, `curriculumName`, `courseTitle` |
| 教师 | `teacher`, `teacherName`, `teacherNames`, `teachers`, `teacher_name`；数组时用“、”连接 |
| 地点 | `location`, `place`, `classroom`, `room`, `address`, `addr`, `classroomName`；对象时取 `name/address/place/location` |
| ID | `id`, `courseId`, `lessonId`, `curriculumId`, `lesson_id`, `curriculum_id` |
| 星期（1 基） | `weekDay`, `weekday`, `dayOfWeek`, `week_day`, `xq`, `weekdayNum`, `week` |
| 星期（0 基） | `day`, `dayIndex`, `dayOfWeekIndex`, `weekIndex` |
| 开始节 | `start`, `startSection`, `sectionStart`, `nodeStart`, `beginSection`, `startNode`, `startSectionNum`, `sectionStartNum` |
| 结束节 | `end`, `endSection`, `sectionEnd`, `nodeEnd`, `endNode`, `endSectionNum` |
| 跨节数 | `span`, `sectionSpan`, `nodeNum`, `sectionCount`, `step`, `length`, `duration`, `periods` |
| 时间文本 | `time`, `timeText`, `sectionText`, `courseTime`, `lessonTime`, `classTime`, `timeStr` |
| 节次数组 | `sections: [3,4]` → start=3, end=4, span=2 |
| 嵌套节次对象 | `section`/`node`/`period`/`sectionInfo` 等对象中的 start/end/span |

3. 时间文本兜底（如 `周一第1-2节`）：
   - 星期：`星期/周/礼拜` + `一…日/天/1-7`；
   - 节次：必须显式出现“节”，避免把 `08:00-09:40` 误判成节次。
4. 无法确定 day（0–6）或 start（1–10）的条目**直接丢弃**，不猜测、不挪到别的格子。
5. `span` 超过第 10 节时裁剪到 `11 - start`。

### 5.2 会话失效判定

- HTTP 401/403 → `SessionExpired`
- 响应以 `<` 开头或包含 `<html` → 被重定向到登录页 → `SessionExpired`
- 顶层 `status != 200` 且 `msg/message` 含失效文案 → `SessionExpired`
- 无课程容器且存在失效文案 → `SessionExpired`，否则 `CourseFetch`
- 空数组 → 合法的“没有课程”，进入 Empty 状态

---

## 6. 未实现但已捕获的接口

以下接口在资料中出现，但**不属于第一版范围**，未实现：

- `GET https://kb.chaoxing.com/pc/curriculum/getCurriculumHistory`
- `GET https://kb.chaoxing.com/pc/school/getOpeningStatus`
- `GET https://kb.chaoxing.com/pc/curriculum/getLocationHistoryList`

不要因为接口被发现就自动加入产品。

---

## 7. 安全与日志

- 登录表单包含加密后的账号密码；Cookie 是会话凭证。两者都不进入日志、异常信息或缓存文件。
- `Logger` 只接受固定事件名（`LOGIN_REQUEST_STARTED`、`COURSE_FETCH_SUCCESS` 等），
  异常只记录**类名**。
- 所有接口仅通过 HTTPS；`usesCleartextTraffic=false`。

## 8. 当真实响应与本文不一致时

1. 不要改 UI 去适配猜测，先抓取真实 JSON；
2. 在 Debug 构建下确认失败点（事件名，不打印敏感体）；
3. 更新 `ChaoxingLessonParser` 的别名或容器键，并补充单元测试；
4. 更新本文档，把 `UNVERIFIED` 改为已验证结论，并注明日期；
5. 不要在源码、文档、Issue 或日志中留下账号、密码、Cookie。
