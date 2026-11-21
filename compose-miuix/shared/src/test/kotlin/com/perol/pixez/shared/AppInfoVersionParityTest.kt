package com.perol.pixez.shared

import com.perol.pixez.shared.data.repository.hasNewVersion
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 版本号镜像守护：AppInfo.VERSION_NAME 是应用内自报版本（更新检查、关于页、
 * User-Agent 的唯一来源），必须与 composeApp/build.gradle.kts 的 versionName 保持
 * 一致。漏改任一处会导致「已装最新版仍持续提示更新」（自报版本恒小于远端最新
 * Release tag）或包信息与自报版本错位。
 *
 * 回归背景：v0.9.109.6-miuix 发布时仅升级了 gradle 版本三元组，AppInfo.VERSION_NAME
 * 仍为 0.9.109.5-miuix，更新检查远端 0.9.109.6 恒大于本地自报 0.9.109.5，
 * 用户装后仍被持续提示更新。
 */
class AppInfoVersionParityTest {

    /** 从 Gradle 测试工作目录向上定位 composeApp/build.gradle.kts。 */
    private fun gradleBuildFile(): Path {
        var dir: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        repeat(8) {
            val current = dir ?: return@repeat
            for (relative in listOf(
                "compose-miuix/composeApp/build.gradle.kts",
                "composeApp/build.gradle.kts",
            )) {
                val candidate = current.resolve(relative)
                if (Files.isRegularFile(candidate)) return candidate
            }
            dir = current.parent
        }
        error("未定位到 composeApp/build.gradle.kts（working dir=" + System.getProperty("user.dir") + "）")
    }

    /** 从 build.gradle.kts 提取 defaultConfig.versionName 的字面量。 */
    private fun gradleVersionName(source: String): String {
        val regex = Regex("versionName\\s*=\\s*\"([^\"]+)\"")
        return regex.find(source)?.groupValues?.get(1)
            ?: error("build.gradle.kts 中未找到 versionName 字面量")
    }

    @Test
    fun `AppInfo 版本号必须与 gradle versionName 一致`() {
        val gradleVersion = gradleVersionName(gradleBuildFile().readText())
        assertEquals(
            gradleVersion,
            AppInfo.VERSION_NAME,
            "AppInfo.VERSION_NAME 与 composeApp versionName 不一致：更新检查以自报版本对比远端 Release，漏改一侧将导致已装最新版仍持续提示更新",
        )
    }

    @Test
    fun `自报版本与远端最新 tag 一致时不得提示更新`() {
        // 直接复刻发布事故场景：远端 tag 与 gradle/AppInfo 版本同名时 isNew 必须为 false。
        val gradleVersion = gradleVersionName(gradleBuildFile().readText())
        assertFalse(hasNewVersion(gradleVersion, AppInfo.VERSION_NAME))
        assertTrue(hasNewVersion("v0.9.999.0-miuix", AppInfo.VERSION_NAME))
    }
}
