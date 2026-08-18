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
1. 下载下方 Assets 中的 `PixEz-MIUIX-vX.Y.Z.apk` 安装包并在设备上直接安装。

### 桌面端 (Windows / macOS / Linux)
1. 下载下方 Assets 中的 `PixEz-MIUIX-vX.Y.Z-windows-x64.jar`。
2. 确保本地安装有 Java 17 或 21 运行环境，在终端执行 `java -jar <jar-file-name>` 即可运行。

## 系统要求

- **Android**: Android 7.0 (API 24) 及以上版本。
- **桌面端**: 支持 Windows 10/11、macOS (Apple Silicon / Intel)、Linux，需配备 JRE/JDK 17 或 21。

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

`CHANGELOG.md` 在文件顶部维护最新版本区块，格式如下：

```markdown
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
