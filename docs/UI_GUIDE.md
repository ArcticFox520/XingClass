# UI / 视觉实现规范

本文说明 XingClass 的视觉目标如何在 **Jetpack Compose 原生实现**中落地。代码是最终事实来源，
本文记录意图、Token 和不可破坏的规则。

## 1. 优先级

```text
信息架构 / 一屏可读性  >  新拟物空间感  >  动效
```

新拟物是**补充**，不允许为了“纯新拟物”破坏课表可读性。例如课程卡使用低饱和色底 + 高对比文字，
而不是和背景完全同色，否则 7 列小卡上的课程名无法辨认。

## 2. 光源与阴影

固定光源：**左上亮、右下暗**。任何组件都不得反向。

| 状态 | 表现 | 实现 |
|---|---|---|
| Raised | 暗阴影 +offset/+offset，亮阴影 −offset/−offset | `setShadowLayer` 两次绘制轮廓 |
| Inset | 内凹环：暗偏移进入左上内缘，亮偏移进入右下内缘 | 外轮廓 − 内缩轮廓 = 环，带 shadow layer 并裁剪 |
| Pressed | Raised 阴影淡出、Inset 淡入，**无 translate** | `pressProgress` 0→1，300ms |

统一入口：

- `Modifier.neumorphic(shape, style, backgroundColor, lightShadow, darkShadow, elevation, pressProgress, shadowScale)`
- `NeumorphicSurface`、`NeumorphicButton`、`NeumorphicIconButton`、`NeumorphicTab`、`NeumorphicBadge`
- 阴影档位：`NeumorphicElevation.SMALL / MEDIUM / LARGE`，全部来自 `DesignTokens.kt`

禁止：每个组件自定义阴影数字、Material 的 `shadow-*` 风格、位移模拟按压、粗边框、直角。

## 3. 颜色 Token

| Token | Light | Dark |
|---|---|---|
| background / surface | `#E0E5EC` | `#2B3038` |
| lightShadow | `#FFFFFF` | `#38404B` |
| darkShadow | `#B8BEC7` | `#1E2228` |
| textPrimary | `#39414F` | `#E3E8EF` |
| textSecondary | `#586172` | `#A3ADBB` |
| accent | `#5949D2` | `#9B90FF` |
| danger | `#9E3E3A` | `#E08580` |
| scrim | `#2A3040` @40% | `#0B0D10` @55% |

课程色 8 组（低饱和底色 + 高对比文字），浅/深色分别映射：

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

所有课程组合的正文对比度 ≥ 4.5:1（浅色最低 4.79:1，深色均 ≥ 6:1）。
课程颜色由课程标题哈希决定，同一门课在刷新后保持同一颜色。

## 4. 圆角与字号

| Token | 值 |
|---|---|
| cornerSmall | 12dp |
| cornerMedium | 16dp |
| cornerLarge | 20dp |
| cornerXLarge | 28dp（抽屉顶部） |
| pill | 50%（徽标/胶囊） |

字号：wordmark 26sp、页面标题 20sp、正文 14sp、说明 11sp、网格课程名 10sp（紧凑 9sp）、
网格地点 8sp、节数 12sp。**主课表禁止出现滚动**，所以网格字号必须小到能容纳 7×10。

## 5. 三层嵌套（硬性）

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

## 6. 主课表布局

- 根：`Column`，标题栏与星期栏高度固定，网格行 `weight(1f)` 占满剩余高度
- 网格：**自定义 `Layout`**，测量高度均分 10 行、宽度均分 7 列
- 课程位置完全由 `day/start/span` 计算（`ScheduleLayoutCalculator`，纯 Kotlin，已单测）
- 不使用 `LazyColumn` / `LazyRow` / `verticalScroll` 作为主课表容器
- 重叠课程自动分列（贪心区间着色），不会互相覆盖
- 卡片高度由跨节数决定，文字行数根据实际测量高度动态限制，长课程名不会撑破网格

## 7. 文本截断

- 课程卡标题：`maxLines = 动态(1..4)`，`overflow = Ellipsis`
- 课程卡地点：`maxLines = 1`，`Ellipsis`；跨节 ≥ 2 且卡片高度足够才显示
- 抽屉课程：标题、地点/教师均 `maxLines = 1` + `Ellipsis`，右侧内容 `weight(1f)`
- 抽屉左列固定 46dp；不使用会撑破容器的可伸缩列
- 任何文字都不贴边：卡片内边距 4dp/3dp，抽屉行 12dp/11dp

## 8. 按压与动效

- 所有可点击元素：Raised → Inset，300ms，ease-in-out
- 输入框聚焦：内凹**变浅**（`shadowScale 1.0 → 0.55`），表示“通道打开”
- 页面切换：淡入/滑入 300ms；抽屉从底部滑入并淡出遮罩
- 刷新图标：仅刷新中旋转，900ms/圈
- 加载态：三个呼吸圆点，不使用 Material 转圈
- 动画不影响布局：阴影/透明度动画不改变尺寸

## 9. 主题

- 三态：自动 / 白天 / 黑夜，持久化在设置中
- 自动：08:00–22:00 白天，其余黑夜；RootViewModel 每 60 秒检查一次，不按秒刷新
- 深色不是简单反色：背景、亮/暗阴影、文字、课程色、accent 全部重新映射
- 主题切换后系统状态栏图标同步（浅色主题用深色图标）

## 10. 页面状态

| 页面 | 状态 |
|---|---|
| 登录 | Idle / Loading / Success / Error |
| 课表 | Loading / Cached / Refreshing / Success / Empty / Error / Offline |
| 抽屉 | Open（选中 Tab 内凹）/ Closed（取消高亮）/ 空日 |
| 设置 | 同步时间 / 清除缓存反馈 / 退出确认 |

状态文案集中在 `strings.xml`，ViewModel 通过 `UiText`（资源 id）暴露，不持有 Context。

## 11. 可访问性

- 可点击元素均带 `Role.Button` / `Role.Tab` 与 content description（图标按钮）
- 文字对比度符合 WCAG AA（正文与课程名 ≥ 4.5:1）
- 点击区域：图标按钮 38–40dp，Tab 高 38dp，课程卡占满网格单元
- 不使用颜色作为唯一信息：今日列同时有圆点标记，离线同时有文字徽标

## 12. 禁止清单（对照实现检查）

- [x] 主课表无滚动容器
- [x] 无直角卡片
- [x] 无扁平单层阴影
- [x] 无粗边框（网格内只有极淡的凹槽分隔线）
- [x] 不以纯黑/纯白作为主体背景
- [x] 文字不溢出、不突然截断
- [x] 课程卡不破坏网格
- [x] 不用位移模拟按压
- [x] 阴影参数全部来自 DesignTokens
