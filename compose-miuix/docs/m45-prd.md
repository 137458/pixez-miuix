# M45 里程碑：屏蔽标签数据层

## 状态
已完成。

## 目标
原 Flutter PixEz 支持将指定标签加入屏蔽列表（`bantag.db`），使包含该标签的作品不在列表中展示，并支持以 `r'...'` 形式书写正则表达式。M45 在 MIUIX 版中建立对应的屏蔽标签数据层，保持与旧版数据库文件 `bantag.db` 兼容，为后续列表页过滤做准备。

## 范围

### 必做（按最小任务量拆分）

1. **SQLDelight 数据库**
   - 新增 `BanTag.sq`，表名 `bantag`，字段 `id`（自增主键）、`translate_name`（文本，标签翻译名）、`name`（文本，标签名或正则表达式）。
   - 数据库文件名为 `bantag.db`，与旧 Flutter 应用保持一致。

2. **仓库扩展**
   - 扩展 `BanRepository`，增加 `banTagQueries`。
   - 新增 `getAllBanTags()`：查询全部被屏蔽标签。
   - 新增 `isBanTag(tag: String)`：查询指定标签是否被屏蔽（精确匹配 `name` 或 `translate_name`）。
   - 新增 `isBannedByTags(tags: List<String>)`：查询标签列表中是否包含被屏蔽标签，支持正则表达式（当 `name` 以 `r'` 开头且以 `'` 结尾时，中间内容作为正则匹配）。
   - 新增 `insertBanTag(name: String, translateName: String)`：将标签加入屏蔽列表，存在时先删除再插入（对齐旧版 replace 行为）。
   - 新增 `deleteBanTag(id: Long)`：按主键删除。
   - 新增 `clearAllBanTags()`：清空全部屏蔽标签。

3. **依赖注册**
   - `AppDependencies` 新增 `banTagDriver`，使用 `DriverFactory` 打开 `bantag.db`。
   - `BanRepository` 构造函数接收 `banIllustIdDriver`、`banUserIdDriver` 与 `banTagDriver` 三个驱动。
   - `AppDependencies.close()` 中关闭 `banTagDriver`。

4. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M45 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用现有 `SQLDelight + DriverFactory` 机制，为屏蔽标签单独打开 `bantag.db`，与旧版文件路径兼容。
- 正则表达式标签匹配逻辑严格对齐原 Flutter 实现：`name` 以 `r'` 开头且以 `'` 结尾时，中间内容作为 Regex；否则按精确字符串匹配 `name` 与 `translate_name`。
- 正则编译失败时捕获异常并视为不匹配，避免非法正则导致崩溃。
- `BanRepository` 同时管理 `banillustid.db`、`banuserid.db` 与 `bantag.db`，避免向 UI 层引入新的依赖参数。
- 所有公开方法均为 suspend，内部切到 `Dispatchers.IO`，与 M34/M43 保持一致。

## 验收条件

- [x] `BanRepository` 可正常读写 `bantag.db`。
- [x] `isBannedByTags(tags)` 正确识别被屏蔽标签，支持普通标签与正则表达式标签。
- [x] 正则表达式非法时不会崩溃，降级为不匹配。
- [x] `AppDependencies` 正确创建并关闭 `banTagDriver`。
- [x] Android + Desktop 双端编译通过。
- [x] M45 code review 完成，无 P0/P1 问题遗留。
