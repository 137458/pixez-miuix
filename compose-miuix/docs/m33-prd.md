# M33 里程碑：设置页清除缓存

## 状态
进行中。

## 目标
原 Flutter PixEz 在设置页提供「清除缓存」入口，用于释放 Coil 图片磁盘/内存缓存。M33 将在 MIUIX 版设置页复刻该功能，并保持实现简洁、可跨平台运行。

## 范围

### 必做（按最小任务量拆分）

1. **清除缓存能力**
   - 在 `SettingsScreen` 的「下载」或「存储」分组下新增「清除缓存」菜单项。
   - 通过 Coil 3 的 `SingletonImageLoader.get(context)` 获取当前 `ImageLoader`。
   - 调用 `memoryCache?.clear()` 与 `diskCache?.clear()` 清理图片缓存。
   - 清理完成后通过 `ToastMessage` 提示用户。

2. **UI 状态反馈**
   - 点击后进入短暂加载态，防止用户重复点击。
   - 成功或失败均给出中文提示。

3. **编译验证与 code review**
   - Android + Desktop 双端编译通过。
   - 执行 M33 code review，无 P0/P1 问题遗留。

## 技术决策

- 不引入 expect/actual：Coil 3 在 commonMain 已暴露 `SingletonImageLoader` 与 `ImageLoader`，可直接跨平台清理缓存。
- 复用现有 `ToastMessage` 组件做操作反馈，不新增弹窗组件。
- 清理操作在 `rememberCoroutineScope` 中异步执行，避免阻塞 UI。

## 验收条件

- [ ] `SettingsScreen` 显示「清除缓存」入口。
- [ ] 点击后成功清除 Coil 内存与磁盘缓存，并弹出提示。
- [ ] 清理过程中按钮不可重复点击。
- [ ] Android + Desktop 双端编译通过。
- [ ] M33 code review 完成，无 P0/P1 问题遗留。
