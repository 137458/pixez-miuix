# M32 里程碑：下载任务历史管理

## 状态
已完成。

## 目标
原 Flutter PixEz 在作品下载后会记录下载历史，用户可在设置页查看已下载作品列表。M32 将为 MIUIX 版复用已有的 `task.db` 表结构，实现下载任务持久化与下载历史列表页。

## 范围

### 必做（按最小任务量拆分）

1. **下载历史持久化层**
   - 复用现有 `TaskDatabase` 与 `Task.sq` 表结构，新增 `DownloadHistoryRepository`。
   - 提供 `saveTask(task)`、`getAllTasks()`、`getTasksByStatus(status)`、`deleteTask(id)`、`clearAll()` 方法。
   - 将 SQLDelight 生成的 `Task` 行映射为对外 `DownloadTaskHistory` 模型。

2. **下载流程写入历史**
   - 修改 `DownloadRepository.download(illust, pageIndex)`：下载前将任务以 `Downloading` 状态写入 `task` 表；完成后更新为 `Success` 或 `Failed`，并记录错误信息。
   - 多页下载 `downloadAllPages` 逐页复用单页逻辑，自动写入多条历史。

3. **下载历史列表页**
   - 新增 `DownloadHistoryScreen`：按时间倒序展示所有下载任务，显示标题、页码、文件名、状态。
   - 支持长按或点击菜单删除单条记录，提供「清空全部」入口。
   - 点击条目跳转对应作品详情页。

4. **设置页入口与路由**
   - `SettingsScreen` 新增「下载历史」入口。
   - `RootComponent` 新增 `DownloadHistory` Config/Child 与 `onDownloadHistoryClicked()` 方法。
   - `RootContent` 处理 `Child.DownloadHistory` 路由。

## 技术决策

- 复用旧 Flutter 遗留的 `task.db` 与 `Task.sq` 表结构，不新增数据表，保持数据兼容性。
- `status` 字段映射：`0=Pending, 1=Downloading, 2=Success, 3=Failed`。
- 历史记录中不保存本地绝对路径，仅保存 `file_name`；跨平台路径差异由 `IllustSaver` 各平台实现决定。
- UI 状态管理使用 `produceState` + `runCatchingNonCancel`，与 M4 后各列表页保持一致。

## 验收条件

- [x] `IllustDetailScreen` 触发下载后，`task` 表新增对应记录。
- [x] 下载成功/失败状态正确回写数据库。
- [x] 设置页「下载历史」可进入列表页并展示任务。
- [x] 点击历史条目可跳转作品详情。
- [x] Android + Desktop 双端编译通过。
- [x] M32 code review 完成，无 P0/P1 问题遗留。
