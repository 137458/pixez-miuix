package com.perol.pixez.shared.platform

/**
 * 平台原生对话框的缺省文案。
 *
 * platform 层不得反向依赖 ui 的本地化资源（分层单向，架构审查 DN15）：本地化标题由调用方
 * 经参数传入；未传时用这里的中性缺省值兜底，而不是在 actual 内读 LocalStrings / AppStrings。
 */
object PlatformDialogDefaults {
    /** 目录选择对话框缺省标题（调用方应传 L10n 文案覆盖）。 */
    const val PICK_DIRECTORY_TITLE = "Save location"

    /** 图片选择对话框缺省标题（调用方应传 L10n 文案覆盖）。 */
    const val PICK_PHOTO_TITLE = "SauceNAO"
}
