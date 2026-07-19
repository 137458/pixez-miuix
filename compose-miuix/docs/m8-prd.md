# M8 里程碑：作品详情更多菜单（复制信息 / 复制链接）

## 目标
在原 Flutter PixEz 作品详情页右上角「更多」菜单中，移植「复制信息」与「复制链接」两项基础操作到 MIUIX 页面，保持与原应用行为一致，不新增原应用不支持的功能。

## 范围

### 必做（按最小任务量拆分）

1. **跨平台剪贴板抽象**
   - 新增 expect/actual `IllustClipboard`：
     - Android：使用系统 `ClipboardManager` 复制文本。
     - Desktop：使用 `java.awt.datatransfer.StringSelection` 写入系统剪贴板。
     - iOS：使用 `UIPasteboard.general.string`。
     - macOS：使用 `NSPasteboard`。
   - commonMain 提供 `copy(text: String)` 接口，UI 层不感知平台实现。

2. **底部操作菜单组件**
   - 新增 `IllustActionMenu` 底部弹窗组件，用于展示作品相关操作项。
   - 基于 MIUIX 0.8.8 的 `SuperBottomSheet` 实现（`OverlayBottomSheet` 为更高版本 API，当前依赖不可用）。
   - 当前包含：
     - 复制信息：格式为 `title:{title}\npainter:{user.name}\nillust id:{id}`。
     - 复制链接：复制 `https://www.pixiv.net/artworks/{id}`。
   - 点击后关闭菜单，并通过 Toast 提示操作结果。

3. **作品详情页接入**
   - `IllustDetailScreen` 右上角「更多」按钮从空实现改为打开 `IllustActionMenu`。
   - 菜单所需数据从当前 `illust` 对象提取。

## 技术决策
- 复用现有 `ToastMessage` 组件提供轻量反馈。
- 复用 `BrowserLauncherContext.applicationContext` 获取 Android `Context`，与 `IllustSaver` 保持一致。
- 不引入第三方分享/剪贴板库，避免不必要的依赖。

## 验收条件
- Android + Desktop 双端编译通过。
- `IllustDetailScreen` 点击「更多」弹出底部菜单，选择「复制信息」或「复制链接」后剪贴板内容正确。
- 操作成功后显示 Toast 提示，失败时给出可读错误提示。
- 各新增功能需经过 code review 并修复 P0/P1 问题。
