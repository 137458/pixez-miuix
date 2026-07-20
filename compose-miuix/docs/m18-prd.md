# M18 里程碑：搜索时间范围筛选

## 状态
已完成。

- 完成 commit：`d48d38f1`
- 完成时间：2026-07-19

## 目标
原 Flutter PixEz 搜索页支持选择时间范围（开始日期 / 结束日期）后再请求搜索结果。M18 将为 MIUIX 搜索页补充该能力，使用户可按指定日期区间过滤插画。

## 范围

### 必做（按最小任务量拆分）

1. **API 层支持**
   - `SearchRepository.searchIllust` 新增 `startDate` 与 `endDate` 参数。
   - 仅当参数非空时附加 `start_date` / `end_date` query parameter。
   - 日期格式与旧版保持一致：`YYYY-M-D`（与 `api_client.dart#getFormatDate` 一致）。

2. **筛选状态持久化**
   - `SettingsKeys` 新增 `SEARCH_START_DATE`、`SEARCH_END_DATE` 键。
   - `SearchScreen` 进入页面时读取时间范围，切换后回写 `SettingsRepository`。
   - 存储格式：`YYYY-MM-DD`，便于输入与回显；发送至 API 前再格式化为 `YYYY-M-D`。

3. **时间范围筛选 UI**
   - `SearchFilterBar` 增加开始日期 / 结束日期输入区。
   - 使用 MIUIX `TextField` 接收 `YYYY-MM-DD` 格式输入。
   - 提供「清空」按钮移除已选时间范围。
   - 输入有效日期后自动重新加载结果。
   - 对用户输入做 500ms 防抖，并校验 `YYYY-MM-DD` 格式，避免逐字输入触发频繁请求。

## 验收条件

- [x] `SearchRepository.searchIllust` 支持 `startDate` / `endDate`。
- [x] `SettingsKeys` 新增 `SEARCH_START_DATE`、`SEARCH_END_DATE`。
- [x] `SearchScreen` 进入时恢复时间范围，变化时回写设置。
- [x] 输入有效时间范围后，搜索结果按日期区间过滤。
- [x] Android + Desktop 双端编译通过。
- [x] M18 code review 完成，无 P0/P1 问题遗留。
