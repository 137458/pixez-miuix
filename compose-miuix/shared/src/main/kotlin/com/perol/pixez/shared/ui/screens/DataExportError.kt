package com.perol.pixez.shared.ui.screens

/**
 * 数据导入导出的稳定错误码。
 *
 * 码值是对外契约的一部分：界面层按它映射本地化文案、日志按它定位问题，
 * 因此只允许新增成员，不要重命名或重排既有成员，也不要把码值当文案使用。
 */
internal enum class DataExportErrorCode {
    /** 用户没有填写文件路径。 */
    PathBlank,

    /** 目标文件不是 `.json`。 */
    PathUnsupportedExtension,

    /** 路径穿越到应用导出目录之外。 */
    PathOutsideExportDir,

    /** 当前平台拿不到导出根目录。 */
    BaseDirUnavailable,

    /** 待导入文件体积超过 `MAX_IMPORT_FILE_BYTES`。 */
    ImportFileTooLarge,

    /** 导入条目数超过 `MAX_IMPORT_ITEM_COUNT`。 */
    ImportItemCountExceeded,

    /** 导入字段长度超过 `MAX_IMPORT_ITEM_LENGTH`。 */
    ImportFieldTooLong,

    /** 导入字段含控制字符。 */
    ImportIllegalCharacter,

    /** 导入的 ID 或时间戳为负数。 */
    ImportNegativeValue,

    /** JSON 无法解析为目标数据结构。 */
    ImportMalformedContent,

    /** 文件读写本身失败。 */
    IoFailure,
}

/**
 * 携带 [DataExportErrorCode] 的导入导出异常。
 *
 * [detail] 只是纯技术性的英文上下文（字段名、上限值等），用于日志排查；
 * 面向用户的文案一律由界面层按 [code] 从 `AppStrings` 取，异常自身不携带任何用户可见文字。
 */
internal class DataExportException(
    val code: DataExportErrorCode,
    detail: String? = null,
    cause: Throwable? = null,
) : Exception(if (detail.isNullOrBlank()) code.name else "${code.name}($detail)", cause)
