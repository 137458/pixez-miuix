package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.task.TaskDatabase
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.DownloadTaskHistory
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 下载任务历史仓储测试：验证保存/查询往返、状态过滤、按时间倒序与查询上限。
 */
class DownloadHistoryRepositoryTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var repository: DownloadHistoryRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TaskDatabase.Schema.create(driver)
        repository = DownloadHistoryRepository(driver)
    }

    @After
    fun tearDown() {
        driver.close()
    }

    private fun task(id: Long, url: String, status: DownloadStatus = DownloadStatus.Success, marker: Int = 0) = DownloadTaskHistory(
        id = id,
        illustId = 100 + marker,
        pageIndex = 0,
        title = "title-$marker",
        userName = "user-$marker",
        remoteUrl = url,
        fileName = "file-$marker.jpg",
        status = status,
        sanityLevel = 2,
        userId = 900,
        medium = "m",
    )

    @Test
    fun `保存新任务分配自增 id 且往返字段完整`() = runBlocking {
        val saved = repository.saveTask(task(0, "https://pximg/1.jpg", marker = 7))

        assertTrue(saved.id > 0, "新任务应分配自增 id")
        val loaded = repository.getAllTasks().single()
        assertEquals("title-7", loaded.title)
        assertEquals(107, loaded.illustId)
        assertEquals("https://pximg/1.jpg", loaded.remoteUrl)
        assertEquals("m", loaded.medium)
        assertEquals(DownloadStatus.Success, loaded.status)
    }

    @Test
    fun `按已有 id 保存覆盖原行`() = runBlocking {
        val first = repository.saveTask(task(0, "u1"))
        repository.saveTask(first.copy(status = DownloadStatus.Failed))

        val loaded = repository.getAllTasks()
        assertEquals(1, loaded.size)
        assertEquals(DownloadStatus.Failed, loaded.single().status)
    }

    @Test
    fun `getAllTasks 按时间倒序且 limit 上限生效`() = runBlocking {
        for (i in 1..5) {
            repository.saveTask(task(0, "u$i", marker = i))
        }

        val limited = repository.getAllTasks(limit = 3)

        assertEquals(3, limited.size, "limit 应截断返回行数")
        assertEquals(listOf("title-5", "title-4", "title-3"), limited.map { it.title }, "limit 应保留最新写入的记录")
    }

    @Test
    fun `getTasksByStatus 仅返回对应状态且受 limit 约束`() = runBlocking {
        repository.saveTask(task(0, "u1", DownloadStatus.Downloading, marker = 1))
        repository.saveTask(task(0, "u2", DownloadStatus.Success, marker = 2))
        repository.saveTask(task(0, "u3", DownloadStatus.Downloading, marker = 3))
        repository.saveTask(task(0, "u4", DownloadStatus.Failed, marker = 4))

        val running = repository.getTasksByStatus(DownloadStatus.Downloading)

        assertEquals(2, running.size)
        assertTrue(running.all { it.status == DownloadStatus.Downloading })

        val limitedRunning = repository.getTasksByStatus(DownloadStatus.Downloading, limit = 1)
        assertEquals(1, limitedRunning.size)
    }

    @Test
    fun `deleteTask 删除指定行且 clearAll 清空`() = runBlocking {
        val first = repository.saveTask(task(0, "u1"))
        repository.saveTask(task(0, "u2"))

        repository.deleteTask(first.id)
        assertEquals(1, repository.getAllTasks().size)

        repository.clearAll()
        assertTrue(repository.getAllTasks().isEmpty())
    }
}
