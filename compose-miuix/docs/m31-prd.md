# M31 里程碑：作品标签点击搜索

## 状态
已完成。

## 目标
原 Flutter PixEz 的作品详情页中，点击标签可直接跳转搜索。M31 将为 MIUIX 版 `IllustDetailScreen` 增加标签点击搜索能力。

## 范围

### 必做（按最小任务量拆分）

1. **搜索路由扩展**
   - `RootComponent` 新增 `Search(query)` Config 与 Child，支持以指定关键词进入搜索页。
   - `RootComponent` 新增 `onSearchClicked(query)` 方法。
   - `SearchScreen` 新增 `initialQuery: String = ""` 参数，进入时自动填充并触发搜索。

2. **标签点击入口**
   - `IllustDetailScreen` 标签列表中的每个标签变为可点击。
   - 点击后调用 `onTagClick(tag.name)` 跳转搜索（使用原始标签名）。

3. **RootContent 接入**
   - 处理 `Child.Search` 路由，渲染带初始查询词的 `SearchScreen`。
   - 作品详情页的标签点击回调转发到 `component.onSearchClicked(query)`。

## 验收条件

- [x] 作品详情页标签可点击。
- [x] 点击标签后进入搜索页并展示对应关键词的搜索结果。
- [x] Android + Desktop 双端编译通过。
- [x] M31 code review 完成，无 P0/P1 问题遗留。
