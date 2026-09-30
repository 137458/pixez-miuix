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
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Owns the primary desktop process and forwards launch arguments from later processes over loopback.
 * A file lock identifies PixEz; the loopback socket only transports bounded, line-oriented payloads.
 *
 * 端口鉴权（N-4）：主实例启动时生成一次性随机 token 写入本用户目录（跨用户不可读），
 * 次实例读取后随 PIXEZ_LAUNCH_V2 帧携带，服务端常时比较——
 * 本机其它用户会话或不知道 token 的进程无法再注入启动参数驱动导航/OAuth 交换。
 */
internal class SingleInstanceCoordinator private constructor(
    private val lockChannel: FileChannel,
    private val lock: FileLock,
    private val serverSocket: ServerSocket?,
    private val executor: ExecutorService,
    private val expectedToken: String,
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
            val payload = readPayload(reader)
            if (payload == null) {
                writer.write("INVALID\n")
            } else if (!constantTimeEquals(payload.token, expectedToken)) {
                // token 不符（未知进程注入或旧版本次实例）：拒绝且不执行任何监听器
                writer.write("FORBIDDEN\n")
            } else if (listeners.isNotEmpty()) {
                listeners.forEach { listener -> runCatching { listener(payload.arguments) } }
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
        private const val Header = "PIXEZ_LAUNCH_V2"
        private const val ForwardAttempts = 12

        fun acquireOrForward(
            arguments: List<String>,
            lockPath: File = File(System.getProperty("user.home") ?: ".", ".pixez/pixez-desktop.lock"),
            port: Int = Port,
            tokenPath: File = File(lockPath.parentFile, "pixez-desktop.token"),
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
                return if (forward(arguments, port, tokenPath)) Acquisition.ForwardedToPrimary
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
                    expectedToken = generateToken(),
                )
                return Acquisition.Primary(degraded)
            }

            // 生成一次性鉴权 token 并落盘本用户目录：次实例与主实例经文件系统交接
            val token = generateToken()
            runCatching {
                tokenPath.parentFile?.mkdirs()
                java.nio.file.Files.writeString(tokenPath.toPath(), token)
            }.getOrElse {
                lock.release()
                channel.close()
                socket.close()
                return Acquisition.Unavailable("Unable to persist the instance auth token.")
            }

            val coordinator = SingleInstanceCoordinator(
                lockChannel = channel,
                lock = lock,
                serverSocket = socket,
                executor = Executors.newSingleThreadExecutor { runnable ->
                    Thread(runnable, "pixez-launch-listener").apply { isDaemon = true }
                },
                expectedToken = token,
            )
            coordinator.startListening()
            return Acquisition.Primary(coordinator)
        }

        private fun generateToken(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        }

        /** 常时比较，避免逐字符早退泄露 token 前缀。 */
        private fun constantTimeEquals(a: String, b: String): Boolean =
            MessageDigest.isEqual(a.toByteArray(StandardCharsets.UTF_8), b.toByteArray(StandardCharsets.UTF_8))

        private fun forward(arguments: List<String>, port: Int, tokenPath: File): Boolean {
            // 无法读取鉴权 token（主实例为旧版本或文件被删）时不盲发：注入防护优先于转发成功率
            val token = runCatching {
                java.nio.file.Files.readString(tokenPath.toPath()).trim()
            }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return false
            repeat(ForwardAttempts) { attempt ->
                val forwarded = runCatching {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(InetAddress.getLoopbackAddress(), port), ConnectTimeoutMillis)
                        socket.soTimeout = SocketTimeoutMillis
                        val writer = BufferedWriter(socket.getOutputStream().writer(StandardCharsets.UTF_8))
                        writer.write(Header)
                        writer.newLine()
                        writer.write(token)
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
                        // FORBIDDEN（鉴权失败）立即放弃且不消耗重试预算
                        BufferedReader(socket.getInputStream().reader(StandardCharsets.UTF_8)).readLine() == "OK"
                    }
                }.getOrDefault(false)
                if (forwarded) return true
                if (attempt < ForwardAttempts - 1) Thread.sleep(250)
            }
            return false
        }

        private data class Payload(val token: String, val arguments: List<String>)

        private fun readPayload(reader: BufferedReader): Payload? {
            if (reader.readLine() != Header) return null
            val token = reader.readLine()?.takeIf { it.isNotEmpty() && it.length <= 128 } ?: return null
            val arguments = mutableListOf<String>()
            while (true) {
                val argument = reader.readLine() ?: return null
                if (argument.isEmpty()) return Payload(token, arguments)
                if (arguments.size >= MaxArguments || argument.length > MaxArgumentLength) return null
                arguments += argument
            }
        }
    }
}
