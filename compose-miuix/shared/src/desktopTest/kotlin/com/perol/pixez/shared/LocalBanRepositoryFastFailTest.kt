package com.perol.pixez.shared

import androidx.compose.runtime.Applier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.perol.pixez.shared.data.local.banillustid.BanIllustIdDatabase
import com.perol.pixez.shared.data.local.bantag.BanTagDatabase
import com.perol.pixez.shared.data.local.banuserid.BanUserIdDatabase
import com.perol.pixez.shared.data.repository.BanRepository
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 中-20：LocalBanRepository 快速失败语义回归测试。
 *
 * IllustCard 屏蔽操作曾静默回退 AppDependencies.orNull()?.banRepository 全局单例，
 * 掩盖「CompositionLocal 未提供」的装配错误。收敛后与 LocalHistoryRepository 一致：
 * 未提供时读取 [LocalBanRepository.current] 必须立即抛出 [IllegalStateException]，
 * 提供后必须精确返回所提供的仓库实例（注入单通道，无回退分支）。
 */
class LocalBanRepositoryFastFailTest {

    private lateinit var illustDriver: JdbcSqliteDriver
    private lateinit var userDriver: JdbcSqliteDriver
    private lateinit var tagDriver: JdbcSqliteDriver
    private lateinit var repository: BanRepository

    @Before
    fun setUp() {
        illustDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        userDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        tagDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BanIllustIdDatabase.Schema.create(illustDriver)
        BanUserIdDatabase.Schema.create(userDriver)
        BanTagDatabase.Schema.create(tagDriver)
        repository = BanRepository(illustDriver, userDriver, tagDriver)
    }

    @After
    fun tearDown() {
        illustDriver.close()
        userDriver.close()
        tagDriver.close()
    }

    /**
     * 内容不产出节点的空 Applier：本测试只消费 CompositionLocal，无节点操作。
     */
    private class NoOpApplier : Applier<Unit> {
        override val current: Unit = Unit
        override fun down(node: Unit) {}
        override fun up() {}
        override fun insertTopDown(index: Int, instance: Unit) {}
        override fun insertBottomUp(index: Int, instance: Unit) {}
        override fun remove(index: Int, count: Int) {}
        override fun move(from: Int, to: Int, count: Int) {}
        override fun clear() {}
    }

    /**
     * 以最小组合根执行一次 [content]，返回 (成功值, 失败原因)。
     * 初始组合在 [Composition.setContent] 内同步执行，读取抛出的异常在此处捕获；
     * 组合体内不做 try/catch（Compose 禁止组合期捕获可组合调用）。
     */
    private fun <T> readInComposition(content: @Composable () -> T): Pair<T?, Throwable?> {
        val dispatcher = Executors.newSingleThreadExecutor { Thread(it, "local-ban-compose-test") }
            .asCoroutineDispatcher()
        try {
            return runBlocking {
                val clock = BroadcastFrameClock()
                val recomposer = Recomposer(dispatcher + clock)
                val composition = Composition(NoOpApplier(), recomposer)
                val valueRef = AtomicReference<Any?>(null)
                val failureRef = AtomicReference<Throwable?>(null)
                val doneRef = AtomicBoolean(false)
                val recomposeJob = launch(dispatcher + clock) { recomposer.runRecomposeAndApplyChanges() }
                try {
                    composition.setContent {
                        valueRef.compareAndSet(null, content())
                        doneRef.set(true)
                    }
                } catch (t: Throwable) {
                    // 初始组合同步执行，快速失败异常在此处被断言方捕获
                    failureRef.compareAndSet(null, t)
                }
                try {
                    withTimeout(10_000L) {
                        while (!doneRef.get() && failureRef.get() == null) {
                            clock.sendFrame(0L)
                            yield()
                        }
                    }
                } finally {
                    try {
                        composition.dispose()
                    } catch (_: Throwable) {
                        // 拆解失败不代表被测语义：结果已在 valueRef/failureRef 捕获
                    }
                    try {
                        // Recomposer.cancel 会将重组失败原因再抛出，该异常已由 failureRef 记录
                        recomposer.cancel()
                    } catch (_: Throwable) {
                    }
                    recomposeJob.cancel()
                }
                @Suppress("UNCHECKED_CAST")
                valueRef.get() as T? to failureRef.get()
            }
        } finally {
            dispatcher.close()
        }
    }

    @Test
    fun `LocalBanRepository 未提供时读取 current 快速失败`() {
        val (_, failure) = readInComposition { LocalBanRepository.current }
        assertNotNull(failure, "LocalBanRepository 未提供时应快速失败，而不是静默返回默认值")
        assertTrue(
            failure is IllegalStateException,
            "快速失败异常应为 IllegalStateException，实际为 ${failure::class.simpleName}: ${failure.message}",
        )
        assertContains(
            failure.message ?: "",
            "LocalBanRepository",
            message = "错误信息应指明缺失的注入点，便于定位装配错误，实际为: ${failure.message}",
        )
    }

    @Test
    fun `LocalBanRepository 提供后返回所提供的仓库实例`() {
        // CompositionLocalProvider 返回 Unit，读取结果经引用带出组合。
        val read = AtomicReference<BanRepository?>(null)
        val (_, failure) = readInComposition {
            CompositionLocalProvider(LocalBanRepository provides repository) {
                read.set(LocalBanRepository.current)
            }
        }
        assertNull(failure, "提供后读取不应失败: ${failure?.message}")
        assertSame(repository, read.get(), "提供后 LocalBanRepository.current 应精确返回所提供的实例")
    }
}
