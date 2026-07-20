# M43 里程碑：屏蔽画师数据层

## 状态
已完成。

## 目标
原 Flutter PixEz 支持将指定画师加入屏蔽列表（`banuserid.db`），使其作品不在列表中展示。M43 在 MIUIX 版中建立对应的屏蔽画师数据层，保持与旧版数据库文件 `banuserid.db` 兼容，为后续列表页过滤做准备。

## 范围

### 必做（按最小任务量拆分）

1. **SQLDelight 数据库**
   - 新增 `BanUserId.sq`，表名 `banuserid`，字段 `id`（自增主键）、`user_id`（文本，被屏蔽画师 ID）、`name`（画师名称）。
   - 数据库文件名为 `banuserid.db`，与旧 Flutter 应用保持一致。

2. **仓库扩展**
   - 扩展 `BanRepository`，增加 `banUserIdQueries`。
   - 新增 `getAllBanUsers()`：查询全部被屏蔽画师。
   - 新增 `getBannedUserIds()`：查询被屏蔽画师 ID 集合（`Set<Int>`）。
   - 新增 `isBanUser(userId: Int)`：查询指定画师是否被屏蔽。
   - 新增 `insertBanUser(userId: Int, name: String)`：将画师加入屏蔽列表，存在时先删除再插入（对齐旧版 replace 行为）。
   - 新增 `deleteBanUser(id: Long)`：按主键删除。

3. **依赖注册**
   - `AppDependencies` 新增 `banUserDriver`，使用 `DriverFactory` 打开 `banuserid.db`。
   - `BanRepository` 构造函数接收 `banIllustIdDriver` 与 `banUserIdDriver` 两个驱动。
   - `AppDependencies.close()` 中关闭 `banUserDriver`。

4. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M43 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用现有 `SQLDelight + DriverFactory` 机制，为屏蔽画师单独打开 `banuserid.db`，与旧版文件路径兼容。
- `BanRepository` 同时管理 `banillustid.db` 与 `banuserid.db`，避免向 UI 层引入新的依赖参数。
- 所有公开方法均为 suspend，内部切到 `Dispatchers.IO`，与 M34 保持一致。

## 验收条件

- [x] `BanRepository` 可正常读写 `banuserid.db`。
- [x] `getBannedUserIds()` 返回 `Set<Int>` 用于后续列表页过滤。
- [x] `AppDependencies` 正确创建并关闭 `banUserDriver`。
- [x] Android + Desktop 双端编译通过。
- [x] M43 code review 完成，无 P0/P1 问题遗留。
