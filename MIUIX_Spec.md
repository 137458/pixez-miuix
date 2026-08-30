# Xiaomi HyperOS / MIUIX 设计规范与 UI 架构落地指南 (MIUIX Spec)

> 本文档是 PixEz MIUIX 项目的 UI 架构核心规范，提炼自项目基于 Compose Multiplatform 与 Xiaomi HyperOS (MIUIX) 的实战经验。
> 所有新增页面与通用组件开发必须严格遵循本文档规范。

---

## 目录

- [1. 核心设计原则与组件约束](#1-核心设计原则与组件约束)
- [2. 核心组件使用规范](#2-核心组件使用规范)
  - [2.1 顶栏与滚动联动 (TopAppBar & MiuixScrollBehavior)](#21-顶栏与滚动联动-topappbar--miuixscrollbehavior)
  - [2.2 卡片与列表项 (Card & BasicComponent)](#22-卡片与列表项-card--basiccomponent)
  - [2.3 弹窗与动作表单 (OverlayDialog & OverlayBottomSheet)](#23-弹窗与动作表单-overlaydialog--overlaybottomsheet)
  - [2.4 下拉刷新与加载指示器 (PullToRefresh & InfiniteProgressIndicator)](#24-下拉刷新与加载指示器-pulltorefresh--infiniteprogressindicator)
  - [2.5 悬浮胶囊底栏 (FloatingBottomBar)](#25-悬浮胶囊底栏-floatingbottombar)
- [3. 视觉动效与高级材质标准](#3-视觉动效与高级材质标准)
  - [3.1 Liquid Glass 液态玻璃渲染架构](#31-liquid-glass-液态玻璃渲染架构)
  - [3.2 动态流光着色器 (RuntimeShaderCompat)](#32-动态流光着色器-runtimeshadercompat)
  - [3.3 几何 Squircle 超椭圆与圆角](#33-几何-squircle-超椭圆与圆角)
- [4. 响应式与大屏适配规范 (Tablet & Desktop)](#4-响应式与大屏适配规范-tablet--desktop)
  - [4.1 屏幕断点与容器最大宽度](#41-屏幕断点与容器最大宽度)
  - [4.2 瀑布流自适应多列策略](#42-瀑布流自适应多列策略)
  - [4.3 悬浮底栏在大屏上的居中与宽度约束](#43-悬浮底栏在大屏上的居中与宽度约束)
- [5. 数据流、分页与触底加载标准](#5-数据流分页与触底加载标准)
- [6. 多语言与常量收敛规范](#6-多语言与常量收敛规范)

---

## 1. 核心设计原则与组件约束

1. **严格使用 MIUIX (`top.yukonga.miuix.kmp`) 官方组件体系**：
   - 严禁引入或泄漏 Material 3 (`androidx.compose.material3.*`) 控件。
   - 按钮、输入框、卡片、开关、单选/多选、进度条等基础组件必须统一使用 `top.yukonga.miuix.kmp.basic.*`。
2. **通透轻盈与层次分明**：
   - 页面背景采用低饱和通透基底，内容区采用带有微内边距的 Squircle 独立卡片容器包裹。
   - 避免无层次的平铺列表，优先使用带有圆角和间距的 MIUIX 分组卡片。
3. **触感反馈与物理阻尼**：
   - 交互动效遵循 HyperOS 物理曲线，列表触顶/触底支持 Overscroll 阻尼回弹。

---

## 2. 核心组件使用规范

### 2.1 顶栏与滚动联动 (TopAppBar & MiuixScrollBehavior)

- **标准顶栏**：
  ```kotlin
  val scrollBehavior = MiuixScrollBehavior()
  
  Scaffold(
      topBar = {
          TopAppBar(
              title = "页面标题",
              scrollBehavior = scrollBehavior,
              navigationIcon = {
                  IconButton(onClick = onBack) {
                      Icon(imageVector = MiuixIcons.Back, contentDescription = "返回")
                  }
              },
              actions = {
                  IconButton(onClick = onRefresh) {
                      Icon(imageVector = MiuixIcons.Refresh, contentDescription = "刷新")
                  }
              },
          )
      },
  ) { paddingValues ->
      // 必须将 scrollBehavior.nestedScrollConnection 注入滚动容器
      LazyColumn(
          modifier = Modifier
              .fillMaxSize()
              .nestedScroll(scrollBehavior.nestedScrollConnection),
          contentPadding = paddingValues,
      ) {
          // ...
      }
  }
  ```

### 2.2 卡片与列表项 (Card & BasicComponent)

- 所有设置项、功能项和信息分组必须使用 `top.yukonga.miuix.kmp.basic.Card` 包裹：
  ```kotlin
  SmallTitle(text = "系统设置")
  Card(
      modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
  ) {
      BasicComponent(
          title = "深色模式",
          summary = "跟随系统或强制开启",
          onClick = { /* 打开设置 */ },
          endActions = {
              Switch(checked = isDarkMode, onCheckedChange = { ... })
          },
      )
  }
  ```

### 2.3 弹窗与动作表单 (OverlayDialog & OverlayBottomSheet)

- 普通确认/操作对话框使用 `OverlayDialog`：
  ```kotlin
  OverlayDialog(
      title = "确认删除",
      summary = "删除后将无法恢复，确定继续吗？",
      show = showDialog,
      onDismissRequest = { showDialog = false },
  ) {
      Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
          TextButton(
              text = "取消",
              onClick = { showDialog = false },
              modifier = Modifier.weight(1f),
          )
          TextButton(
              text = "确定",
              onClick = { onConfirm() },
              colors = ButtonDefaults.textButtonColorsPrimary(),
              modifier = Modifier.weight(1f),
          )
      }
  }
  ```
- 多选项或复杂操作面板使用 `OverlayBottomSheet`。

### 2.4 下拉刷新与加载指示器 (PullToRefresh & InfiniteProgressIndicator)

- 下拉刷新必须使用 MIUIX 官方 `PullToRefresh`：
  ```kotlin
  PullToRefresh(
      isRefreshing = isRefreshing,
      onRefresh = { onRefresh() },
      modifier = Modifier.fillMaxSize(),
  ) {
      // 滚动列表容器
  }
  ```
- 列表触底或页面加载状态使用 `InfiniteProgressIndicator`，进度条使用 `LinearProgressIndicator`。

### 2.5 悬浮胶囊底栏 (FloatingBottomBar)

- 悬浮底栏采用双层结构：外层居中容器 + 限制最大宽度的 Squircle 胶囊：
  ```kotlin
  Box(
      modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .navigationBarsPadding()
          .padding(bottom = 12.dp),
      contentAlignment = Alignment.Center,
  ) {
      Row(
          modifier = Modifier
              .widthIn(
                  min = AppConstants.Layout.FLOATING_BAR_MIN_WIDTH_DP.dp,
                  max = AppConstants.Layout.FLOATING_BAR_MAX_WIDTH_DP.dp,
              )
              .fillMaxWidth(0.92f)
              .liquidGlass(...)
              .clip(RoundedCornerShape(32.dp)),
      ) {
          // Tab 项
      }
  }
  ```

### 2.6 设置项与偏好组件规范 (Preferences & Settings Architecture)

- **原生偏好组件选型**：
  - **开关项**：统一使用 `SwitchPreference`，要求标题清晰，并提供随开启/关闭状态切换的语义化 `summaryOn` / `summaryOff` 文案。
  - **二级页面与详情跳转**：统一使用 `ArrowPreference`。
  - **连续数值或断点调节**：统一使用 `SliderPreference`。
  - **单选枚举切换**：优先使用 `OverlayDropdownPreference` 或基于 `OverlayDialog` + `CheckIndicator` 的单选列表。
  - **操作触发与外部跳转**：使用 `BasicComponent`。
- **架构内聚与反碎片化准则**：
  - **严禁碎片化孤岛**：严禁仅为 1~2 个开关开辟独立的二级设置页面。
  - **领域聚合归类**：强相关联的业务开关（如敏感内容过滤与黑名单、收藏与下载联动）必须内聚收敛在同一个设置子页面中，通过 `SmallTitle` + `Card` 进行清晰的领域分组。

---

## 3. 视觉动效与高级材质标准

### 3.1 Liquid Glass 液态玻璃渲染架构

- 悬浮胶囊与毛玻璃容器采用 3 层复合渲染架构：
  1. **底层基底模糊**：`CombinedBackdrop` 实现高斯模糊（Radius 24dp~32dp）。
  2. **中层高光拾取**：根据主题亮暗度与手势焦点实时计算高光边缘（Highlight Border）。
  3. **顶层透镜折射**：结合内描边带来细腻通透的物理质感。

### 3.2 动态流光着色器 (RuntimeShaderCompat)

- 全局动态色彩流动背景采用平台兼容的着色器抽象：
  - **Android 端**：Android 13+ (API 33+) 采用原生 `android.graphics.RuntimeShader`；低版本平滑降级为多层高斯渐变。
  - **Desktop JVM 端**：基于 Skia `org.jetbrains.skia.RuntimeEffect` 进行实时 GLSL 渲染。
  - **统一接口**：通过 `RuntimeShaderCompat` 封装，页面层无感调用。

### 3.3 几何 Squircle 超椭圆与圆角

- 标准卡片圆角：`18.dp`
- 胶囊底栏圆角：`32.dp`
- 头像圆角：`CircleShape`
- 预览缩略图圆角：`12.dp`

---

## 4. 响应式与大屏适配规范 (Tablet & Desktop)

### 4.1 屏幕断点与容器最大宽度

为避免大屏（平板、折叠屏展开态、桌面宽屏）上内容被无限横向拉伸导致失真，统一收敛以下约束至 `AppConstants.Layout`：

| 布局常量 | 推荐值 | 适用场景 |
| :--- | :---: | :--- |
| `TABLET_CONTENT_MAX_WIDTH_DP` | `760.dp` | 详情页正文、设置页、单列表页面居中最大宽度 |
| `FLOATING_BAR_MAX_WIDTH_DP` | `540.dp` | 悬浮底栏胶囊最大宽度 |
| `FLOATING_BAR_MIN_WIDTH_DP` | `320.dp` | 悬浮底栏胶囊最小宽度 |
| `GRID_CARD_MIN_WIDTH_DP` | `180.dp` | 瀑布流卡片最小宽度（用于计算自适应列数） |

### 4.2 瀑布流自适应多列策略

- 插画列表（`IllustStaggeredGrid`）与特辑发现（`SpotlightScreen`）采用 `StaggeredGridCells.Adaptive`：
  ```kotlin
  LazyVerticalStaggeredGrid(
      columns = StaggeredGridCells.Adaptive(minSize = AppConstants.Layout.GRID_CARD_MIN_WIDTH_DP.dp),
      verticalItemSpacing = 8.dp,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
      // items
  }
  ```
  - 手机竖屏（宽度 ~360dp-420dp）：自适应 2 列。
  - 平板竖屏 / 折叠屏（宽度 ~600dp-800dp）：自适应 3-4 列。
  - 平板横屏 / 桌面宽屏（宽度 >1200dp）：自适应 5-7 列。

### 4.3 悬浮底栏在大屏上的居中与宽度约束

- 大屏上悬浮底栏必须保持水平居中，并严格限制在 `540.dp` 内，严禁全宽顶满屏幕边缘。

---

## 5. 数据流、分页与触底加载标准

1. **Repository 层**：
   - 涉及列表流式查询的方法必须提供返回完整响应对象的接口（包含 `@SerialName("next_url") val nextUrl: String?`）。
2. **列表触底预加载**：
   - 使用 `derivedStateOf` 与 `LazyListState` / `LazyStaggeredGridState`：
   ```kotlin
   val shouldLoadMore by remember(items.size, nextUrl, isLoadingMore, loadMoreError) {
       derivedStateOf {
           val totalCount = items.size
           if (totalCount == 0 || nextUrl == null || isLoadingMore || loadMoreError != null) {
               false
           } else {
               val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: -1
               lastVisibleIndex >= totalCount - 4
           }
       }
   }
   
   LaunchedEffect(shouldLoadMore) {
       if (shouldLoadMore) {
           loadMore()
       }
   }
   ```
3. **黑名单统一过滤**：
   - 所有插画列表在追加（Append）或刷新时，必须经过 `BanRepository`（屏蔽作品 ID、画师 ID、屏蔽标签）及 `banAIIllust` 过滤。

---

## 6. 多语言与常量收敛规范

1. **UI 文本一律接入多语言体系**：
   - 禁止在 Composable 中硬编码用户可见文本。
   - 所有文本通过 `LocalStrings.current.xxx` 引用，定义在 `AppStrings.kt` 中，并同步更新各语言实现（`AppStringsZh.kt`、`AppStringsEn.kt`、`AppStringsId.kt` 等）。
2. **外部链接与配置常量统一收敛**：
   - 项目主页、Release 链接、API 端点、预设档位统一收敛至 `AppConstants.kt`。
