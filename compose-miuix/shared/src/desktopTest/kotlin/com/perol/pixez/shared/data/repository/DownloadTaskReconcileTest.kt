package com.perol.pixez.shared.data.repository

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.DriverFactory
import com.perol.pixez.shared.data.local.task.TaskDatabase
import com.perol.pixez.shared.data.model.DownloadStatus
import com.perol.pixez.shared.data.model.DownloadTaskHistory
import com.perol.pixez.shared.reconcileInterruptedDownloadTasks
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * R-12b：进程崩溃后 task 表会残留 Downloading 行，启动对账必须把它们落到失败终态。
 *
 * 只改 Downloading：Pending 表示排队待下，Success/Failed 已是终态，对账不得触碰。
 */
class DownloadTaskReconcileTest {

    private lateinit var driver: SqlDriver
    private lateinit var historyRepo: DownloadHistoryRepository

    @Before
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        TaskDatabase.Schema.create(driver)
        historyRepo = DownloadHistoryRepository(driver)
    }

    @After
    fun tearDown() {
        DriverFactory().closeDriver(driver)
    }

    @Test
    fun `启动对账只把 Downloading 任务标记为失败`() = runBlocking {
        val pending = saveTask(illustId = 666001L, status = DownloadStatus.Pending)
        val downloading = saveTask(illustId = 666002L, status = DownloadStatus.Downloading)
        val failed = saveTask(illustId = 666003L, status = DownloadStatus.Failed)

        reconcileInterruptedDownloadTasks(driver)

        val statuses = historyRepo.getAllTasks().associate { it.id to it.status }
        assertEquals(3, statuses.size, "对账不得增删任务行")
        assertEquals(DownloadStatus.Pending, statuses[pending.id], "排队待下的任务不属于中断，必须保持 Pending")
        assertEquals(DownloadStatus.Failed, statuses[downloading.id], "崩溃后悬空的 Downloading 任务必须落到 Failed")
        assertEquals(DownloadStatus.Failed, statuses[failed.id], "已是失败终态的任务保持不变")
    }

    @Test
    fun `启动对账幂等`() = runBlocking {
        val pending = saveTask(illustId = 666011L, status = DownloadStatus.Pending)
        val downloading = saveTask(illustId = 666012L, status = DownloadStatus.Downloading)
        val failed = saveTask(illustId = 666013L, status = DownloadStatus.Failed)

        reconcileInterruptedDownloadTasks(driver)
        val afterFirst = historyRepo.getAllTasks().associate { it.id to it.status }
        reconcileInterruptedDownloadTasks(driver)
        val afterSecond = historyRepo.getAllTasks().associate { it.id to it.status }

        assertEquals(3, afterSecond.size, "重复对账不得增删任务行")
        assertEquals(afterFirst, afterSecond, "重复对账不得再改变任何任务状态")
        assertEquals(DownloadStatus.Pending, afterSecond[pending.id], "Pending 两次对账后仍应是 Pending")
        assertEquals(DownloadStatus.Failed, afterSecond[downloading.id], "Downloading 第一次对账后即落到 Failed")
        assertEquals(DownloadStatus.Failed, afterSecond[failed.id], "Failed 两次对账后仍是 Failed")
    }

    private suspend fun saveTask(illustId: Long, status: DownloadStatus): DownloadTaskHistory =
        historyRepo.saveTask(
            DownloadTaskHistory(
                id = 0,
                illustId = illustId,
                pageIndex = 0,
                title = "Task $illustId",
                userName = "ArtistA",
                remoteUrl = "https://i.pximg.net/img-original/img/2026/11/19/00/00/00/${illustId}_p0.jpg",
                fileName = "${illustId}_p0.jpg",
                status = status,
            ),
        )
}
