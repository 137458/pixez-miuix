# M59 PRD：关于页信息补全

## 目标

原 Flutter 版「关于 PixEz」页面包含开发者信息、贡献者列表、项目仓库、反馈邮箱、支持/感谢等入口。当前 MIUIX 版 `AboutScreen` 仅展示应用名称与版本，信息严重不足。M59 补齐关于页核心信息，使用 MIUIX 组件重新呈现，仅做 UI 层替换，不接入应用内购买等原应用未在 MIUIX 版实现的能力。

## 范围

- 在 `AboutScreen` 中展示：
  - 应用名称、版本、简介。
  - 开发者信息（Perol_Notsfsssf、Right now）及简介。
  - 贡献者横向滚动列表（头像 + 名称 + 贡献说明）。
  - 项目仓库入口，点击打开 `https://github.com/Notsfsssf/pixez-flutter`。
  - 反馈邮箱入口，点击打开邮件客户端（`mailto:PxezFeedBack@outlook.com`）。
  - 支持/感谢入口，点击进入 `ThanksScreen` 致谢列表。
- 将 `lib/page/about/contributors.dart` 中的贡献者静态数据迁移到 Kotlin。
- 新增 `ThanksScreen` 与对应 Decompose 路由 `Config.Thanks` / `Child.Thanks`。

## 不在范围

- 应用内购买（iOS/Google Play）支持。
- 评分、检查更新、Telegram 群等需要平台能力或外部服务的入口（可后续按需补充）。

## 技术决策

- 使用 `BasicComponent` 展示可点击的信息项，与设置页风格一致。
- 贡献者头像使用已有的 `PixivAsyncImage` 加载网络图片。
- URL / 邮箱通过已有的跨平台 `openBrowser(url)` 打开；邮箱使用 `mailto:` 协议。
- `ThanksScreen` 为独立二级页，通过 `RootComponent.onThanksClicked()` 进入，保持导航模式一致。
- 所有字符串沿用原 Flutter 版中文文案，不做新增文案。

## 实现步骤

1. 创建 `compose-miuix/shared/src/commonMain/kotlin/com/perol/pixez/shared/data/model/Contributor.kt`，迁移 contributors 列表。
2. 创建 `ThanksScreen.kt`，展示致谢人员列表。
3. 在 `RootComponent.kt` 中新增 `Config.Thanks`、`Child.Thanks` 与 `onThanksClicked()`。
4. 在 `RootContent.kt` 中映射 `Child.Thanks` 到 `ThanksScreen`。
5. 重写 `AboutScreen.kt`，组装上述 UI。
6. 编译验证与 code review。

## 验收条件

- [ ] `AboutScreen` 展示开发者、贡献者、项目仓库、反馈邮箱、支持/感谢入口。
- [ ] 点击项目仓库或开发者链接可在系统浏览器中打开。
- [ ] 点击支持/感谢可进入 `ThanksScreen` 并返回。
- [ ] Android + Desktop 双端编译通过。
- [ ] M59 code review 完成，无 P0/P1 问题遗留。
