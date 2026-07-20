# M27 里程碑：用户详情背景图

## 状态
已完成。

## 目标
原 Flutter PixEz 的用户详情页会展示画师设置的背景图。M27 将为 MIUIX 版 `UserDetailScreen` 增加背景图展示，提升页面视觉完整度。

## 范围

### 必做（按最小任务量拆分）

1. **背景图读取**
   - 在 `UserProfileHeader` 中读取 `userDetail.profile.backgroundImageUrl`。
   - 仅当链接非空时展示背景图。

2. **背景图 UI**
   - 在 `UserProfileHeader` 的 Column 顶部，头像上方展示背景图。
   - 背景图宽度填满，高度固定为 120.dp。
   - 使用 `ContentScale.Crop` 保持原应用常见的封面图裁切效果。
   - 背景图与头像之间保持 16.dp 间距。

3. **加载与错误兜底**
   - 使用现有 `PixivAsyncImage` 自动处理加载与失败占位。
   - 背景图加载失败时仅不显示，不影响其他信息展示。

## 验收条件

- [x] 用户详情页在存在背景图时顶部展示背景图。
- [x] 无背景图时不显示占位区域。
- [x] Android + Desktop 双端编译通过。
- [x] M27 code review 完成，无 P0/P1 问题遗留。
