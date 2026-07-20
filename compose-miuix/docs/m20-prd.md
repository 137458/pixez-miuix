# M20 里程碑：原生系统分享

## 状态
进行中。

## 目标
原 Flutter PixEz 的作品详情与用户详情操作菜单中，支持将作品/用户链接通过系统分享面板发送给其他应用。M20 将为 MIUIX 版补充跨平台原生分享能力，使 Android/iOS 调用系统分享，Desktop 回退到剪贴板并提示用户。

## 范围

### 必做（按最小任务量拆分）

1. **跨平台分享抽象**
   - 新增 expect/actual `IllustShare`：
     - `share(text: String, subject: String? = null)` 接口。
   - 各平台实现：
     - Android：通过 `BrowserLauncherContext` 获取 Context，使用 `Intent.ACTION_SEND` 启动系统分享面板。
     - Desktop：回退到 `IllustClipboard.copy(text)`，并通过 Toast 提示“链接已复制到剪贴板”。
     - iOS：使用 `UIActivityViewController` 展示系统分享面板。
     - macOS：使用 `NSSharingServicePicker` 展示系统分享面板。

2. **菜单接入**
   - `IllustActionMenu` 增加「分享链接」选项，点击后调用 `IllustShare.share(buildIllustShareLink(it), it.title)`。
   - `UserActionMenu` 增加「分享链接」选项，点击后调用 `IllustShare.share(buildUserShareLink(user), user.name)`。

3. **结果反馈**
   - Android/iOS/macOS 分享面板本身提供交互反馈，无需额外 Toast。
   - Desktop 回退复制后通过现有 `ToastMessage` 提示成功。

## 验收条件

- [ ] `IllustShare` expect/actual 抽象与 4 平台实现完成。
- [ ] 作品详情菜单增加「分享链接」并调用原生分享。
- [ ] 用户详情菜单增加「分享链接」并调用原生分享。
- [ ] Android + Desktop 双端编译通过。
- [ ] M20 code review 完成，无 P0/P1 问题遗留。
