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
 * @return [Result] 包装写入结果，失败时携带异常信息。
 */
internal expect fun writeExportFile(path: String, content: String): Result<Unit>

/**
 * 从指定路径读取导出文件的文本内容。
 *
 * 平台实现会校验 [path] 必须位于 [getExportBaseDirectory] 之下，
 * 防止 `../` 路径遍历导致读取应用私有目录之外的数据。
 *
 * @param path 用户输入的源文件路径，必须以 `.json` 结尾。
 * @return [Result] 包装读取到的文本，失败时携带异常信息。
 */
internal expect fun readExportFile(path: String): Result<String>

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
internal enum class DataType(
    val title: String,
    val summary: String,
) {
    SearchTagHistory("搜索标签历史", "导出/导入历史搜索记录"),
    BookTags("收藏标签", "导出/导入常用的收藏标签"),
    IllustHistory("插画历史", "导出/导入插画浏览历史"),
    NovelHistory("小说历史", "导出/导入小说浏览历史"),
    MuteData("屏蔽数据", "导出/导入屏蔽的标签、画师与作品"),
}

/**
 * 执行导出：根据数据类型读取对应仓库，序列化为 JSON 后写入指定路径。
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
    when (type) {
        DataType.SearchTagHistory -> {
            val content = readExportFile(path).getOrThrow()
            val list = json.decodeFromString(ListSerializer(String.serializer()), content)
            val validated = validateImportedStringList(list)
            settingsRepository.setStringList(SettingsKeys.SEARCH_HISTORY, validated)
        }

        DataType.BookTags -> {
            val content = readExportFile(path).getOrThrow()
            val list = json.decodeFromString(ListSerializer(String.serializer()), content)
            val validated = validateImportedStringList(list)
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
 * 校验导入的字符串列表，防止过长或包含非法控制字符的数据损坏设置。
 *
 * @return 校验通过后的列表。
 * @throws IllegalArgumentException 任一条目不合法时抛出。
 */
private fun validateImportedStringList(list: List<String>): List<String> {
    require(list.size <= MAX_IMPORT_ITEM_COUNT) { "条目数量超过上限 $MAX_IMPORT_ITEM_COUNT" }
    return list.map { item ->
        validateText(item)
        item
    }
}

/**
 * 校验导入的插画浏览历史列表，字段长度与内容均受基础校验约束。
 */
private fun validateHistoryItems(list: List<HistoryItem>): List<HistoryItem> {
    require(list.size <= MAX_IMPORT_ITEM_COUNT) { "条目数量超过上限 $MAX_IMPORT_ITEM_COUNT" }
    return list.map { item ->
        // ID 与作者 ID 应为非负整数；时间戳由导出方提供，这里仅做取值范围兜底。
        require(item.illustId >= 0) { "插画 ID 不能为负数" }
        require(item.userId >= 0) { "画师 ID 不能为负数" }
        require(item.time >= 0) { "时间戳不能为负数" }
        validateText(item.pictureUrl, "图片链接")
        item.title?.let { validateText(it, "标题") }
        item.userName?.let { validateText(it, "画师名") }
        item
    }
}

/**
 * 校验导入的小说浏览历史列表，字段长度与内容均受基础校验约束。
 */
private fun validateNovelItems(list: List<NovelHistoryItem>): List<NovelHistoryItem> {
    require(list.size <= MAX_IMPORT_ITEM_COUNT) { "条目数量超过上限 $MAX_IMPORT_ITEM_COUNT" }
    return list.map { item ->
        require(item.novelId >= 0) { "小说 ID 不能为负数" }
        require(item.userId >= 0) { "画师 ID 不能为负数" }
        require(item.time >= 0) { "时间戳不能为负数" }
        validateText(item.pictureUrl, "图片链接")
        validateText(item.title, "标题")
        validateText(item.userName, "画师名")
        item
    }
}

/**
 * 校验导入的屏蔽数据，内部三类列表分别校验数量与字段长度。
 */
private fun validateMuteData(data: MuteData): MuteData {
    val illusts = validateMuteItems(data.illusts, "屏蔽作品") { item ->
        validateText(item.illustId, "作品 ID")
        validateText(item.name, "作品标题")
    }
    val users = validateMuteItems(data.users, "屏蔽画师") { item ->
        validateText(item.userId, "画师 ID")
        validateText(item.name, "画师名")
    }
    val tags = validateMuteItems(data.tags, "屏蔽标签") { item ->
        validateText(item.name, "标签名")
        validateText(item.translateName, "标签翻译名")
    }
    return MuteData(illusts = illusts, users = users, tags = tags)
}

/**
 * 通用屏蔽记录列表校验：先校验总数上限，再对每条记录执行 [validateItem]。
 */
private inline fun <T> validateMuteItems(
    list: List<T>,
    label: String,
    validateItem: (T) -> Unit,
): List<T> {
    require(list.size <= MAX_IMPORT_ITEM_COUNT) { "$label 数量超过上限 $MAX_IMPORT_ITEM_COUNT" }
    list.forEach { validateItem(it) }
    return list
}

/**
 * 校验单个文本字段：长度不超过上限且不含控制字符。
 */
private fun validateText(text: String, label: String = "内容") {
    require(text.length <= MAX_IMPORT_ITEM_LENGTH) { "$label 长度超过上限 $MAX_IMPORT_ITEM_LENGTH" }
    require(text.none { it.isISOControl() }) { "$label 包含非法控制字符" }
}

private const val MAX_IMPORT_ITEM_COUNT = 10_000
private const val MAX_IMPORT_ITEM_LENGTH = 1_000
