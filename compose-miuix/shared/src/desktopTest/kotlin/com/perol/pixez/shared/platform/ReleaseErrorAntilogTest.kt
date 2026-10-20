package com.perol.pixez.shared.platform

import io.github.aakira.napier.LogLevel
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ReleaseErrorAntilog / ErrorLogSanitizer / ErrorLogFiles 的行为测试（架构审查 中-12）。
 *
 * 锁定隐私红线：错误日志不落 Authorization/token/完整 URL（查询串）/用户标识。
 */
class ReleaseErrorAntilogTest {

    private val tempDirs = mutableListOf<File>()

    private fun newTempDir(): File =
        File.createTempFile("pixez-logtest", null).apply {
            delete()
            mkdirs()
        }.also { tempDirs.add(it) }

    @AfterTest
    fun cleanup() {
        tempDirs.forEach { dir -> dir.deleteRecursively() }
        tempDirs.clear()
    }

    // ---------- 级别过滤：仅 W/E 级 ----------

    @Test
    fun isEnable_放行W与E级() {
        val antilog = ReleaseErrorAntilog(newTempDir())
        assertTrue(antilog.isEnable(LogLevel.WARNING, "Tag"))
        assertTrue(antilog.isEnable(LogLevel.ERROR, "Tag"))
    }

    @Test
    fun isEnable_拦截I_D_V级() {
        val antilog = ReleaseErrorAntilog(newTempDir())
        assertFalse(antilog.isEnable(LogLevel.INFO, "Tag"))
        assertFalse(antilog.isEnable(LogLevel.DEBUG, "Tag"))
        assertFalse(antilog.isEnable(LogLevel.VERBOSE, "Tag"))
    }

    @Test
    fun performLog_W级写入滚动文件() {
        val dir = newTempDir()
        ReleaseErrorAntilog(dir).log(LogLevel.WARNING, "DownloadRepo", null, "下载任务失败 id=123")
        val logFile = File(dir, "app.log")
        assertTrue(logFile.exists(), "W 级日志应落盘 app.log")
        val text = logFile.readText()
        assertTrue(text.contains("[W/DownloadRepo]"), "应包含级别与标签，实际：$text")
        assertTrue(text.contains("下载任务失败 id=123"), "应包含日志消息，实际：$text")
    }

    @Test
    fun performLog_E级写入并附异常堆栈() {
        val dir = newTempDir()
        val error = IllegalStateException("connection reset by peer")
        ReleaseErrorAntilog(dir).log(LogLevel.ERROR, "PixivHttp", error, "API 请求失败 status=503")
        val text = File(dir, "app.log").readText()
        assertTrue(text.contains("[E/PixivHttp]"), "应包含级别与标签，实际：$text")
        assertTrue(text.contains("API 请求失败 status=503"), "应包含日志消息，实际：$text")
        assertTrue(text.contains("connection reset by peer"), "应包含异常堆栈，实际：$text")
    }

    @Test
    fun performLog_I级不落盘() {
        val dir = newTempDir()
        ReleaseErrorAntilog(dir).log(LogLevel.INFO, "Tag", null, "info message")
        ReleaseErrorAntilog(dir).log(LogLevel.DEBUG, "Tag", null, "debug message")
        assertFalse(File(dir, "app.log").exists(), "I/D 级日志不应落盘")
    }

    // ---------- 隐私红线：脱敏 ----------

    @Test
    fun sanitize_Authorization头值脱敏() {
        val sanitized = ErrorLogSanitizer.sanitize("请求头校验失败 Authorization: Bearer abc123def456ghi789")
        assertNotNull(sanitized)
        assertFalse(sanitized.contains("abc123def456ghi789"), "红线：Authorization 凭证值不得落盘，实际：$sanitized")
        assertTrue(sanitized.contains("Authorization"), "字段名保留以定位问题")
    }

    @Test
    fun sanitize_token赋值脱敏_常规与边界两组数据() {
        val regular = ErrorLogSanitizer.sanitize("oauth 返回 access_token=pIXIV0123456789abcdef refresh_token=ZyXwVu9876543210")
        assertNotNull(regular)
        assertFalse(regular.contains("pIXIV0123456789abcdef"), "红线：access_token 值不得落盘，实际：$regular")
        assertFalse(regular.contains("ZyXwVu9876543210"), "红线：refresh_token 值不得落盘，实际：$regular")

        val boundary = ErrorLogSanitizer.sanitize("回调链接 https://example.com/callback#access_token=ShortTok123&token_type=bearer")
        assertNotNull(boundary)
        assertFalse(boundary.contains("ShortTok123"), "红线：fragment 形式的 access_token 值不得落盘，实际：$boundary")
    }

    @Test
    fun sanitize_URL查询串剥离_保留接口路径() {
        val sanitized = ErrorLogSanitizer.sanitize(
            "Pixiv API 请求失败 status=403, url=https://app-api.pixiv.net/v1/user/detail?user_id=12345678, body=neterror",
        )
        assertNotNull(sanitized)
        assertFalse(sanitized.contains("user_id=12345678"), "红线：查询串中的用户标识不得落盘，实际：$sanitized")
        assertFalse(sanitized.contains("?"), "红线：完整 URL（含查询串）不得落盘，实际：$sanitized")
        assertTrue(sanitized.contains("v1/user/detail"), "接口路径保留以保留排障价值，实际：$sanitized")
    }

    @Test
    fun sanitize_常规错误文本不误伤() {
        val plain = "下载失败: Connection reset；url=https://i.pximg.net/img/2023/01/01/00/00/00/123456_p0.jpg"
        assertEquals(plain, ErrorLogSanitizer.sanitize(plain), "常规错误文本不应被误改写")
        assertNull(ErrorLogSanitizer.sanitize(null))
    }

    @Test
    fun performLog_落盘内容经脱敏_红线端到端() {
        val dir = newTempDir()
        val antilog = ReleaseErrorAntilog(dir)
        antilog.log(
            LogLevel.ERROR,
            "PixivHttp",
            null,
            "Pixiv API 请求失败 status=403, url=https://app-api.pixiv.net/v1/illust/detail?illust_id=98765&user_id=12345678",
        )
        val text = File(dir, "app.log").readText()
        assertFalse(text.contains("user_id=12345678"), "红线：用户标识不得落盘，实际：$text")
        assertFalse(text.contains("illust_id=98765"), "红线：完整 URL 查询串不得落盘，实际：$text")
        assertTrue(text.contains("status=403"), "状态码等非敏感信息保留，实际：$text")
    }

    // ---------- 滚动文件：5MB×3（测试以缩小上限验证同一逻辑） ----------

    @Test
    fun performLog_目录不存在时自动创建() {
        val dir = File(newTempDir(), "nested/logs")
        ReleaseErrorAntilog(dir).log(LogLevel.ERROR, "Tag", null, "首次写入")
        val logFile = File(dir, "app.log")
        assertTrue(logFile.exists(), "目录不存在时应自动创建并落盘")
        assertTrue(logFile.readText().contains("首次写入"))
    }

    @Test
    fun performLog_超限滚动且文件数不超上限() {
        val dir = newTempDir()
        val antilog = ReleaseErrorAntilog(dir, maxBytes = 200L, maxFiles = 3)
        repeat(20) { index ->
            antilog.log(LogLevel.ERROR, "Roll", null, "第 $index 条：0123456789012345678901234567890123456789")
        }
        val logFiles = dir.listFiles { file -> file.name.startsWith("app") }.orEmpty()
        assertTrue(logFiles.size in 1..3, "滚动文件数应在 1..3 内，实际 ${logFiles.map { it.name }}")
        assertTrue(File(dir, "app.log").exists(), "app.log 应始终存在")
        assertEquals(3, ReleaseErrorAntilog.MAX_FILES)
    }

    @Test
    fun performLog_滚动后最新内容保留在applog() {
        val dir = newTempDir()
        val antilog = ReleaseErrorAntilog(dir, maxBytes = 200L, maxFiles = 3)
        repeat(10) { index ->
            antilog.log(LogLevel.WARNING, "Roll", null, "第 $index 条：0123456789012345678901234567890123456789")
        }
        val text = File(dir, "app.log").readText()
        assertTrue(text.contains("第 9 条"), "最新日志应保留在当前 app.log，实际：$text")
    }

    @Test
    fun performLog_日志目录不可用时静默降级且无落盘产物() {
        // 目录路径被既有文件占用：mkdirs 与写盘必然失败，模拟目录不可写（磁盘满/权限拒绝同类）。
        val dir = newTempDir()
        val blocker = File(dir, "blocker.txt").apply { writeText("keep-me") }
        val blockedDirectory = File(blocker, "logs")
        ReleaseErrorAntilog(blockedDirectory).log(LogLevel.ERROR, "Tag", null, "目录不可用也不应抛出、不应落盘")
        // 执行到此处即验证 log() 未向主流程抛出；以下断言落盘降级的具体业务状态：
        assertEquals(
            listOf("blocker.txt"),
            dir.listFiles()?.map { it.name }?.sorted(),
            "目录不可用时应无任何落盘产物，实际：${dir.listFiles()?.map { it.name }}",
        )
        assertEquals("keep-me", blocker.readText(), "既有文件不得被日志写入破坏")
        assertFalse(File(blockedDirectory, "app.log").exists(), "不可用目录下不得产出 app.log")
    }

    // ---------- 崩溃兜底落盘 ----------

    @Test
    fun writeCrashLog_落盘线程名与堆栈() {
        val dir = newTempDir()
        val crash = ErrorLogFiles.writeCrashLog(dir, "main", IllegalStateException("boom at com.perol.pixez.PixEzApp"))
        val file = assertNotNull(crash, "崩溃日志应成功落盘")
        assertTrue(file.name.startsWith("crash-"), "崩溃文件名应为 crash- 前缀：${file.name}")
        val text = file.readText()
        assertTrue(text.contains("Thread: main"), "应包含线程名，实际：$text")
        assertTrue(text.contains("boom at com.perol.pixez.PixEzApp"), "应包含异常堆栈，实际：$text")
    }

    @Test
    fun writeCrashLog_崩溃堆栈同样过红线脱敏() {
        val dir = newTempDir()
        val crash = ErrorLogFiles.writeCrashLog(
            dir,
            "DefaultDispatcher",
            IllegalStateException("token 失效 access_token=LeakyTokenValue123456"),
        )
        val text = assertNotNull(crash).readText()
        assertFalse(text.contains("LeakyTokenValue123456"), "红线：崩溃日志不得包含 token 值，实际：$text")
    }

    // ---------- 崩溃日志保留策略：仅保留最新 3 份（与错误日志滚动一致） ----------

    @Test
    fun writeCrashLog_超出3份时删除最旧仅保留最新3份() {
        val dir = newTempDir()
        val now = System.currentTimeMillis()
        // 预置 3 份更早的崩溃日志；时间戳互异且与当前时刻相差足够大，保证文件名不冲突。
        val oldest = File(dir, "crash-${now - 3000}.log").apply { writeText("Thread: main\noldest-crash") }
        val middle = File(dir, "crash-${now - 2000}.log").apply { writeText("Thread: main\nmiddle-crash") }
        val newer = File(dir, "crash-${now - 1000}.log").apply { writeText("Thread: main\nnewer-crash") }
        // 第 4 份经公共入口写入，时间戳取当前时刻，新于全部预置文件。
        val newest = assertNotNull(ErrorLogFiles.writeCrashLog(dir, "main", IllegalStateException("newest-crash")))
        val remaining = dir.listFiles { file -> file.name.startsWith("crash-") }.orEmpty().map { it.name }.sorted()
        assertEquals(
            listOf(middle.name, newer.name, newest.name),
            remaining,
            "crash 日志应仅保留最新 3 份，实际：$remaining",
        )
        assertFalse(oldest.exists(), "超出份数的最旧崩溃日志应被删除：${oldest.name}")
        assertTrue(newest.readText().contains("newest-crash"), "保留的最新日志内容应对应本次崩溃，实际：${newest.readText()}")
        assertTrue(middle.readText().contains("middle-crash"), "未超限的旧日志内容不得被改写")
    }

    @Test
    fun writeCrashLog_恰好3份时全部保留且不误删无关文件() {
        val dir = newTempDir()
        val keeper = File(dir, "unrelated.txt").apply { writeText("keep-me") }
        val now = System.currentTimeMillis()
        val first = File(dir, "crash-${now - 2000}.log").apply { writeText("Thread: main\nfirst-crash") }
        val second = File(dir, "crash-${now - 1000}.log").apply { writeText("Thread: main\nsecond-crash") }
        val third = assertNotNull(ErrorLogFiles.writeCrashLog(dir, "main", IllegalStateException("third-crash")))
        val remaining = dir.listFiles { file -> file.name.startsWith("crash-") }.orEmpty().map { it.name }.sorted()
        assertEquals(
            listOf(first.name, second.name, third.name),
            remaining,
            "恰好 3 份时应全部保留，实际：$remaining",
        )
        assertEquals("keep-me", keeper.readText(), "清理不得误删非 crash 日志文件")
        assertTrue(first.readText().contains("first-crash"), "既有崩溃日志内容不得被改写")
    }
}
