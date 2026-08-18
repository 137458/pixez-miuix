# 全局待办清单 (Global TODO)

> **统一管理**
> 本清单整合了 PixEz MIUIX 项目各模块待办事项、功能演进、大屏适配与架构优化任务，按优先级排序。
> - 单项任务完成后勾选 `- [x]`
> - 新增待办请按优先级插入对应分组
> - 已完成项目保留 `- [x]` 标记，便于审计与版本追溯

---

## P0 - 高优先级（核心业务闭环与稳定性）

### 列表流式分页与刷新体系（已完成）
- [x] **搜索模块流式分页**（插画搜索网格与画师搜索列表支持无限翻页）
- [x] **首页与推荐流式分页**（`HelloScreen` 支持 `nextUrl` 连续拉取与 `PullToRefresh`）
- [x] **关注动态流式分页**（`NewScreen` 支持按全部/公开/私密流式翻页）
- [x] **排行榜流式分页**（`RankingScreen` 支持日/周/月/男性向/女性向等模式下的触底流式翻页）
- [x] **作品评论流式分页**（`CommentsScreen` 接入 `getIllustCommentsResponse` 与下拉刷新）
- [x] **关注与粉丝列表流式分页**（`UserFollowListScreen` 与 `UserFollowerListScreen` 流式翻页）
- [x] **画师主页作品与收藏分页**（`UserDetailScreen` 作品与收藏 Tab 无限分页）
- [x] **系列作品分页**（`IllustSeriesScreen` 连载系列作品流式分页）

### 屏蔽与安全引擎（已完成）
- [x] **作品 ID / 画师 ID 黑名单**（`BanRepository` 本地持久化与即时过滤）
- [x] **标签黑名单**（模糊与精准匹配过滤）
- [x] **AI 生成作品过滤**（`banAIIllust` 全局开关与响应过滤）

---

## P1 - 中优先级（大屏响应式体验与 MIUIX 动效）

### 大屏与平板视觉调优（已完成）
- [x] **悬浮底栏平板适配**（胶囊最大宽度限制在 540dp 内并水平居中，消除全屏拉伸）
- [x] **自适应多列瀑布流**（`IllustStaggeredGrid` 与 `SpotlightScreen` 在大屏自适应 3-7 列）
- [x] **详情与设置容器限宽**（`IllustDetailScreen` 与 `SettingsScreen` 大屏居中限宽 760dp）

### 视觉动效与细节打磨
- [x] **Liquid Glass 3层渲染**（`CombinedBackdrop` 高斯模糊与高光折射）
- [x] **动态流光着色器兼容层**（`RuntimeShaderCompat` 在 Android 13+ 与 Desktop Skia 渲染）
- [ ] **动图 (Ugoira) 播放器手势与全屏沉浸缩放**
- [ ] **多图长图模式平滑阅读器**

---

## P2 - 中低优先级（系统扩展与多语言）

### 国际化与本地化 (i18n)
- [x] **中文简体**（`AppStringsZh.kt` 100% 覆盖）
- [x] **英文 (English)**（`AppStringsEn.kt` 基础覆盖）
- [x] **印尼语 (Bahasa Indonesia)**（`AppStringsId.kt` 基础覆盖）
- [ ] **日文 (日本語)**（扩充完整日文翻译字典）
- [ ] **繁体中文 (繁體中文)**（扩充完整繁体中文翻译字典）

### 存储与任务系统
- [x] **多任务并发下载队列**（`DownloadRepository` 状态流与持久化）
- [x] **浏览历史管理与模糊搜索**（`HistoryScreen`）
- [ ] **下载文件命名格式自定义**（如 `{illust_id}_{title}_{user_name}`）
- [ ] **数据导出与跨端恢复工具完善**

---

## P3 - 低优先级（桌面原生与发布准备）

### 桌面端 (Desktop) 专属体验优化
- [ ] **快捷键支持**（空格翻页、Esc 返回、Ctrl+S 保存原图）
- [ ] **托盘图标与后台常驻下载**
- [ ] **多窗口与详情弹窗分屏支持**

### 发布与自动化
- [ ] **GitHub Actions 多平台自动编译构建 (Android APK + Desktop MSI/DMG/Deb)**
- [ ] **发布版本 Changelog 自动生成**
