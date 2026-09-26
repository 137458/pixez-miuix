package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.banillustid.BanIllustIdDatabase
import com.perol.pixez.shared.data.local.bantag.BanTagDatabase
import com.perol.pixez.shared.data.local.banuserid.BanUserIdDatabase
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MuteRepositoryTest {

    private lateinit var illustDriver: JdbcSqliteDriver
    private lateinit var userDriver: JdbcSqliteDriver
    private lateinit var tagDriver: JdbcSqliteDriver
    private lateinit var banRepository: BanRepository
    private lateinit var repository: MuteRepository

    @Before
    fun setUp() {
        illustDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        userDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        tagDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        BanIllustIdDatabase.Schema.create(illustDriver)
        BanUserIdDatabase.Schema.create(userDriver)
        BanTagDatabase.Schema.create(tagDriver)

        banRepository = BanRepository(illustDriver, userDriver, tagDriver)
        repository = MuteRepository(banRepository)
    }

    @After
    fun tearDown() {
        illustDriver.close()
        userDriver.close()
        tagDriver.close()
    }

    @Test
    fun `getMuteData maps all three categories with fields intact`() = runBlocking {
        banRepository.insertBanIllust(illustId = 12345, name = "作品标题")
        banRepository.insertBanUser(userId = 6789, name = "画师名")
        banRepository.insertBanTag(name = "タグ", translateName = "标签")

        val data = repository.getMuteData()

        assertEquals(1, data.illusts.size)
        assertEquals("12345", data.illusts[0].illustId)
        assertEquals("作品标题", data.illusts[0].name)

        assertEquals(1, data.users.size)
        assertEquals("6789", data.users[0].userId)
        assertEquals("画师名", data.users[0].name)

        assertEquals(1, data.tags.size)
        assertEquals("タグ", data.tags[0].name)
        assertEquals("标签", data.tags[0].translateName)
    }

    @Test
    fun `importMuteData replaces existing data per table`() = runBlocking {
        // 预置旧数据
        banRepository.insertBanIllust(illustId = 1, name = "旧作品")
        banRepository.insertBanUser(userId = 2, name = "旧画师")
        banRepository.insertBanTag(name = "旧标签", translateName = "old")

        repository.importMuteData(
            MuteData(
                illusts = listOf(MuteIllust(id = null, illustId = "999", name = "新作品")),
                users = listOf(MuteUser(id = null, userId = "888", name = "新画师")),
                tags = listOf(MuteTag(id = null, name = "新标签", translateName = "new")),
            ),
        )

        val data = repository.getMuteData()
        assertEquals(listOf("999"), data.illusts.map { it.illustId })
        assertEquals(listOf("888"), data.users.map { it.userId })
        assertEquals(listOf("新标签"), data.tags.map { it.name })
        assertEquals(listOf("new"), data.tags.map { it.translateName })
    }

    @Test
    fun `importMuteData with empty lists clears all tables`() = runBlocking {
        banRepository.insertBanIllust(illustId = 1, name = "a")
        banRepository.insertBanUser(userId = 2, name = "b")
        banRepository.insertBanTag(name = "c", translateName = "d")

        repository.importMuteData(MuteData(illusts = emptyList(), users = emptyList(), tags = emptyList()))

        val data = repository.getMuteData()
        assertTrue(data.illusts.isEmpty())
        assertTrue(data.users.isEmpty())
        assertTrue(data.tags.isEmpty())
    }

    @Test
    fun `import then export round-trips values ignoring regenerated ids`() = runBlocking {
        val imported = MuteData(
            illusts = listOf(
                MuteIllust(id = null, illustId = "11", name = "n1"),
                MuteIllust(id = null, illustId = "22", name = "n2"),
            ),
            users = listOf(MuteUser(id = null, userId = "33", name = "u1")),
            tags = listOf(MuteTag(id = null, name = "t1", translateName = "tt1")),
        )
        repository.importMuteData(imported)

        val exported = repository.getMuteData()
        assertEquals(
            imported.illusts.map { it.illustId to it.name }.sortedBy { it.first },
            exported.illusts.map { it.illustId to it.name }.sortedBy { it.first },
        )
        assertEquals(
            imported.users.map { it.userId to it.name },
            exported.users.map { it.userId to it.name },
        )
        assertEquals(
            imported.tags.map { it.name to it.translateName },
            exported.tags.map { it.name to it.translateName },
        )
    }

    @Test
    fun `mute data json keeps legacy flutter store keys`() {
        val json = Json { ignoreUnknownKeys = true }
        val data = MuteData(
            illusts = listOf(MuteIllust(id = 1L, illustId = "100", name = "作品")),
            users = listOf(MuteUser(id = null, userId = "200", name = "画师")),
            tags = listOf(MuteTag(id = null, name = "tag", translateName = "标签")),
        )

        val encoded = json.encodeToString(MuteData.serializer(), data)
        assertTrue(encoded.contains("\"banillustid\""))
        assertTrue(encoded.contains("\"banuserid\""))
        assertTrue(encoded.contains("\"bantag\""))
        assertTrue(encoded.contains("\"illust_id\""))
        assertTrue(encoded.contains("\"user_id\""))
        assertTrue(encoded.contains("\"translate_name\""))

        // 旧 Flutter 导出的 payload 必须能反序列化
        val legacy = """{"banillustid":[{"id":7,"illust_id":"42","name":"x"}],"banuserid":[],"bantag":[]}"""
        val decoded = json.decodeFromString(MuteData.serializer(), legacy)
        assertEquals("42", decoded.illusts[0].illustId)
        assertEquals(7L, decoded.illusts[0].id)
    }
}
