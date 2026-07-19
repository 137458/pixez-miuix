# M14 里程碑：搜索高级筛选

## 状态
进行中。

## 目标
原 Flutter PixEz 搜索页支持多种高级筛选条件。当前 MIUIX 搜索页已支持排序与搜索目标，M14 将补充 AI 类型筛选与收藏数阈值筛选，保持搜索能力与原应用一致。

## 范围

### 必做（按最小任务量拆分）

1. **AI 类型筛选**
   - `SearchRepository.searchIllust` 新增 `searchAiType` 参数（0 = 全部，1 = 排除 AI 生成）。
   - 调用 `/v1/search/illust` 时按需传入 `search_ai_type`。
   - `SearchScreen` 在作品 Tab 的筛选栏中增加「AI 生成」Switch，默认开启（即 `searchAiType = 0`）。

2. **收藏数阈值筛选**
   - `SearchScreen` 在作品 Tab 的筛选栏中增加收藏数阈值选项：默认、100 users入り、250 users入り、500 users入り、1000 users入り、5000 users入り、10000 users入り、20000 users入り、30000 users入り、50000 users入り。
   - 选择非默认项时，在搜索词后追加" ${value}users入り"，复用现有 `searchIllust` 逻辑。

## 验收条件

- [ ] `SearchRepository.searchIllust` 支持 `searchAiType` 参数。
- [ ] `SearchScreen` 可切换 AI 类型筛选并重新加载结果。
- [ ] `SearchScreen` 可选择收藏数阈值并重新加载结果。
- [ ] Android + Desktop 双端编译通过。
- [ ] M14 code review 完成，无 P0/P1 问题遗留。
