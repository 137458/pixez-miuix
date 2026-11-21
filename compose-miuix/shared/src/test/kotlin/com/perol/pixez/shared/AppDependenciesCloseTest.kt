package com.perol.pixez.shared

import com.perol.pixez.shared.testutil.TestSettingsProvider
import com.perol.pixez.shared.testutil.TestSqlDriverFactory
import io.github.aakira.napier.Napier
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * AppDependencies.close() 对静态 currentInstance 的清理行为测试。
 *
 * 架构审查高-1 第 1 步：close() 只关资源不清理单例引用时，
 * 进程存活期间残留的 orNull() 调用会拿到资源已全部关闭的悬空容器。
 */
class AppDependenciesCloseTest {

    private lateinit var tempRoot: File

    @Before
    fun setup() {
        tempRoot = Files.createTempDirectory("pixez-appdependencies-close-test-").toFile().also { it.deleteOnExit() }
    }

    @After
    fun tearDown() {
        Napier.takeLogarithm()
        tempRoot.deleteRecursively()
    }

    @Test
    fun `close clears current instance so orNull returns null`() {
        val dependencies = AppDependencies(TestSqlDriverFactory(tempRoot), TestSettingsProvider())
        assertSame(dependencies, AppDependencies.orNull())

        dependencies.close()

        assertNull(AppDependencies.orNull())
    }

    @Test
    fun `closing stale instance does not clear newer current instance`() {
        val stale = AppDependencies(TestSqlDriverFactory(tempRoot), TestSettingsProvider())
        val current = AppDependencies(TestSqlDriverFactory(tempRoot), TestSettingsProvider())
        assertSame(current, AppDependencies.orNull())

        stale.close()

        assertSame(current, AppDependencies.orNull())

        current.close()

        assertNull(AppDependencies.orNull())
    }
}
