# M13 里程碑：搜索画师 Tab

## 状态
已完成。commit 见 `待填充`。

## 目标
原 Flutter PixEz 搜索页支持按关键词搜索画师（用户）。当前 MIUIX 搜索页仅支持搜索插画，M13 将增加「画师」Tab，保持搜索交互一致。

## 范围

### 必做（按最小任务量拆分）

1. **画师搜索 API**
   - `SearchRepository` 新增 `searchUser(word)`，调用 `/v1/search/user`。
   - 复用现有 `UserPreviewsResponse` / `UserPreview` 模型解析响应。

2. **搜索页 Tab 切换**
   - `SearchScreen` 顶部增加「作品 / 画师」TabRow。
   - 「作品」Tab 保持现有插画搜索与筛选逻辑。
   - 「画师」Tab 使用 `searchUser` 加载用户预览列表，展示头像、名称、账号及最近作品预览。
   - 点击画师项导航到用户详情页。

3. **入口接入**
   - `RootContent` 向 `SearchScreen` 传入 `onUserClick` 回调。

## 验收条件

- [ ] `SearchRepository.searchUser` 成功返回画师预览列表。
- [ ] `SearchScreen` 可切换「作品 / 画师」Tab 并分别展示结果。
- [ ] 点击画师项导航到用户详情页。
- [ ] Android + Desktop 双端编译通过。
- [ ] M13 code review 完成，无 P0/P1 问题遗留。
