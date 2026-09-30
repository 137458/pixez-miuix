package com.perol.pixez.desktop

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Owns the primary desktop process and forwards launch arguments from later processes over loopback.
 * A file lock identifies PixEz; the loopback socket only transports bounded, line-oriented payloads.
 */
internal class SingleInstanceCoordinator private constructor(
    private val lockChannel: FileChannel,
    private val lock: FileLock,
    private val serverSocket: ServerSocket?,
    private val executor: ExecutorService,
) : Closeable {
    private val listeners = CopyOnWriteArrayList<(List<String>) -> Unit>()

    fun addLaunchListener(listener: (List<String>) -> Unit): Closeable {
        listeners += listener
        return Closeable { listeners -= listener }
    }

    private fun startListening() {
        val socket = serverSocket ?: return  // 降级模式（端口被占用）：不启动转发监听
        executor.execute {
            while (!socket.isClosed) {
                val client = runCatching { socket.accept() }.getOrNull() ?: continue
                runCatching { handleClient(client) }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        socket.use { client ->
            client.soTimeout = SocketTimeoutMillis
            val reader = BufferedReader(client.getInputStream().reader(StandardCharsets.UTF_8))
            val writer = BufferedWriter(client.getOutputStream().writer(StandardCharsets.UTF_8))
            val arguments = readPayload(reader)
            if (arguments == null) {
                writer.write("INVALID\n")
            } else if (listeners.isNotEmpty()) {
                listeners.forEach { listener -> runCatching { listener(arguments) } }
                writer.write("OK\n")
            } else {
                // 主实例尚未完成首帧组合（监听器未注册）：回 BUSY 让次实例感知未送达而非误以为成功
                writer.write("BUSY\n")
            }
            writer.flush()
        }
    }

    override fun close() {
        runCatching { serverSocket?.close() }
        executor.shutdownNow()
        runCatching { executor.awaitTermination(1, TimeUnit.SECONDS) }
        runCatching { lock.release() }
        runCatching { lockChannel.close() }
    }

    sealed interface Acquisition {
        data class Primary(val coordinator: SingleInstanceCoordinator) : Acquisition
        data object ForwardedToPrimary : Acquisition
        data class Unavailable(val reason: String) : Acquisition
    }

    companion object {
        private const val Port = 45_861
        private const val ConnectTimeoutMillis = 1_500
        private const val SocketTimeoutMillis = 3_000
        private const val MaxArguments = 32
        private const val MaxArgumentLength = 8_192
        private const val Header = "PIXEZ_LAUNCH_V1"
        private const val ForwardAttempts = 12

        fun acquireOrForward(
            arguments: List<String>,
            lockPath: File = File(System.getProperty("user.home") ?: ".", ".pixez/pixez-desktop.lock"),
            port: Int = Port,
        ): Acquisition {
            lockPath.parentFile?.mkdirs()
            val channel = runCatching {
                FileChannel.open(
                    lockPath.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                )
            }.getOrElse { return Acquisition.Unavailable("Unable to open the instance lock.") }
            val lock = runCatching { channel.tryLock() }.getOrNull()
            if (lock == null) {
                channel.close()
                return if (forward(arguments, port)) Acquisition.ForwardedToPrimary
                else Acquisition.Unavailable("PixEz is already running but did not accept launch arguments.")
            }

            val socket = runCatching {
                ServerSocket().apply {
                    reuseAddress = true
                    bind(InetSocketAddress(InetAddress.getLoopbackAddress(), port))
                }
            }.getOrElse {
                // 文件锁已证明本进程是唯一实例；端口被第三方占用（如 Hyper-V 保留段）时
                // 不应阻断启动——降级为“无转发 Primary”模式，仅牺牲二次启动参数转发能力。
                lock.release()
                channel.close()
                io.github.aakira.napier.Napier.w("单实例监听端口被占用，降级启动（禁用二次启动参数转发）")
                val degraded = SingleInstanceCoordinator(
                    lockChannel = channel,
                    lock = lock,
                    serverSocket = null,
                    executor = Executors.newSingleThreadExecutor { r -> Thread(r, "pixez-launch-listener").apply { isDaemon = true } },
                )
                return Acquisition.Primary(degraded)
            }
            val coordinator = SingleInstanceCoordinator(
                lockChannel = channel,
                lock = lock,
                serverSocket = socket,
                executor = Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "pixez-launch-listener").apply { isDaemon = true }
                },
            )
            coordinator.startListening()
            return Acquisition.Primary(coordinator)
        }

        private fun forward(arguments: List<String>, port: Int): Boolean {
            repeat(ForwardAttempts) { attempt ->
                val forwarded = runCatching {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), port), ConnectTimeoutMillis)
                        socket.soTimeout = SocketTimeoutMillis
                        val writer = BufferedWriter(socket.getOutputStream().writer(StandardCharsets.UTF_8))
                        writer.write(Header)
                        writer.newLine()
                        arguments.take(MaxArguments).forEach { argument ->
                            require(argument.length <= MaxArgumentLength && !argument.contains('\n') && !argument.contains('\r')) {
                                "Invalid launch argument"
                            }
                            writer.write(argument)
                            writer.newLine()
                        }
                        writer.newLine()
                        writer.flush()
                        BufferedReader(socket.getInputStream().reader(StandardCharsets.UTF_8)).readLine() == "OK"
                    }
                }.getOrDefault(false)
                if (forwarded) return true
                if (attempt < ForwardAttempts - 1) Thread.sleep(250)
            }
            return false
        }

        private fun readPayload(reader: BufferedReader): List<String>? {
            if (reader.readLine() != Header) return null
            val arguments = mutableListOf<String>()
            while (true) {
                val argument = reader.readLine() ?: return null
                if (argument.isEmpty()) return arguments
                if (arguments.size >= MaxArguments || argument.length > MaxArgumentLength) return null
                arguments += argument
            }
        }
    }
}
