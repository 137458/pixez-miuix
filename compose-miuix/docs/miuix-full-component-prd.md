# PRD：全面接入 MIUIX / HyperOS 原生组件体系与视觉架构

> 父文档：本文件作为 PixEz MIUIX 版本全面深化 HyperOS 设计语言、全量采用 MIUIX 官方组件的架构指导与需求规格说明书。

## 一、 背景与问题陈述 (Problem Statement)

PixEz 在向 Compose Multiplatform 移植的过程中，已基本完成了由 Material 3 基础框架向 Xiaomi HyperOS (`top.yukonga.miuix.kmp`) 的初步过渡。但在近期的深度代码审查与用户体验走查中，发现当前工程仍存在以下深层架构与组件接入断层：

1. **偏好设置层拼凑冗余**：虽然主流二级页面已初步替换为 Preference，但部分页面（如自适应列宽滑块、多选批量项等）仍使用手写 `BoxWithConstraints + Column + Slider` 或手写 `CheckIndicator`，尚未全量使用 `SliderPreference`、`RangeSliderPreference` 与 `CheckboxPreference`。
2. **弹窗与浮层体系权责不清**：尚未严格区分依附于页面生命周期的 `Overlay*` 浮层与独立于页面的 `Window*` 全局浮层；TopAppBar 操作按钮与列表快捷操作大量回退到底部抽屉（BottomSheet），缺乏轻量级锚点弹出菜单（`OverlayIconDropdownMenu`、`OverlayListPopup`、`OverlayCascadingListPopup`）。
3. **大屏与桌面端微交互缺失**：
   - 桌面端鼠标悬停（Hover）或移动端长按关键按钮（收藏、保存、原图、下载、分享）没有任何提示气泡（`TooltipBox`）。
   - 除设置主页外，主流长列表与瀑布流（首页推荐、排行榜、搜索结果、浏览历史、评论区）均缺乏贴边滚动条（`VerticalScrollBar`）。
   - 状态徽标（版本更新 `"NEW"` 标签、插画卡片 `"AI"` 标识、多图 `"P2"` 标识）多为手写绘制，未接入原生 `Badge` 与 `BadgedBox`。
   - 数值与色彩拾取仍为基础表单，未接入 MIUIX 标志性的滚轮选择器（`NumberPicker`）与调色板（`ColorPicker` / `ColorPalette`）。
4. **形状与动效质感未达极致**：目前大部分卡片容器与图片缩略图仍采用固定半径的标准圆角（`RoundedCornerShape`），未发挥 HyperOS 独有的连续曲率超椭圆（`SquircleShape`）平滑质感，且透明顶栏/底栏缺乏渐进式景深毛玻璃（`ProgressiveBlur`）。

---

## 二、 总体方案与架构设计 (Solution & Architecture)

全面贯彻 Xiaomi HyperOS 设计规范，以 `top.yukonga.miuix.kmp` 为基石，将应用 UI 交互体系解构为四大支柱：

```
                    ┌─────────────────────────────────────────────────────────┐
                    │               PixEz MIUIX 组件与视觉架构                │
                    └────────────────────────────┬────────────────────────────┘
                                                 │
         ┌───────────────────────────┬───────────┴───────────┬───────────────────────────┐
         ▼                           ▼                       ▼                           ▼
┌──────────────────┐       ┌──────────────────┐    ┌──────────────────┐        ┌──────────────────┐
│  miuix-preference│       │  Overlay/Window  │    │     miuix-ui     │        │  squircle & blur │
│   偏好设置体系   │       │   弹窗与浮层体系 │    │   基础交互与输入 │        │   形状与动效体系 │
├──────────────────┤       ├──────────────────┤    ├──────────────────┤        ├──────────────────┤
│• SwitchPreference│       │• OverlayDialog   │    │• SearchBar+Input │        │• squircleSurface │
│• ArrowPreference │       │• WindowDialog    │    │• TabRow 胶囊滑块 │        │• squircleClip    │
│• RadioButtonPref │       │• OverlayBottomSht│    │• PullToRefresh   │        │• squircleBorder  │
│• CheckboxPref    │       │• OverlayListPopup│    │• TooltipBox      │        │• ProgressiveBlur │
│• DropdownPref    │       │• IconDropdownMenu│    │• Badge/BadgedBox │        │• 景深毛玻璃背景  │
│• SliderPreference│       │• CascadingPopup  │    │• VerticalScrollBar│        │                  │
└──────────────────┘       └──────────────────┘    └──────────────────┘        └──────────────────┘
```

---

## 三、 详细需求与组件场景映射 (Functional Requirements)

### 1. 偏好设置体系深化 (`miuix-preference`)
- **滑块与区间偏好**：
  - 在 `CrossAdapterSettingScreen` 与 `LayoutSettingScreen` 中，使用 `SliderPreference` 替代手写的 `AdapterWidthSlider`，直接接入原生吸附点（Magnetic snap）、触觉振动反馈与实时阈值数值显示。
- **多选偏好规范**：
  - 在批量屏蔽分类、高级下载命名规则等场景，引入 `CheckboxPreference` 取代传统单行 BasicComponent。
- **富选项微调**：
  - 探索引入 `OverlaySpinnerPreference`，在画质选择或网络代理切换等重点设置中展示带状态图标和详细描述的丰富下拉菜单。

### 2. 弹窗与浮层体系分层 (`Overlay*` vs `Window*`)
- **宿主边界划分**：
  - 页面内部确认操作（如清理下载历史确认、修改命名格式、画质切换）严格使用 `OverlayDialog`，必须依赖 `Scaffold` 宿主，跟随页面生命周期自动出栈销毁。
  - 全局拦截型操作（如应用内强制版本更新升级弹窗、跨页面未登录提示、全局权限申请）采用 `WindowDialog`，独立于页面层级，防止路由切换导致弹窗意外丢失。
- **锚点气泡菜单**：
  - 在插画详情页右上角、作品分享入口、搜索历史记录右侧，使用 `OverlayIconDropdownMenu` 与 `OverlayListPopup`，替代笨重的全局底部抽屉（BottomSheet），实现即点即出的小米风格悬浮气泡菜单。
  - 在作品多级标签筛选与画师分组中，接入 `OverlayCascadingListPopup` 支持多级子菜单展开。

### 3. 核心基础交互与输入组件 (`miuix-ui`)
- **悬浮提示气泡 (`TooltipBox + PlainTooltip`)**：
  - 针对桌面端（Desktop）及平板大屏，为所有无文字标注的 `IconButton`（顶栏返回、搜索过滤、详情页收藏、原图查看、本地下载、作品分享、评论按钮）封装 `TooltipBox`，支持鼠标悬停秒级气泡提示。
- **原生角标系统 (`Badge + BadgedBox`)**：
  - 将 `SettingsScreen` 更新提示 `"NEW"` 标签由手写 `Box` 替换为官方 `Badge`。
  - 在插画卡片右下角/左上角使用 `Badge` 承载 `"AI"` 生成标识与多图页码 `"P{N}"`。
  - 为底部导航栏（如动态 Tab、下载任务入口）接入 `BadgedBox`，实时展示新动态红点或正在进行的任务数。
- **全域长列表滚动条 (`VerticalScrollBar`)**：
  - 为推荐流瀑布流（`HelloScreen`）、排行榜网格（`RankingScreen`）、搜索结果（`SearchScreen`）、历史记录（`HistoryScreen`）与评论区（`CommentsScreen`）全量接入贴边半透明 `VerticalScrollBar`。
  - 正确联动 `contentPadding`，杜绝遮挡瀑布流内容与底栏悬浮区域。
- **原生色彩与数字拾取 (`ColorPicker` / `NumberPicker`)**：
  - 主题设置页（`ThemeSettingScreen`）种子色自定义完全接入官方 `ColorPicker` / `ColorPalette`，提供色环与饱和度精准调控。
  - 下载任务并发数（1~10）及定时清理天数采用 MIUI 经典滚轮 `NumberPicker`。
- **面包屑与悬浮操作栏 (`BreadcrumbBar` / `FloatingToolbar`)**：
  - 下载路径选择与层级查看接入 `BreadcrumbBar`，支持各级路径直接点击跳转。
  - 漫画阅读器或全屏画集预览中，在 `Scaffold.floatingToolbar` 槽位嵌入 MIUI 原生悬浮胶囊工具栏。

### 4. 形状与视觉动效体系 (`miuix-squircle` & `miuix-blur`)
- **超椭圆平滑圆角 (`SquircleShape`)**：
  - 引入 `Modifier.squircleSurface`、`Modifier.squircleClip`、`Modifier.squircleBorder`，替换全局卡片容器与插画缩略图的 `RoundedCornerShape`，在支持的设备上实现无生硬折角的连续曲率超椭圆视觉。
- **渐进式景深模糊 (`ProgressiveBlur`)**：
  - 引入 `miuix-blur`，在悬浮底栏（Floating Bottom Bar）、沉浸式透明顶栏下方接入 `Modifier.progressiveTextureBlur`，实现从边缘到底部的自然平滑过渡模糊。

---

## 四、 架构与工程规范约束

1. **组件排他性原则**：严格使用 Xiaomi HyperOS / MIUIX (`top.yukonga.miuix.kmp.*`) 官方组件，严禁泄漏引入 Material 3 控件。
2. **多语言 100% 覆盖**：所有新增组件的 UI 文案、提示气泡内容、无障碍描述（contentDescription）统一接入 `LocalStrings.current`（`AppStrings.kt`）。
3. **常量中心化**：所有数值步长、阈值极值、预设档位与占位模板一律统一收敛至 `AppConstants.kt`。
4. **多端兼容与优雅降级**：
   - Android（手机/折叠屏/平板）与 Desktop（Windows/macOS/Linux）双端保持无缝兼容。
   - Shader / Squircle 特效在低版本平台自动回退到常规圆角与半透明背景，确保无运行时崩溃。

---

## 五、 实施路线图与里程碑

- **Milestone 1: 偏好设置深化与全域滚动条**（Issue #1、#3）
- **Milestone 2: 桌面微交互气泡与原生角标**（Issue #4）
- **Milestone 3: 弹窗浮层分层与锚点下拉气泡**（Issue #2）
- **Milestone 4: 原生色彩拾取、滚轮选择与面包屑**（Issue #5、#6）
- **Milestone 5: 超椭圆 Squircle 与渐进式毛玻璃质感升级**（Issue #7）
