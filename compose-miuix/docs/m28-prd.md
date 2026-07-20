# M28 里程碑：评论发表

## 状态
已完成。

## 目标
原 Flutter PixEz 的作品评论页支持发表评论。M28 将为 MIUIX 版 `CommentsScreen` 增加评论发表功能。

## 范围

### 必做（按最小任务量拆分）

1. **评论发表 API**
   - `IllustRepository` 新增 `postComment(illustId, comment)`，调用 Pixiv `/v1/illust/comment/add`（与原 Flutter 应用一致）。
   - 请求参数：`illust_id`、`comment`。
   - 成功返回 `Unit`；失败通过 `networkCall` 抛出异常。

2. **评论输入 UI**
   - `CommentsScreen` 底部增加固定输入区：TextField + 发送按钮。
   - 未登录或评论为空时禁用发送按钮。
   - 发送中显示加载态，防止重复提交。

3. **发送后刷新**
   - 评论发表成功后，通过递增 `retryCount` 触发评论列表重新加载。
   - 发送成功清空输入框。
   - 发送失败时保留输入内容，便于用户重试。

## 验收条件

- [x] `IllustRepository` 新增 `postComment` 并成功调用 Pixiv API。
- [x] `CommentsScreen` 底部出现评论输入框与发送按钮。
- [x] 评论发送成功后刷新列表并清空输入框。
- [x] Android + Desktop 双端编译通过。
- [x] M28 code review 完成，无 P0/P1 问题遗留。
