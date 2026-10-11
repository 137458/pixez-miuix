package com.perol.pixez.shared.ui.components

import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.repository.DownloadRepository
import com.perol.pixez.shared.data.repository.IllustRepository
import com.perol.pixez.shared.data.repository.InMemorySettings
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.IllustSaver
import com.perol.pixez.shared.platform.getAppCacheDirectory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * saveUgoiraIllust 保存链路测试（架构审查 中-15）：
 * 缓存命中时复用会话缓存 zip 且不得删除缓存文件（播放会话仍需复用）；
 * 缓存未命中时走流式下载保存，且保存临时文件不残留。
 */
class UgoiraSessionSaveTest {

    private fun createDummyIllust(id: Long): Illust = Illust(
        id = id,
        title = "Ugoira Save Test",
        type = "illust",
        imageUrls = ImageUrls(
            squareMedium = "https://i.pximg.net/sq.jpg",
            medium = "https://i.pximg.net/med.jpg",
            large = "https://i.pximg.net/large.jpg",
        ),
        caption = "caption",
        restrict = 0,
        user = IllustUser(
            id = 42L,
            name = "ArtistA",
            account = "artist_a",
            profileImageUrls = IllustProfileImageUrls(""),
            isFollowed = false,
        ),
        tags = emptyList(),
        tools = emptyList(),
        createDate = "2026-01-01",
        pageCount = 1,
        width = 1000,
        height = 1000,
        sanityLevel = 2,
        xRestrict = 0,
        series = null,
        metaSinglePage = null,
        metaPages = emptyList(),
        totalView = 100,
        totalBookmarks = 50,
        isBookmarked = false,
        visible = true,
        isMuted = false,
        illustAIType = 1,
        illustBookStyle = 0,
    )

    private fun tempWorkDir(): Path =
        FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "pixez_ugoira_save_${Random.nextLong()}"

    private fun downloadRepository(storePath: Path): DownloadRepository {
        val settings = SettingsRepository(InMemorySettings())
        settings.storePath = storePath.toString()
        return DownloadRepository(
            saver = IllustSaver(),
            settingsRepository = settings,
        )
    }

    private fun apiClientReturning(body: String): HttpClient = HttpClient(MockEngine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    isLenient = true
                },
            )
        }
        engine {
            addHandler {
                respond(body, HttpStatusCode.OK, headersOf("Content-Type", ContentType.Application.Json.toString()))
            }
        }
    }

    private fun downloadClientReturning(bytes: ByteArray): HttpClient = HttpClient(MockEngine) {
        engine {
            addHandler {
                respond(bytes, HttpStatusCode.OK, headersOf("Content-Type", ContentType.Application.OctetStream.toString()))
            }
        }
    }

    @Test
    fun `saveUgoiraIllust 缓存命中时保存缓存 zip 内容且保留缓存文件`() = runBlocking {
        val illust = createDummyIllust(id = 888001L)
        val workDir = tempWorkDir()
        FileSystem.SYSTEM.createDirectory(workDir, mustCreate = true)
        val cacheZipPath = workDir / "cached.zip"
        FileSystem.SYSTEM.write(cacheZipPath) { writeUtf8("PK_CACHED_ZIP") }
        val framesDirPath = workDir / "frames"
        UgoiraSessionCache.put(
            illust.id,
            UgoiraReadySession(
                provider = UgoiraFrameProvider(emptyList(), framesDirPath),
                tempZipPath = cacheZipPath,
                framesDir = framesDirPath,
                zipUrl = "https://i.pximg.net/img-zip-ugoira/v1/888001.zip",
            ),
        )
        val repo = downloadRepository(workDir)
        try {
            val result = saveUgoiraIllust(
                illust = illust,
                illustRepository = IllustRepository(
                    apiClient = HttpClient(),
                    downloadClient = HttpClient(),
                    settingsRepository = SettingsRepository(InMemorySettings()),
                ),
                downloadRepository = repo,
            )

            val savedPath = result.getOrThrow()
            assertEquals(
                "PK_CACHED_ZIP",
                FileSystem.SYSTEM.read(savedPath.toPath()) { readUtf8() },
                "保存内容应来自会话缓存 zip",
            )
            assertTrue(FileSystem.SYSTEM.exists(cacheZipPath), "会话缓存 zip 必须保留，供播放与后续保存复用")
        } finally {
            UgoiraSessionCache.clear()
            runCatching { FileSystem.SYSTEM.deleteRecursively(workDir) }
        }
    }

    @Test
    fun `saveUgoiraIllust 缓存未命中时下载 zip 保存且清理保存临时文件`() = runBlocking {
        val illust = createDummyIllust(id = 888002L)
        val workDir = tempWorkDir()
        FileSystem.SYSTEM.createDirectory(workDir, mustCreate = true)
        val zipBytes = "PK_DOWNLOADED_ZIP".encodeToByteArray()
        val apiClient = apiClientReturning(
            """{"ugoira_metadata":{"zip_urls":{"medium":"https://i.pximg.net/img-zip-ugoira/v1/888002.zip"},""" +
                """"frames":[{"file":"000000.jpg","delay":80}]}}""",
        )
        val downloadClient = downloadClientReturning(zipBytes)
        val repo = downloadRepository(workDir)
        try {
            val result = saveUgoiraIllust(
                illust = illust,
                illustRepository = IllustRepository(
                    apiClient = apiClient,
                    downloadClient = downloadClient,
                    settingsRepository = SettingsRepository(InMemorySettings()),
                ),
                downloadRepository = repo,
            )

            val savedPath = result.getOrThrow()
            assertEquals(
                "PK_DOWNLOADED_ZIP",
                FileSystem.SYSTEM.read(savedPath.toPath()) { readUtf8() },
                "保存内容应为下载的 zip 字节",
            )
            val leftover = FileSystem.SYSTEM.listOrNull(getAppCacheDirectory())
                ?.filter { it.name.startsWith("ugoira_save_888002_") }
                .orEmpty()
            assertTrue(leftover.isEmpty(), "保存临时文件不应残留: $leftover")
        } finally {
            UgoiraSessionCache.clear()
            runCatching { FileSystem.SYSTEM.deleteRecursively(workDir) }
            apiClient.close()
            downloadClient.close()
        }
    }
}
