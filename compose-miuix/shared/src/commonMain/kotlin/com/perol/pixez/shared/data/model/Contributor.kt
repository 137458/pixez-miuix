package com.perol.pixez.shared.data.model

/**
 * 贡献者信息。
 *
 * 数据来自原 Flutter 版 lib/page/about/contributors.dart，
 * 仅保留头像、名称、主页与贡献说明，不迁移彩蛋点击逻辑。
 *
 * @param name 贡献者名称。
 * @param avatar 头像 URL。
 * @param url 个人主页或 GitHub 链接。
 * @param content 贡献说明。
 */
data class Contributor(
    val name: String,
    val avatar: String,
    val url: String,
    val content: String,
)

/**
 * PixEz 项目贡献者列表。
 */
val CONTRIBUTORS: List<Contributor> = listOf(
    Contributor(
        name = "Tragic Life",
        avatar = "https://avatars.githubusercontent.com/u/16817202?v=4",
        url = "https://github.com/TragicLifeHu",
        content = "🌍",
    ),
    Contributor(
        name = "Skimige",
        avatar = "https://avatars.githubusercontent.com/u/9017470?v=4",
        url = "https://xyx.moe/",
        content = "📖",
    ),
    Contributor(
        name = "Xian",
        avatar = "https://avatars.githubusercontent.com/u/34748039?v=4",
        url = "https://github.com/itzXian",
        content = "🌍",
    ),
    Contributor(
        name = "karin722",
        avatar = "https://avatars.githubusercontent.com/u/54385201?v=4",
        url = "http://ivtune.net/",
        content = "🌍",
    ),
    Contributor(
        name = "Romani-Archman",
        avatar = "https://avatars.githubusercontent.com/u/68731023?v=4",
        url = "http://archman.fun/",
        content = "📖",
    ),
    Contributor(
        name = "Henry-ZHR",
        avatar = "https://avatars.githubusercontent.com/u/51886614?v=4",
        url = "https://github.com/Henry-ZHR",
        content = "💻",
    ),
    Contributor(
        name = "Takase",
        avatar = "https://avatars.githubusercontent.com/u/20792268?v=4",
        url = "https://github.com/takase1121",
        content = "🌍",
    ),
    Contributor(
        name = "ChsBuffer",
        avatar = "https://avatars.githubusercontent.com/u/33744752?v=4",
        url = "https://github.com/chsbuffer",
        content = "💻",
    ),
    Contributor(
        name = "媛天徵",
        avatar = "https://avatars.githubusercontent.com/u/64569368?v=4",
        url = "https://github.com/YooTynChi",
        content = "🌍",
    ),
    Contributor(
        name = "Scighost",
        avatar = "https://avatars.githubusercontent.com/u/61003590?v=4",
        url = "https://github.com/Scighost",
        content = "💻",
    ),
    Contributor(
        name = "sheason2019",
        avatar = "https://avatars.githubusercontent.com/u/73812146?v=4",
        url = "https://github.com/sheason2019",
        content = "💻",
    ),
    Contributor(
        name = "frg2089",
        avatar = "https://avatars.githubusercontent.com/u/42184238?v=4",
        url = "https://github.com/frg2089",
        content = "💻🪟",
    ),
)
