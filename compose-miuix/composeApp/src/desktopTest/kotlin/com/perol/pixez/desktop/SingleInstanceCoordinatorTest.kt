package com.perol.pixez.desktop

import org.junit.Test
import java.io.File
import java.net.ServerSocket
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SingleInstanceCoordinatorTest {
    @Test
    fun `secondary instance forwards arguments to the primary instance`() {
        val temporaryDirectory = Files.createTempDirectory("pixez-single-instance-test").toFile()
        val lockFile = File(temporaryDirectory, "instance.lock")
        val port = ServerSocket(0).use { it.localPort }
        val primary = assertIs<SingleInstanceCoordinator.Acquisition.Primary>(
            SingleInstanceCoordinator.acquireOrForward(emptyList(), lockFile, port),
        )
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
            )
            assertEquals(SingleInstanceCoordinator.Acquisition.ForwardedToPrimary, secondary)
            assertEquals(true, received.await(3, TimeUnit.SECONDS))
            assertEquals(listOf("pixiv://account/login?code=test"), forwardedArguments)
        }
        primary.coordinator.close()
        temporaryDirectory.deleteRecursively()
    }
}
