# M17 里程碑：搜索 Ugoira 筛选

## 状态
已完成。commit 见 `6403c8c3`。

## 目标
原 Flutter PixEz 搜索结果页支持 Ugoira（动图）筛选：全部、仅动图、排除动图。M17 将为 MIUIX 搜索页补充该能力，使用户可按作品类型过滤搜索结果。

## 范围

### 必做（按最小任务量拆分）

1. **Ugoira 筛选状态持久化**
   - `SettingsKeys` 新增 `SEARCH_UGOIRA_FILTER` 键。
   - `SearchScreen` 进入页面时从 `SettingsRepository` 读取 Ugoira 筛选状态。
   - 切换 Ugoira 筛选时回写 `SettingsRepository`。
   - 默认值：`0`（全部）。

2. **Ugoira 筛选 UI**
   - `SearchFilterBar` 增加 Ugoira 筛选 TabRow：全部、仅动图、排除动图。
   - 切换后重新过滤当前搜索结果。

3. **本地过滤逻辑**
   - 在 `SearchIllustResultGrid` 展示结果前，根据 `ugoiraFilter` 过滤 `illusts` 列表。
   - 过滤规则：
     - `0`（全部）：不过滤。
     - `1`（仅动图）：保留 `type == "ugoira"`。
     - `2`（排除动图）：排除 `type == "ugoira"`。

## 验收条件

- [x] `SettingsKeys` 新增 `SEARCH_UGOIRA_FILTER` 键。
- [x] `SearchScreen` 进入时恢复上一次的 Ugoira 筛选状态。
- [x] 切换 Ugoira 筛选后，当前搜索结果按规则过滤。
- [x] Android + Desktop 双端编译通过。
- [x] M17 code review 完成，无 P0/P1 问题遗留。
