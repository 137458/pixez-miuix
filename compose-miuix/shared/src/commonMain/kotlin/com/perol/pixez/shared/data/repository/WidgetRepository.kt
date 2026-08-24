package com.perol.pixez.shared.data.repository

import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.local.account.AccountDatabase
import com.perol.pixez.shared.data.local.glanceillustpersist.GlanceIllustPersistDatabase
import com.perol.pixez.shared.data.local.glanceillustpersist.Glanceillustpersist
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.network.AuthTokenStorage
import com.perol.pixez.shared.network.PixivHttpClient
import io.github.aakira.napier.Napier

/**
 * 桌面小组件专用的数据加载与持久化仓库。
 *
 * 封装小部件在独立进程/广播接收器中的数据读取、按需网络拉取与持久化缓存。
 */
class WidgetRepository(
    private val driverFactory: DriverFactory,
    private val settingsRepository: SettingsRepository,
) {
    private val glanceDriver = driverFactory.createDriver(GlanceIllustPersistDatabase.Schema, "glance_illust_persist.db")
    private val glanceDatabase = GlanceIllustPersistDatabase(glanceDriver)

    /**
     * 获取或网络拉取指定推荐类型的插画信息。
     */
    suspend fun getOrFetchWidgetIllust(targetType: String): Glanceillustpersist? {
        val type = targetType.ifBlank { "recom" }
        val cached = try {
            glanceDatabase.glanceIllustPersistQueries.selectByType(type).executeAsList().firstOrNull()
        } catch (e: Exception) {
            null
        }
        if (cached != null) {
            return cached
        }

        val fetchedList = fetchFromRemote(type)
        if (fetchedList.isNotEmpty()) {
            val now = System.currentTimeMillis()
            try {
                glanceDatabase.glanceIllustPersistQueries.transaction {
                    fetchedList.forEach { illust ->
                        glanceDatabase.glanceIllustPersistQueries.insertOrReplace(
                            id = null,
                            illust_id = illust.id.toLong(),
                            user_id = illust.user.id.toLong(),
                            picture_url = illust.imageUrls.medium.ifBlank { illust.imageUrls.large },
                            title = illust.title,
                            user_name = illust.user.name,
                            ctype = type,
                            original_url = illust.metaSinglePage?.originalImageUrl
                                ?: illust.metaPages.firstOrNull()?.imageUrls?.original.orEmpty(),
                            large_url = illust.imageUrls.large,
                            ctime = now,
                        )
                    }
                }
            } catch (e: Exception) {
                Napier.w("Failed to save widget illusts to db: ${e.message}", tag = "WidgetRepository")
            }
            return glanceDatabase.glanceIllustPersistQueries.selectByType(type).executeAsList().firstOrNull()
        }
        return null
    }

    private suspend fun fetchFromRemote(type: String): List<Illust> {
        val accountDriver = driverFactory.createDriver(AccountDatabase.Schema, "account.db")
        val tokenStorage = AuthTokenStorage(accountDriver)
        val pixivHttpClient = PixivHttpClient(
            tokenStorage = tokenStorage,
            languageProvider = { "zh-CN" },
        )
        val illustRepo = IllustRepository(pixivHttpClient.apiClient)

        return try {
            when (type) {
                "day", "rank" -> illustRepo.getRanking(mode = "day")
                "week" -> illustRepo.getRanking(mode = "week")
                "month" -> illustRepo.getRanking(mode = "month")
                "day_male" -> illustRepo.getRanking(mode = "day_male")
                "day_female" -> illustRepo.getRanking(mode = "day_female")
                "news" -> illustRepo.getRanking(mode = "day")
                "follow" -> illustRepo.getFollowIllusts()
                else -> {
                    try {
                        illustRepo.getRecommended(forceRefresh = true)
                    } catch (_: Exception) {
                        illustRepo.getWalkthroughIllusts(forceRefresh = true)
                    }
                }
            }
        } catch (e: Exception) {
            Napier.w("Failed to fetch widget illusts for type $type: ${e.message}", tag = "WidgetRepository")
            emptyList()
        }
    }
}
