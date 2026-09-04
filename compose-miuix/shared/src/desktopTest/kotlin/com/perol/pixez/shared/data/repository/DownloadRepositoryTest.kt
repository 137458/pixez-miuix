package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.local.task.TaskDatabase
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.Illust
import com.perol.pixez.shared.data.model.ImageUrls
import com.perol.pixez.shared.data.model.IllustUser
import com.perol.pixez.shared.data.model.IllustProfileImageUrls
import com.perol.pixez.shared.data.settings.SettingsRepository
import com.perol.pixez.shared.platform.IllustSaver
import com.russhwolf.settings.PreferencesSettings
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.prefs.Preferences
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DownloadRepositoryTest {

    private lateinit var tempDir: File
    private lateinit var driver: JdbcSqliteDriver
    private lateinit var historyRepo: DownloadHistoryRepository
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var downloadRepo: DownloadRepository

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("pixez_test_dl").toFile()
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TaskDatabase.Schema.create(driver)
        historyRepo = DownloadHistoryRepository(driver)

        val node = Preferences.userRoot().node("com/perol/pixez/test/download_${System.currentTimeMillis()}")
        node.clear()
        settingsRepo = SettingsRepository(PreferencesSettings(node))
        settingsRepo.storePath = tempDir.absolutePath

        downloadRepo = DownloadRepository(
            httpClient = HttpClient(),
            saver = IllustSaver(),
            historyRepository = historyRepo,
            settingsRepository = settingsRepo,
        )
    }

    @After
    fun tearDown() {
        DriverFactory().closeDriver(driver)
        tempDir.deleteRecursively()
    }

    private fun createDummyIllust(
        id: Int = 123456,
        title: String = "Test Artwork",
        userId: Int = 7890,
        userName: String = "ArtistA",
    ): Illust {
        return Illust(
            id = id,
            title = title,
            type = "illust",
            imageUrls = ImageUrls(
                squareMedium = "https://i.pximg.net/sq.jpg",
                medium = "https://i.pximg.net/med.jpg",
                large = "https://i.pximg.net/large.jpg",
            ),
            caption = "caption",
            restrict = 0,
            user = IllustUser(
                id = userId,
                name = userName,
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
    }

    @Test
    fun `buildFileName with default format produces correct name`() {
        val illust = createDummyIllust(id = 999888, title = "Hello World")
        val fileName = downloadRepo.buildFileName(
            illust = illust,
            pageIndex = 0,
            remoteUrl = "https://i.pximg.net/img-original/img/2026/01/01/00/00/00/999888_p0.jpg",
        )
        assertEquals("999888_p0.jpg", fileName)
    }

    @Test
    fun `buildFileName with custom template replaces all placeholders`() {
        settingsRepo.format = "{user_name}_{user_id}_{title}_{illust_id}_p{part}"
        val illust = createDummyIllust(
            id = 555666,
            title = "My Masterpiece",
            userId = 1234,
            userName = "Painter",
        )
        val fileName = downloadRepo.buildFileName(
            illust = illust,
            pageIndex = 2,
            remoteUrl = "https://i.pximg.net/img-original/555666_p2.png",
        )
        assertEquals("Painter_1234_My Masterpiece_555666_p2.png", fileName)
    }

    @Test
    fun `buildFileName sanitizes illegal characters in title and author name`() {
        settingsRepo.format = "{user_name}_{title}"
        val illust = createDummyIllust(
            title = "A/B\\C:D*E?F\"G<H>I|End",
            userName = "Artist/Slash",
        )
        val fileName = downloadRepo.buildFileName(
            illust = illust,
            pageIndex = 0,
            remoteUrl = "https://i.pximg.net/sample.jpg",
        )
        assertEquals("Artist_Slash_A_B_C_D_E_F_G_H_I_End.jpg", fileName)
    }

    @Test
    fun `saveUgoiraZip saves to disk and creates history record`() = runBlocking {
        val illust = createDummyIllust(id = 888999, title = "Animated Ugoira")
        val dummyZipBytes = "PK_DUMMY_ZIP_DATA".encodeToByteArray()

        val savedPath = downloadRepo.saveUgoiraZip(
            illust = illust,
            bytes = dummyZipBytes,
            zipUrl = "https://i.pximg.net/ugoira600x600.zip",
        )

        assertTrue(File(savedPath).exists(), "Saved ZIP file must exist on disk")
        assertEquals("PK_DUMMY_ZIP_DATA", File(savedPath).readText())

        val historyTasks = historyRepo.getAllTasks()
        val recordedTask = historyTasks.firstOrNull { it.illustId == 888999 }
        assertTrue(recordedTask != null, "History must contain the ugoira task")
        assertEquals(DownloadStatus.Success, recordedTask.status)
        assertEquals("888999_ugoira.zip", recordedTask.fileName)
    }

    @Test
    fun `saveUgoiraZip with singleFolder false creates artist subfolder`() = runBlocking {
        settingsRepo.singleFolder = false
        val illust = createDummyIllust(id = 777111, title = "Subdir Ugoira", userId = 4321, userName = "SubdirArtist")
        val dummyZipBytes = "PK_SUBDIR_ZIP_DATA".encodeToByteArray()

        val savedPath = downloadRepo.saveUgoiraZip(
            illust = illust,
            bytes = dummyZipBytes,
            zipUrl = "https://i.pximg.net/sub_ugoira600x600.zip",
        )

        val file = File(savedPath)
        assertTrue(file.exists())
        assertEquals("SubdirArtist_4321", file.parentFile.name)
    }
}
