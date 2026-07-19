# M10 里程碑：搜索结果基础筛选

## 状态
进行中。

## 目标
原 Flutter PixEz 搜索结果页支持排序与搜索目标筛选。当前 MIUIX 搜索仅支持默认排序与默认目标，M10 将补齐最常用的排序与目标筛选能力，保持与原应用行为一致。

## 范围

### 必做（按最小任务量拆分）

1. **排序筛选**
   - 在搜索结果区域顶部提供排序选择：最新降序（`date_desc`）、最新升序（`date_asc`）、人气降序（`popular_desc`）。
   - 切换排序后重新加载搜索结果。

2. **搜索目标筛选**
   - 在排序选择旁提供搜索目标选择：标签部分一致（`partial_match_for_tags`）、标签完全一致（`exact_match_for_tags`）、标题与说明（`title_and_caption`）。
   - 切换目标后重新加载搜索结果。

3. **UI 集成**
   - 筛选栏仅在用户执行搜索后（结果展示状态）显示。
   - 使用 MIUIX `TabRow` 实现排序与目标切换，保持与 M9 用户详情页 Tab 风格一致。

### 不做

- 日期范围筛选（原应用支持，超出最小任务量，迁出至后续里程碑）。
- 收藏数（users 入り）筛选。
- Ugoira / AI 生成等高级筛选。
- 搜索筛选持久化（记住当前选择）。
- 画师搜索结果 Tab（原应用有「插画 / 画师」两个 Tab，迁出至后续里程碑）。

## 技术决策

- 复用 `SearchRepository.searchIllust(word, sort, searchTarget)` 已有参数，不扩展仓库接口。
- 筛选状态使用 `rememberSaveable` 保存，避免配置变更后重置。
- Tab 组件使用 MIUIX 0.8.8 `top.yukonga.miuix.kmp.basic.TabRow`。
- 沿用 `produceState` + `runCatchingNonCancel` 加载模式。

## 验收条件

- [ ] 搜索结果页顶部显示排序与目标筛选 TabRow。
- [ ] 切换排序后重新加载对应排序结果。
- [ ] 切换搜索目标后重新加载对应目标结果。
- [ ] Android + Desktop 双端编译通过。
- [ ] M10 code review 完成，无 P0/P1 问题遗留。
