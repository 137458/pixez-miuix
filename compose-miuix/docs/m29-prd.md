# M29 里程碑：排行榜日期选择

## 状态
已完成。

## 目标
原 Flutter PixEz 的排行榜支持选择日期查看历史榜单。M29 将为 MIUIX 版 `RankingScreen` 增加日期选择能力。

## 范围

### 必做（按最小任务量拆分）

1. **日期输入 UI**
   - `RankingScreen` 顶部模式选择条下方增加日期输入区。
   - 使用 MIUIX `TextField` 接收 `YYYY-MM-DD` 格式日期。
   - 提供「清空」按钮移除已选日期，回到最新榜单。
   - 日期格式校验失败时显示错误提示。

2. **加载指定日期榜单**
   - 将输入的合法日期传递给 `repository.getRanking(selectedMode.code, selectedDate)`。
   - 日期变化时通过 500ms 防抖后重新加载榜单。
   - 日期为空时加载最新榜单（与现有行为一致）。

3. **状态持久化（可选，本里程碑不做）**
   - 本里程碑仅保持内存状态，不持久化日期选择。

## 验收条件

- [x] `RankingScreen` 出现日期输入区与清空按钮。
- [x] 输入合法日期后重新加载对应日期的排行榜。
- [x] 清空日期后恢复最新排行榜。
- [x] Android + Desktop 双端编译通过。
- [x] M29 code review 完成，无 P0/P1 问题遗留。
