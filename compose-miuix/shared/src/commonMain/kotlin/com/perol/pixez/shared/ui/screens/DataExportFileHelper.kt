package com.perol.pixez.shared.ui.screens

import com.perol.pixez.shared.data.repository.HistoryItem
import com.perol.pixez.shared.data.repository.HistoryRepository
import com.perol.pixez.shared.data.repository.MuteData
import com.perol.pixez.shared.data.repository.MuteRepository
import com.perol.pixez.shared.data.repository.NovelHistoryItem
import com.perol.pixez.shared.data.repository.NovelHistoryRepository
import com.perol.pixez.shared.data.settings.SettingsKeys
import com.perol.pixez.shared.data.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * 将文本内容写入指定路径的导出文件。
 *
 * 该函数为平台相关函数：Android / Desktop 使用 Java [java.io.File]，
 * 其它平台在需要时再补充实现。
 *
 * 平台实现会校验 [path] 必须位于 [getExportBaseDirectory] 之下，
 * 防止 `../` 路径遍历导致写入应用私有目录之外。
 *
 * @param path 用户输入的目标文件路径，必须以 `.json` 结尾。
 * @param content 待写入的 JSON 文本。
 * @return [Result] 包装写入结果，失败时携带 [DataExportException]（平台实现只抛错误码，不抛文案）。
 */
internal expect fun writeExportFile(path: String, content: String): Result<Unit>

/**
 * 从指定路径读取导出文件的文本内容。
 *
 * 平台实现会校验 [path] 必须位于 [getExportBaseDirectory] 之下，
 * 防止 `../` 路径遍历导致读取应用私有目录之外的数据。
 *
 * @param path 用户输入的源文件路径，必须以 `.json` 结尾。
 * @return [Result] 包装读取到的文本，失败时携带 [DataExportException]。
 */
/** 导入文件大小上限：防止误放入 export 目录的超大文件在 readText 时耗尽内存。 */
internal const val MAX_IMPORT_FILE_BYTES: Long = 8L * 1024 * 1024

internal expect fun readExportFile(path: String): Result<String>

/**
 * 弹出平台原生文件选择器。
 *
 * @param suggestedName 保存时的建议文件名（导入时忽略）。
 * @param forSave true 为导出保存对话框，false 为导入打开对话框。
 * @return 用户选中的绝对路径；取消或平台未实现时返回 null（调用方回退手输路径）。
 */
internal expect suspend fun pickExportFilePath(suggestedName: String, forSave: Boolean): String?

/**
 * 返回当前平台允许导出/导入的根目录。
 *
 * Android 使用应用外部私有目录下的 `export` 子目录；
 * Desktop 使用用户主目录下的 `PixEz/export` 子目录。
 */
internal expect fun getExportBaseDirectory(): String

/**
 * 当前支持操作的数据类型列表，与原 Flutter DataExportPage 保持一致。
 */
internal enum class DataType {
    SearchTagHistory,
    BookTags,
    IllustHistory,
    NovelHistory,
    MuteData,
}

/**
 * 执行导出：根据数据类型读取对应仓库，序列化为 JSON 后写入指定路径。
 *
 * 失败一律抛出带 [DataExportErrorCode] 的 [DataExportException]，由界面层映射为本地化文案。
 */
internal suspend fun performExport(
    type: DataType,
    path: String,
    settingsRepository: SettingsRepository,
    historyRepository: HistoryRepository,
    novelHistoryRepository: NovelHistoryRepository,
    muteRepository: MuteRepository,
    json: Json,
): Result<Unit> = suspendRunCatching {
    val content = when (type) {
        DataType.SearchTagHistory -> {
            // 搜索历史以字符串列表形式保存在 Settings 中。
            val list = settingsRepository.getStringList(SettingsKeys.SEARCH_HISTORY).orEmpty()
            json.encodeToString(ListSerializer(String.serializer()), list)
        }

        DataType.BookTags -> {
            // 收藏标签同样以字符串列表形式保存。
            val list = settingsRepository.bookTagList
            json.encodeToString(ListSerializer(String.serializer()), list)
        }

        DataType.IllustHistory -> {
            // 读取插画浏览历史并序列化为与旧 Flutter 一致的 JSON 对象数组。
            val list = historyRepository.getAll()
            json.encodeToString(ListSerializer(HistoryItem.serializer()), list)
        }

        DataType.NovelHistory -> {
            // 读取小说浏览历史并序列化为与旧 Flutter 一致的 JSON 对象数组。
            val list = novelHistoryRepository.getAll()
            json.encodeToString(ListSerializer(NovelHistoryItem.serializer()), list)
        }

        DataType.MuteData -> {
            // 屏蔽数据聚合为 JSON 对象，键名与旧 Flutter MuteStore 导出保持一致。
            val data = muteRepository.getMuteData()
            json.encodeToString(MuteData.serializer(), data)
        }
    }
    writeExportFile(path, content).getOrThrow()
}

/**
 * 执行导入：从指定路径读取 JSON 后反序列化，校验通过再写回对应仓库。
 *
 * 失败一律抛出带 [DataExportErrorCode] 的 [DataExportException]，由界面层映射为本地化文案。
 */
internal suspend fun performImport(
    type: DataType,
    path: String,
    settingsRepository: SettingsRepository,
    historyRepository: HistoryRepository,
    novelHistoryRepository: NovelHistoryRepository,
    muteRepository: MuteRepository,
    json: Json,
): Result<Unit> = suspendRunCatching {
    try {
        when (type) {
            DataType.SearchTagHistory -> {
                val content = readExportFile(path).getOrThrow()
                val list = json.decodeFromString(ListSerializer(String.serializer()), content)
                val validated = validateImportedStringList(list, "searchHistory")
                settingsRepository.setStringList(SettingsKeys.SEARCH_HISTORY, validated)
            }

            DataType.BookTags -> {
                val content = readExportFile(path).getOrThrow()
                val list = json.decodeFromString(ListSerializer(String.serializer()), content)
                val validated = validateImportedStringList(list, "bookTags")
                settingsRepository.bookTagList = validated
            }

            DataType.IllustHistory -> {
                val content = readExportFile(path).getOrThrow()
                val list = json.decodeFromString(ListSerializer(HistoryItem.serializer()), content)
                val validated = validateHistoryItems(list)
                historyRepository.replaceAll(validated)
            }

            DataType.NovelHistory -> {
                val content = readExportFile(path).getOrThrow()
                val list = json.decodeFromString(ListSerializer(NovelHistoryItem.serializer()), content)
                val validated = validateNovelItems(list)
                novelHistoryRepository.replaceAll(validated)
            }

            DataType.MuteData -> {
                val content = readExportFile(path).getOrThrow()
                val data = json.decodeFromString(MuteData.serializer(), content)
                val validated = validateMuteData(data)
                muteRepository.importMuteData(validated)
            }
        }
    } catch (e: SerializationException) {
        // 序列化库的异常信息是英文技术文案且随版本漂移，统一归一为错误码后交给界面映射。
        throw DataExportException(DataExportErrorCode.ImportMalformedContent, e.message, e)
    }
}

/**
 * 协程安全版 runCatching：捕获所有异常但重新抛出 [CancellationException]，
 * 避免协程取消时被误判为导入导出失败。
 */
private suspend inline fun <T> suspendRunCatching(crossinline block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }

/**
 * 校验导入的字符串列表：条数与每条内容的长度、字符集。
 *
 * @param group 仅用于日志定位的数据分组标识，非用户可见文案。
 * @throws DataExportException 任一条目不合法时抛出对应错误码。
 */
private fun validateImportedStringList(list: List<String>, group: String): List<String> {
    checkItemCount(list.size, group)
    list.forEach { validateText(it, group) }
    return list
}

/**
 * 校验导入的插画浏览历史列表，字段长度与内容均受基础校验约束。
 */
private fun validateHistoryItems(list: List<HistoryItem>): List<HistoryItem> {
    checkItemCount(list.size, "illustHistory")
    return list.map { item ->
        // ID 与作者 ID 应为非负整数；时间戳由导出方提供，这里仅做取值范围兜底。
        checkNonNegative(item.illustId, "illustHistory.illustId")
        checkNonNegative(item.userId, "illustHistory.userId")
        checkNonNegative(item.time, "illustHistory.time")
        validateText(item.pictureUrl, "illustHistory.pictureUrl")
        item.title?.let { validateText(it, "illustHistory.title") }
        item.userName?.let { validateText(it, "illustHistory.userName") }
        item
    }
}

/**
 * 校验导入的小说浏览历史列表，字段长度与内容均受基础校验约束。
 */
private fun validateNovelItems(list: List<NovelHistoryItem>): List<NovelHistoryItem> {
    checkItemCount(list.size, "novelHistory")
    return list.map { item ->
        checkNonNegative(item.novelId.toLong(), "novelHistory.novelId")
        checkNonNegative(item.userId.toLong(), "novelHistory.userId")
        checkNonNegative(item.time, "novelHistory.time")
        validateText(item.pictureUrl, "novelHistory.pictureUrl")
        validateText(item.title, "novelHistory.title")
        validateText(item.userName, "novelHistory.userName")
        item
    }
}

/**
 * 校验导入的屏蔽数据，内部三类列表分别校验数量与字段长度。
 */
private fun validateMuteData(data: MuteData): MuteData {
    val illusts = validateMuteItems(data.illusts, "muteData.illusts") { item ->
        validateText(item.illustId, "muteData.illusts.illustId")
        validateText(item.name, "muteData.illusts.name")
    }
    val users = validateMuteItems(data.users, "muteData.users") { item ->
        validateText(item.userId, "muteData.users.userId")
        validateText(item.name, "muteData.users.name")
    }
    val tags = validateMuteItems(data.tags, "muteData.tags") { item ->
        validateText(item.name, "muteData.tags.name")
        validateText(item.translateName, "muteData.tags.translateName")
    }
    return MuteData(illusts = illusts, users = users, tags = tags)
}

/**
 * 通用屏蔽记录列表校验：先校验总数上限，再对每条记录执行 [validateItem]。
 */
private inline fun <T> validateMuteItems(
    list: List<T>,
    group: String,
    validateItem: (T) -> Unit,
): List<T> {
    checkItemCount(list.size, group)
    list.forEach { validateItem(it) }
    return list
}

/** 条数上限校验；[group] 为数据分组标识，只进日志不进文案。 */
private fun checkItemCount(size: Int, group: String) {
    if (size > MAX_IMPORT_ITEM_COUNT) {
        throw DataExportException(
            DataExportErrorCode.ImportItemCountExceeded,
            "group=$group limit=$MAX_IMPORT_ITEM_COUNT actual=$size",
        )
    }
}

/** 单个文本字段校验：长度不超过上限且不含控制字符。 */
private fun validateText(text: String, field: String) {
    if (text.length > MAX_IMPORT_ITEM_LENGTH) {
        throw DataExportException(
            DataExportErrorCode.ImportFieldTooLong,
            "field=$field limit=$MAX_IMPORT_ITEM_LENGTH actual=${text.length}",
        )
    }
    if (text.any { it.isISOControl() }) {
        throw DataExportException(DataExportErrorCode.ImportIllegalCharacter, "field=$field")
    }
}

/** ID 与时间戳的取值范围校验。 */
private fun checkNonNegative(value: Long, field: String) {
    if (value < 0L) {
        throw DataExportException(DataExportErrorCode.ImportNegativeValue, "field=$field value=$value")
    }
}

/** 导入条目数量上限。 */
private const val MAX_IMPORT_ITEM_COUNT = 10_000

/** 导入单个文本字段长度上限。 */
private const val MAX_IMPORT_ITEM_LENGTH = 1_000
