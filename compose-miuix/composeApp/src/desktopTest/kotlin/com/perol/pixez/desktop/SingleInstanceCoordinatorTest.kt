package com.perol.pixez.desktop

import org.junit.Test
import java.io.File
import java.net.InetAddress
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

    @Test
    fun `监听端口被占用时降级为主实例并保留文件锁`() {
        val temporaryDirectory = Files.createTempDirectory("pixez-single-instance-degraded-test").toFile()
        val lockFile = File(temporaryDirectory, "instance.lock")
        val tokenFile = File(temporaryDirectory, "pixez-desktop.token")
        // 第三方进程先占用回环端口：主实例只能降级（放弃监听，保留文件锁）
        val blocker = ServerSocket(0, 50, InetAddress.getLoopbackAddress())
        try {
            val occupiedPort = blocker.localPort
            val primary = assertIs<SingleInstanceCoordinator.Acquisition.Primary>(
                SingleInstanceCoordinator.acquireOrForward(emptyList(), lockFile, occupiedPort, tokenFile),
            )

            // 唯一性不变量：降级只放弃监听端口，不得把文件锁让出去
            val second = SingleInstanceCoordinator.acquireOrForward(
                listOf("pixez://illust/1"),
                lockFile,
                occupiedPort,
                tokenFile,
            )
            assertEquals(
                SingleInstanceCoordinator.Acquisition.Unavailable::class,
                second::class,
                "降级主实例仍须持锁，第二个进程不得再成为 Primary（实际: " + second + "）",
            )

            // 关闭后锁释放：端口空闲时应能再次成为主实例
            primary.coordinator.close()
            val freePort = ServerSocket(0).use { it.localPort }
            val recovered = assertIs<SingleInstanceCoordinator.Acquisition.Primary>(
                SingleInstanceCoordinator.acquireOrForward(emptyList(), lockFile, freePort, tokenFile),
            )
            recovered.coordinator.close()
        } finally {
            blocker.close()
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun tokenFileFor(lockFile: File) = File(lockFile.parentFile, "pixez-desktop.token")
}
