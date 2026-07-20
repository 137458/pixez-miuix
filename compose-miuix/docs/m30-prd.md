# M30 里程碑：评论回复

## 状态
已完成。

## 目标
原 Flutter PixEz 的作品评论页支持对指定评论进行回复。M30 将为 MIUIX 版 `CommentsScreen` 增加评论回复能力。

## 范围

### 必做（按最小任务量拆分）

1. **回复 API 扩展**
   - `IllustRepository.postComment(illustId, comment, parentCommentId = null)`。
   - `parentCommentId` 非空时，请求参数增加 `parent_comment_id`。

2. **回复 UI**
   - 每条评论显示「回复」按钮（仅对具备 ID 的评论展示）。
   - 点击回复按钮后，底部输入栏显示回复目标用户提示与取消按钮。
   - 发送时携带 `parent_comment_id`。
   - 发送成功后清空输入与回复目标，并刷新列表。

3. **取消回复**
   - 输入栏提供取消按钮，清除当前回复目标，恢复为普通评论。

## 验收条件

- [x] `IllustRepository.postComment` 支持可选 `parentCommentId`。
- [x] `CommentsScreen` 可对指定评论发起回复。
- [x] 回复发送成功后清空输入并刷新列表。
- [x] Android + Desktop 双端编译通过。
- [x] M30 code review 完成，无 P0/P1 问题遗留。
