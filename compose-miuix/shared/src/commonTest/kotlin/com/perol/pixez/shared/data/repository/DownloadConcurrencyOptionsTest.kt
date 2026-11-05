package com.perol.pixez.shared.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 下载并发上限单源（架构审查 R5）：档位列表与运行时夹取必须由同一常量派生，
 * 避免设置页选项、仓库夹取边界与常量表三方再次漂移。
 */
class DownloadConcurrencyOptionsTest {

    @Test
    fun `并发档位由单一上限派生且连续`() {
        assertEquals(
            listOf(1, 2, 3, 4, 5, 6),
            DownloadRepository.PARALLEL_TASK_OPTIONS,
            "档位应从 MAX_PARALLEL_TASKS 连续派生",
        )
        assertEquals(DownloadRepository.MAX_PARALLEL_TASKS, DownloadRepository.PARALLEL_TASK_OPTIONS.last())
        assertEquals(DownloadRepository.MAX_PARALLEL_TASKS, DownloadRepository.PARALLEL_TASK_OPTIONS.max())
    }

    @Test
    fun `并发设置值夹取到合法区间`() {
        assertEquals(1, DownloadRepository.coerceParallelTasks(0), "下界外应夹到 1")
        assertEquals(1, DownloadRepository.coerceParallelTasks(-3), "负数应夹到 1")
        assertEquals(3, DownloadRepository.coerceParallelTasks(3), "区间内原样返回")
        assertEquals(
            DownloadRepository.MAX_PARALLEL_TASKS,
            DownloadRepository.coerceParallelTasks(DownloadRepository.MAX_PARALLEL_TASKS),
            "上界本身合法",
        )
        assertEquals(
            DownloadRepository.MAX_PARALLEL_TASKS,
            DownloadRepository.coerceParallelTasks(99),
            "上界外应夹到 MAX_PARALLEL_TASKS",
        )
    }

    @Test
    fun `档位列表每项都在合法区间内`() {
        DownloadRepository.PARALLEL_TASK_OPTIONS.forEach { option ->
            assertEquals(option, DownloadRepository.coerceParallelTasks(option), "档位 $option 应在区间内")
        }
    }
}
