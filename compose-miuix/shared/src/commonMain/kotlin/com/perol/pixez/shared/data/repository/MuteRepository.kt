package com.perol.pixez.shared.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 屏蔽数据仓库：聚合 [BanRepository] 中的作品、画师与标签屏蔽记录，
 * 为数据导入导出提供统一入口。
 *
 * 导出格式沿用旧 Flutter `MuteStore` 的 JSON 对象结构，
 * 顶层键为 `banillustid`、`banuserid`、`bantag`。
 */
class MuteRepository(
    private val banRepository: BanRepository,
) {
    /**
     * 读取当前全部屏蔽数据。
     */
    suspend fun getMuteData(): MuteData = withContext(Dispatchers.IO) {
        val illusts = banRepository.getAllBanIllusts().map {
            MuteIllust(id = it.id, illustId = it.illustId, name = it.name)
        }
        val users = banRepository.getAllBanUsers().map {
            MuteUser(id = it.id, userId = it.userId, name = it.name)
        }
        val tags = banRepository.getAllBanTags().map {
            MuteTag(id = it.id, name = it.name, translateName = it.translateName)
        }
        MuteData(illusts = illusts, users = users, tags = tags)
    }

    /**
     * 导入屏蔽数据，分别写入对应数据库表。
     *
     * 导入前会先清空旧记录，避免重复与冲突。
     */
    suspend fun importMuteData(data: MuteData) = withContext(Dispatchers.IO) {
        banRepository.clearAllBanIllusts()
        banRepository.insertAllBanIllusts(
            data.illusts.map { BanRepository.BanIllust(it.id ?: 0L, it.illustId, it.name) },
        )
        banRepository.clearAllBanUsers()
        banRepository.insertAllBanUsers(
            data.users.map { BanRepository.BanUser(it.id ?: 0L, it.userId, it.name) },
        )
        banRepository.clearAllBanTags()
        banRepository.insertAllBanTags(
            data.tags.map { BanRepository.BanTag(it.id ?: 0L, it.name, it.translateName) },
        )
    }
}

/**
 * 屏蔽数据导出模型，顶层结构与旧 Flutter `MuteStore.export` 一致。
 */
@Serializable
data class MuteData(
    @SerialName("banillustid") val illusts: List<MuteIllust>,
    @SerialName("banuserid") val users: List<MuteUser>,
    @SerialName("bantag") val tags: List<MuteTag>,
)

/**
 * 屏蔽作品导出项，字段名与旧 Flutter `BanIllustIdPersist` 保持一致。
 */
@Serializable
data class MuteIllust(
    @SerialName("id") val id: Long? = null,
    @SerialName("illust_id") val illustId: String,
    @SerialName("name") val name: String,
)

/**
 * 屏蔽画师导出项，字段名与旧 Flutter `BanUserIdPersist` 保持一致。
 */
@Serializable
data class MuteUser(
    @SerialName("id") val id: Long? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
)

/**
 * 屏蔽标签导出项，字段名与旧 Flutter `BanTagPersist` 保持一致。
 */
@Serializable
data class MuteTag(
    @SerialName("id") val id: Long? = null,
    @SerialName("name") val name: String,
    @SerialName("translate_name") val translateName: String,
)
