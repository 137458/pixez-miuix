# M26 里程碑：用户详情外部链接展示

## 状态
已完成。

## 目标
原 Flutter PixEz 的用户详情页会展示画师的外部链接（Twitter、个人网页、Pawoo 等）。M26 将为 MIUIX 版 `UserDetailScreen` 增加这些链接的展示与点击跳转。

## 范围

### 必做（按最小任务量拆分）

1. **外部链接解析**
   - 在 `UserProfileHeader` 中读取 `userDetail.profile.twitterUrl`、`webpage`、`pawooUrl`。
   - 仅当链接非空时才展示对应入口。

2. **链接 UI**
   - 在关注/好P友统计下方新增横向链接区域。
   - 每个链接使用可点击文本（如「Twitter」「网页」「Pawoo」），点击后调用 `openBrowser(url)`。
   - 链接之间保持适当间距。

3. **错误兜底**
   - 链接跳转失败时静默忽略（`runCatching`），不阻塞用户浏览。

## 验收条件

- [x] 用户详情页在存在外部链接时展示对应入口。
- [x] 点击入口调用系统浏览器打开链接。
- [x] 无链接时不显示空白区域或异常占位。
- [x] Android + Desktop 双端编译通过。
- [x] M26 code review 完成，无 P0/P1 问题遗留。
