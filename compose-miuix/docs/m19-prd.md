# M19 里程碑：排行榜模式扩展

## 状态
进行中。

## 目标
原 Flutter PixEz 排行榜支持更多模式（包括 AI 生成与 R18 分区）。M19 将为 MIUIX 排行榜页补齐缺失的模式选项，使用户可选择与原应用一致的排行榜类型。

## 范围

### 必做（按最小任务量拆分）

1. **补齐 RankingMode 枚举**
   - 在 `RankingScreen.kt` 的 `RankingMode` 枚举中增加以下模式：
     - `day_ai`（AI 生成日榜）
     - `day_r18_ai`（AI 生成 R18 日榜）
     - `day_r18`（R18 日榜）
     - `week_r18`（R18 周榜）
     - `week_r18g`（R18G 周榜）
   - 标签使用与原应用中文翻译一致的文案。

2. **复用现有 API 调用**
   - `repository.getRanking(selectedMode.code)` 已接受任意模式字符串，无需修改 `IllustRepository`。

3. **UI 展示**
   - 现有 `RankingModeSelector` 会遍历所有枚举项，新增模式会自动出现在选择条中。
   - 选择条水平滚动，可容纳更多选项。

## 验收条件

- [ ] `RankingMode` 枚举补齐 AI / R18 / R18G 共 5 个新模式。
- [ ] 排行榜页可见并能选择新增模式。
- [ ] Android + Desktop 双端编译通过。
- [ ] M19 code review 完成，无 P0/P1 问题遗留。
