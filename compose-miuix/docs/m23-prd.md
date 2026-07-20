# M23 里程碑：搜索历史单条删除

## 状态
已完成。

## 目标
原 Flutter PixEz 的搜索历史支持删除单条记录。M23 将为 MIUIX 版 `SearchScreen` 的搜索历史列表增加单条删除入口，替代当前只能「清空全部」的限制。

## 范围

### 必做（按最小任务量拆分）

1. **搜索历史项 UI 调整**
   - `SearchSuggestions` 中每个历史项改为左右布局：左侧文本保持点击搜索，右侧显示删除按钮。
   - 删除按钮使用 Material `Icons.Default.Close`，点击后移除对应历史记录。

2. **删除逻辑**
   - `SearchScreen` 向 `SearchSuggestions` 传入 `onHistoryRemove: (String) -> Unit` 回调。
   - 回调内部从 `searchHistory` 中过滤掉该条记录，并调用 `settingsRepository.setStringList` 回写持久化。

3. **清空全部保留**
   - 原有「清空历史」按钮继续保留，功能不变。

## 验收条件

- [x] 搜索历史每条记录右侧出现删除按钮。
- [x] 点击删除按钮后该条历史从列表和持久化设置中移除。
- [x] 「清空历史」功能仍正常工作。
- [x] Android + Desktop 双端编译通过。
- [x] M23 code review 完成，无 P0/P1 问题遗留。
