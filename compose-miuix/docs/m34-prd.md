# M34 里程碑：作品详情页屏蔽作品

## 状态
进行中。

## 目标
原 Flutter PixEz 在作品详情页右上角「更多」菜单中提供「屏蔽」入口，可将当前作品加入屏蔽列表，并在详情页显示屏蔽占位提示。M34 将在 MIUIX 版复刻该功能，保持与旧版数据文件 `banillustid.db` 兼容。

## 范围

### 必做（按最小任务量拆分）

1. **屏蔽数据层**
   - 新增 SQLDelight 数据库 `BanIllustIdDatabase`，表名 `banillustid`，字段 `id`, `illust_id`, `name`。
   - 数据库文件名为 `banillustid.db`，与旧 Flutter 应用保持一致，便于数据迁移。
   - 新增 `BanRepository`，封装插入、删除、查询全部屏蔽作品方法。
   - 在 `AppDependencies` 中注册 `banDriver` 与 `banRepository`。

2. **作品详情页接入屏蔽**
   - `IllustDetailScreen` 加载作品后，查询该作品是否已被屏蔽。
   - 若已屏蔽，显示屏蔽占位页（BanPage），提供「查看」按钮临时解除本次遮挡。
   - 扩展 `IllustActionMenu` 组件，增加「屏蔽作品」菜单项。
   - 点击后调用 `BanRepository.insertBanIllust(illustId, title)`，并刷新当前页屏蔽状态。

3. **UI 反馈**
   - 屏蔽成功后通过 `ToastMessage` 提示「已屏蔽」。
   - 屏蔽失败给出错误提示。

4. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M34 code review，无 P0/P1 问题遗留。

## 技术决策

- 复用现有 `SQLDelight + DriverFactory` 机制，为屏蔽数据单独打开 `banillustid.db`，与旧版文件路径兼容。
- 复用现有 `IllustActionMenu` 底部菜单组件，新增菜单项而非新建组件。
- 屏蔽占位页使用简洁的 Column 居中展示，与旧版 `BanPage` 行为对齐：显示「作品\n标题\n」和「查看」按钮。
- 本次不接入列表页过滤（HelloScreen / SearchScreen 等），避免一次里程碑改动面过大；列表过滤放入后续里程碑。

## 验收条件

- [ ] `IllustDetailScreen` 更多菜单中出现「屏蔽作品」入口。
- [ ] 点击后作品被写入 `banillustid.db`，并立即显示屏蔽占位页。
- [ ] 占位页点击「查看」可临时显示作品内容，下次进入仍保持屏蔽。
- [ ] Android + Desktop 双端编译通过。
- [ ] M34 code review 完成，无 P0/P1 问题遗留。
