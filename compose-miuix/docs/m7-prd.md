# M7 里程碑：作品下载任务管理

## 目标
实现插画作品下载功能，支持单张/多页作品保存到本地，与原 Flutter PixEz 的保存行为保持一致，不新增原应用不支持的功能。

## 范围

### 必做（按最小任务量拆分）

1. **下载任务仓库与平台保存抽象（已完成）**
   - 新增 `DownloadTask` 数据模型：任务 ID、作品 ID、页码、远程 URL、文件名、状态、错误信息。
   - 新增 `DownloadRepository`：提供 `download(illust, pageIndex)` 接口，负责下载图片字节并调用平台保存。
   - 新增 expect/actual `IllustSaver`：跨平台保存图片字节到本地，Android 保存到 Pictures/PixEz，Desktop 保存到用户图片目录，iOS/macOS 先提供占位实现。
   - `IllustDetailScreen` 添加「下载」按钮，先支持下载当前展示的单页作品。

2. **多页作品下载（已完成）**
   - `DownloadRepository` 新增 `downloadAllPages(illust, onProgress)`，按页码顺序逐页下载并支持进度回调。
   - `IllustDetailScreen` 信息区在 `pageCount > 1` 时显示「下载全部页」入口。
   - 单页作品仍通过 TopAppBar「下载」按钮下载当前页。

3. **下载反馈与错误处理（当前切片）**
   - 下载中/成功/失败状态反馈（暂定使用 Snackbar 或 Toast 风格的轻量提示）。
   - 失败时支持重试。

4. **平台实现补齐**
   - Android：使用 `MediaStore` 保存到公共 Pictures 目录，自动刷新图库。
   - Desktop：保存到用户主目录下的 `Pictures/PixEz`。
   - iOS/macOS：保存到应用沙盒或相册（后续补充）。

## 技术决策
- 下载使用独立的 Ktor `HttpClient` 或复用现有 `apiClient` 发起 GET 请求获取图片字节。
- 图片 URL 使用 `meta_single_page.original_image_url`（单页）或 `meta_pages[index].image_urls.original`（多页）。
- 文件名格式与原应用保持一致：`{title}_p{index}.{ext}` 或 `{user.name}/{title}_p{index}.{ext}`，M7 切片 1 先采用 `{title}_p{index}.{ext}`。
- 平台保存通过 expect/actual 封装，commonMain 不感知具体文件路径。

## 验收条件
- Android + Desktop 双端编译通过。
- `IllustDetailScreen` 点击下载按钮后，图片成功保存到平台对应目录。
- 下载失败时给出可读错误提示。
- 各新增功能需经过 code review 并修复 P0/P1 问题。
