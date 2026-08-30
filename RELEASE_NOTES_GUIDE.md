# 更新日志与发行版编写规则 (Release Notes Guide)

本文档规定 PixEz MIUIX 项目中 `CHANGELOG.md` 和 GitHub Release 更新日志的统一写法。

---

## 1. 两类更新日志的定位

- **`CHANGELOG.md`**：保存每个版本的完整变更记录，面向项目维护和长期历史追溯。
- **GitHub Release 更新日志**：面向最终用户，使用规范清晰的语言说明新增了什么功能、修复了什么问题，并附上安装方法、系统要求、致谢与开源许可。

GitHub Release 更新日志应基于 `CHANGELOG.md` 提炼编写，不应直接堆砌 Git 提交标题或开发者内部变量名。

---

## 2. GitHub Release 固定结构

每个正式版 Release 统一按以下结构组织：

```markdown
## 新增

- 支持了……
- 添加了……

## 修复

- 修复了……

## 优化

- 优化了……

## 安装方法

### Android
1. 下载下方 Assets 中的 `PixEz-MIUIX-vX.Y.Z.apk`（或 `android-debug-apk`）安装包并在设备上直接安装。

### Windows 桌面端 (无需预装 Java 环境)
- **单文件独立版 (推荐)**：下载 `PixEz-Standalone.exe`，内嵌完整运行环境与渲染库，双击即可直接运行，无需解压。
- **免安装绿色便携版**：下载 `PixEz-windows-x64-portable.zip`，解压后双击目录内的 `PixEz.exe` 即可运行。
- **MSI 安装向导包**：下载 `PixEz-*.msi` 安装包，双击按提示安装至系统，自动创建桌面与开始菜单快捷方式。

## 系统要求

- **Android**: Android 7.0 (API 24) 及以上版本。
- **Windows**: Windows 10 / 11 64 位系统（已内嵌专属优化 JRE 运行环境，开箱即用零依赖）。
- **macOS / Linux**: macOS 12+ / Ubuntu 20.04+。

## 致谢

- [compose-miuix-ui/miuix](https://github.com/compose-miuix-ui/miuix) — Xiaomi HyperOS / MIUIX 跨平台组件库。
- [PixEz-Flutter](https://github.com/Notsfsssf/pixez-flutter) — PixEz 原版设计与业务逻辑参考。

## 许可

本项目遵循 [GPL-3.0 License](LICENSE) 开源协议。
```

> **注意**：没有相关内容的分类（如某版本仅为缺陷修复而无新增功能时）不保留空标题，不为了凑章节把修复写成新增。

---

## 3. 用语与格式规范

- **用户视角**：使用“支持了”“添加了”“修复了”“优化了”等普通用户能直观理解的词汇。
- **去除内部细节**：只写用户可感知的功能与体验结果，严禁暴露底层函数名、类名、提交哈希或仅供调试的内部术语。
- **严禁表情符号**：正文与标题均严格禁止使用 Emoji 图标。
- **合并多余过程**：同类问题或多次迭代修复合并为一条最终结果，不记录试错与回滚过程。
- **真实准确**：只记录已在双端通过构建验证和功能测试的特性。

---

## 4. CHANGELOG.md 写法

`CHANGELOG.md` 在文件顶部维护尚未发布的最新变更与历史已发布版本区块。

> **核心原则**：已打包发布的版本区块（如 `## [v0.9.108]`）一经发布即冻结，不可再往其中追加新的改动；尚未正式打包发布的新增与修复项，必须统一独立归档于顶部的 `## [未发布]` 区块中。

格式如下：

```markdown
## [未发布]

### 修复

- 修复了……

---

## [v0.9.108] - 2026-08-18

### 新增

- 首页、关注动态、搜索、排行榜、画师主页、系列作品、评论区与好友列表全量支持流式无限滚动。
- 所有列表页支持 MIUIX 官方下拉刷新与顶部手动刷新。

### 优化

- 悬浮胶囊底栏在大屏设备上限宽居中，消除全屏拉伸失真。
- 瀑布流在大屏和桌面宽屏上自适应多列布局。

### 修复

- 修复长评作品无法加载第 1 页之后历史评论的问题。
- 修复关注与粉丝较多时列表触底无法翻页的问题。
- 修复连载系列作品超出 30 话无法查看完整章节的问题。
```

---

## 5. 标准全自动发布流程 (Automated Release Workflow)

本项目已接入 GitHub Actions 全自动打包与 Release 发布工作流，发布新版本的标准操作步骤如下：

### 步骤 1：确认版本号
在 `compose-miuix/composeApp/build.gradle.kts` 中确认或升级版本信息：
- `versionCode`（例如 `10010054`）
- `versionName`（例如 `"0.9.109-miuix"`）
- `packageVersion`（例如 `"0.9.109"`）

### 步骤 2：归档更新日志
在 `CHANGELOG.md` 中：
1. 将当前 `## [未发布]` 区块下的条目归档为新版本区块（例如 `## [v0.9.109] - 2026-08-30`）；
2. 在顶部重新开辟一个空的 `## [未发布]` 区块供后续迭代使用。

### 步骤 3：本地编译与打包验证
在提交前运行本地多平台验证指令：
```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:packageWindowsSingleFileExe
```

### 步骤 4：Git 提交与打标签
```bash
git add .
git commit -m "chore(release): release v0.9.109"
git tag v0.9.109
```

### 步骤 5：推送代码与标签触发自动发布
```bash
git push origin master --tags
```

### 步骤 6：CI 全自动构建与 GitHub Release 挂载
- GitHub Actions 检测到 `v*` 标签推送后，将自动并行执行：
  - 构建 Android Debug APK；
  - 构建 Windows 单文件自包含 EXE（`PixEz-Standalone.exe`）；
  - 构建 Windows 免安装绿色便携 ZIP（`PixEz-windows-x64-portable.zip`）；
  - 构建 Windows MSI 安装包（`PixEz-*.msi`）；
- 构建完成后，Actions 会**全自动创建 GitHub Release**，生成 Release Notes，并将上述全套多端产物自动挂载至 Release 附件中供用户下载。

