package com.perol.pixez.desktop

import org.junit.Test
import java.io.File
import java.net.ServerSocket
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertIs

class SingleInstanceCoordinatorTest {
    @Test
    fun `secondary instance forwards arguments to the primary instance`() {
        val temporaryDirectory = Files.createTempDirectory("pixez-single-instance-test").toFile()
        val lockFile = File(temporaryDirectory, "instance.lock")
        val tokenFile = File(temporaryDirectory, "pixez-desktop.token")
        val port = ServerSocket(0).use { it.localPort }
        val primary = assertIs<SingleInstanceCoordinator.Acquisition.Primary>(
            SingleInstanceCoordinator.acquireOrForward(emptyList(), lockFile, port, tokenFile),
        )
        // 主实例启动后 token 文件应已落盘供次实例鉴权交接
        assertTrue(tokenFile.isFile && tokenFile.readText().isNotBlank())

        val received = CountDownLatch(1)
        var forwardedArguments: List<String>? = null
        primary.coordinator.addLaunchListener { arguments ->
            forwardedArguments = arguments
            received.countDown()
        }.use {
            val secondary = SingleInstanceCoordinator.acquireOrForward(
                listOf("pixiv://account/login?code=test"),
                lockFile,
                port,
                tokenFile,
            )
            assertEquals(SingleInstanceCoordinator.Acquisition.ForwardedToPrimary, secondary)
            assertEquals(true, received.await(3, TimeUnit.SECONDS))
            assertEquals(listOf("pixiv://account/login?code=test"), forwardedArguments)
        }
        primary.coordinator.close()
        temporaryDirectory.deleteRecursively()
    }

    @Test
    fun `secondary instance without valid token is rejected`() {
        val temporaryDirectory = Files.createTempDirectory("pixez-single-instance-test").toFile()
        val lockFile = File(temporaryDirectory, "instance.lock")
        val port = ServerSocket(0).use { it.localPort }
        val primary = assertIs<SingleInstanceCoordinator.Acquisition.Primary>(
            SingleInstanceCoordinator.acquireOrForward(emptyList(), lockFile, port),
        )
        var listenerInvoked = false
        primary.coordinator.addLaunchListener { listenerInvoked = true }.use {
            // 覆写 token 文件为伪造值：鉴权失败的转发不得执行监听器
            tokenFileFor(lockFile).writeText("forged-token")
            val secondary = SingleInstanceCoordinator.acquireOrForward(
                listOf("pixez://illust/1"),
                lockFile,
                port,
                tokenFileFor(lockFile),
            )
            assertEquals(
                SingleInstanceCoordinator.Acquisition.Unavailable::class,
                secondary::class,
                "伪造 token 的转发应被 FORBIDDEN 拒绝",
            )
            Thread.sleep(300)
            assertEquals(false, listenerInvoked, "鉴权失败不得触发任何启动监听器")
        }
        primary.coordinator.close()
        temporaryDirectory.deleteRecursively()
    }

    private fun tokenFileFor(lockFile: File) = File(lockFile.parentFile, "pixez-desktop.token")
}
