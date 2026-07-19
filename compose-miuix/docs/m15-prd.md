# M15 里程碑：搜索筛选状态持久化

## 状态
已完成。commit 见 `31ebbc35`。

## 目标
原 Flutter PixEz 搜索页支持"记住当前选择"，M15 将为 MIUIX 搜索页补充筛选状态持久化能力，使用户下次进入搜索页时保留上一次的排序、搜索目标、AI 类型与收藏数阈值设置。

## 范围

### 必做（按最小任务量拆分）

1. **筛选状态持久化**
   - `SettingsKeys` 新增搜索筛选相关键名：`SEARCH_SORT`、`SEARCH_TARGET`、`SEARCH_AI_TYPE`、`SEARCH_BOOKMARK_THRESHOLD`。
   - `SearchScreen` 进入页面时从 `SettingsRepository` 读取上述状态并初始化。
   - 切换排序、搜索目标、AI 类型、收藏数阈值时回写 `SettingsRepository`。

2. **默认值兼容**
   - 读取不到时使用现有默认值：sort = `date_desc`，searchTarget = `partial_match_for_tags`，searchAiType = `0`，bookmarkThreshold = `0`。

## 验收条件

- [x] `SettingsKeys` 新增 4 个搜索筛选状态键。
- [x] `SearchScreen` 进入时恢复上一次的筛选状态。
- [x] 切换筛选条件后，重新进入搜索页仍保持上次选择。
- [x] Android + Desktop 双端编译通过。
- [x] M15 code review 完成，无 P0/P1 问题遗留。
