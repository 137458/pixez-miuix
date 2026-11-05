import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.File

/**
 * 定位可用于 jpackage 的 JDK：先认显式注入（-PpackageJavaHome / PACKAGE_JAVA_HOME），
 * 再按各平台常规安装目录扫描（Windows 的 Program Files、Eclipse Adoptium、用户 .jdks；
 * macOS 的 JavaVirtualMachines；Linux 的 /usr/lib/jvm），只采纳带 bin/jpackage 的发行版。
 * 不写任何机器私有路径：换机器后只要能找到任一常规安装的 JDK 即可打包。
 */
fun resolveJpackageJdk(project: Project): File? {
    val isWindows = OperatingSystem.current().isWindows
    val jpackageRelative = if (isWindows) "bin/jpackage.exe" else "bin/jpackage"
    fun usable(dir: File) = dir.isDirectory && dir.resolve(jpackageRelative).exists()

    val injected = listOfNotNull(
        project.providers.gradleProperty("packageJavaHome").orNull,
        System.getenv("PACKAGE_JAVA_HOME"),
        System.getenv("JAVA_HOME"),
        System.getProperty("java.home"),
    ).map { project.file(it) }.filter(::usable)
    if (injected.isNotEmpty()) return injected.first()

    val roots = if (isWindows) {
        listOfNotNull(
            System.getenv("ProgramFiles"),
            System.getenv("ProgramW6432"),
            System.getenv("LOCALAPPDATA")?.let { "$it\\Programs" },
            System.getProperty("user.home")?.let { "$it\\.jdks" },
        )
    } else {
        listOfNotNull(
            "/Library/Java/JavaVirtualMachines",
            "/usr/lib/jvm",
        )
    }
    return roots.map { project.file(it) }.filter { it.isDirectory }
        .flatMap { root ->
            val children = root.listFiles()?.toList().orEmpty()
            val grandchildren = children.flatMap { child -> child.listFiles()?.toList().orEmpty() }
            children + grandchildren + root
        }
        .firstOrNull(::usable)
}

// 发布签名配置：keystore.properties（已 gitignore）提供 storeFile/storePassword/keyAlias/keyPassword。
// 缺失时回退 debug 签名，保证本地与 CI 构建（assembleDebug）始终可用；正式发版须先配置该文件。
val releaseSigningProps = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use { load(it) } }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

// 回退分支不静默：缺配置时明确告警，并给 release 产物打上 debug-signed 版本标注（见下方 androidComponents）。
val releaseSigningConfigured = releaseSigningProps != null
if (!releaseSigningConfigured) {
    logger.warn(
        "release 构建回退到 debug 签名：未找到 compose-miuix/keystore.properties。" +
            "该 release 产物不可用于正式发版，版本名会追加 -debug-signed 后缀以便追溯。",
    )
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting

        commonMain.dependencies {
            // Compose 1.12.0 使用 compose.* DSL；后续升级时按官方迁移指南评估 compose.dependencies.*。
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            // decompose/decompose-compose 由 shared 以 api 暴露，经 projects.shared 传递可达
            implementation(projects.shared)
            implementation(libs.miuix.ui)
            implementation(libs.napier)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.profileinstaller)
        }

        desktopMain.dependencies {
            // projects.shared 已在 commonMain 声明，desktop 继承，无需重复
            implementation(compose.desktop.currentOs)
            implementation(libs.jna)
            implementation(libs.jna.platform)
        }

        desktopTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit)
        }
    }
}

android {
    namespace = "com.perol.pixez"
    // MIUIX 0.9.4 要求 compileSdk >= 36
    compileSdk = 36

    defaultConfig {
        applicationId = "com.perol.pixez.miuix"
        minSdk = 24
        targetSdk = 35
        versionCode = 10010097
        versionName = "0.9.109.7-miuix"
    }

    signingConfigs {
        if (releaseSigningProps != null) {
            create("release") {
                storeFile = rootProject.file(releaseSigningProps.getProperty("storeFile"))
                storePassword = releaseSigningProps.getProperty("storePassword")
                keyAlias = releaseSigningProps.getProperty("keyAlias")
                keyPassword = releaseSigningProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 有 keystore.properties 时用正式签名，否则回退 debug，避免无密钥环境构建失败
            signingConfig = signingConfigs.getByName(
                if (releaseSigningConfigured) "release" else "debug"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/INDEX.LIST",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "/META-INF/*.version",
                "/META-INF/*.txt",
                "/META-INF/*.properties",
                "DebugProbesKt.bin"
            )
        }
    }
}


androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        if (!releaseSigningConfigured) {
            // 产物自证：debug 签名的 release 包在清单版本名上带 -debug-signed，安装后可直接辨认。
            // 基础版本仍取 defaultConfig 的字面量，避免在脚本里再写一份版本号。
            val baseVersionName = android.defaultConfig.versionName
            if (baseVersionName != null) {
                variant.outputs.forEach { output ->
                    output.versionName.set(baseVersionName + "-debug-signed")
                }
            }
        }
    }
}

// 开发运行（gradlew :composeApp:run）开启桌面调试日志；jpackage 发布包不带该属性
tasks.matching { it.name == "run" }.configureEach {
    (this as? JavaExec)?.jvmArgs("-Dpixez.debug=true")
}

compose.desktop {
    application {
        mainClass = "com.perol.pixez.desktop.MainKt"
        // jpackage 的 JDK 由注入或平台常规安装目录解析（见 resolveJpackageJdk）：不在脚本里写机器私有路径。
        // 全部落空时回退 Gradle 当前 JVM，并显式告警——jpackage 缺失时该 JVM 会在 checkRuntime 处失败。
        val jpackageJdk = resolveJpackageJdk(project)
        if (jpackageJdk != null) {
            javaHome = jpackageJdk.absolutePath
        } else {
            logger.warn(
                "未找到带 jpackage 的 JDK（-PpackageJavaHome、PACKAGE_JAVA_HOME、JAVA_HOME 与常规安装目录均未命中）：" +
                    "打包将回退 Gradle 当前 JVM " + System.getProperty("java.home"),
            )
        }

        nativeDistributions {
            // 只声明宿主真正能产出的格式：Dmg 需 macOS、Deb 需 Linux，在 Windows 上它们永远不会生成。
            val hostFormats: List<TargetFormat> = when {
                OperatingSystem.current().isWindows -> listOf(TargetFormat.Exe, TargetFormat.Msi)
                OperatingSystem.current().isMacOsX -> listOf(TargetFormat.Dmg)
                OperatingSystem.current().isLinux -> listOf(TargetFormat.Deb)
                else -> emptyList()
            }
            targetFormats(*hostFormats.toTypedArray())

            packageName = "PixEz"
            packageVersion = "0.9.109"
            description = "PixEz MIUIX - Pixiv Client with Xiaomi HyperOS design"
            copyright = "© 2026 PixEz Contributors"
            vendor = "PixEz"

            windows {
                iconFile.set(project.file("src/desktopMain/resources/icon.ico"))
                menu = true
                shortcut = true
                dirChooser = true
                perUserInstall = false
                upgradeUuid = "8b646c21-3965-4f40-b384-0a3ffcbe0999"
            }

            macOS {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
                bundleID = "com.perol.pixez"
            }

            linux {
                iconFile.set(project.file("src/desktopMain/resources/icon.png"))
            }

            modules(
                "java.base",
                "java.desktop",
                "java.sql",
                "java.naming",
                "java.management",
                "java.net.http",
                "java.security.jgss",
                "java.xml",
                "jdk.unsupported",
            )

            jvmArgs += listOf(
                "-XX:+UseG1GC",
                "-XX:+UseStringDeduplication",
                "-Xms128m",
                "-Xmx2048m",
                "-Dfile.encoding=UTF-8",
                "-Djava.net.useSystemProxies=true",
                "-Dskiko.fps=0",
                "-Dskiko.vsync.enabled=true",
                "-Dskiko.hardwareAcceleration=true",
                "-Dskiko.directx.enabled=true",
                "-Dcompose.interop.blending=true",
                "-Dsun.java2d.d3d=true",
            )
        }
    }
}

tasks.register<Zip>("packageWindowsPortableZip") {
    group = "compose desktop"
    description = "Packs the Windows desktop distributable into a clean portable zip archive."
    dependsOn("createDistributable")
    from(layout.buildDirectory.dir("compose/binaries/main/app/PixEz"))
    into("PixEz")
    archiveFileName.set("PixEz-windows-x64-portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("compose/binaries/main/zip"))
}

tasks.register("packageWindowsSingleFileExe") {
    group = "compose desktop"
    description = "Packs the entire application and JRE runtime into a standalone single-file self-contained EXE."
    dependsOn("packageWindowsPortableZip")

    doLast {
        val os = org.gradle.internal.os.OperatingSystem.current()
        if (!os.isWindows) {
            logger.warn("packageWindowsSingleFileExe is only supported on Windows.")
            return@doLast
        }

        val zipFile = layout.buildDirectory.file("compose/binaries/main/zip/PixEz-windows-x64-portable.zip").get().asFile
        if (!zipFile.exists()) {
            throw GradleException("Portable zip archive not found: ${zipFile.absolutePath}")
        }

        val iconFile = file("src/desktopMain/resources/icon.ico")
        val outputDir = layout.buildDirectory.dir("compose/binaries/main/single-exe").get().asFile
        outputDir.mkdirs()
        val targetExe = File(outputDir, "PixEz-Standalone.exe")

        val cscPaths = listOf(
            File("C:\\Windows\\Microsoft.NET\\Framework64\\v4.0.30319\\csc.exe"),
            File("C:\\Windows\\Microsoft.NET\\Framework\\v4.0.30319\\csc.exe"),
        )
        val csc = cscPaths.firstOrNull { it.exists() }
            ?: throw GradleException("csc.exe (.NET Framework compiler) not found.")

        val sourceFile = File(temporaryDir, "Launcher.cs")
        sourceFile.writeText(
            """
            using System;
            using System.Diagnostics;
            using System.IO;
            using System.IO.Compression;
            using System.Reflection;

            namespace PixEzLauncher
            {
                static class Program
                {
                    [STAThread]
                    static int Main(string[] args)
                    {
                        try
                        {
                            string localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
                            string appDir = Path.Combine(localAppData, "PixEz", "standalone_runtime");
                            string targetExe = Path.Combine(appDir, "PixEz", "PixEz.exe");

                            string stampFile = Path.Combine(appDir, ".version_stamp");
                            string currentExePath = Assembly.GetExecutingAssembly().Location;
                            string exeStamp = File.GetLastWriteTimeUtc(currentExePath).Ticks.ToString();

                            if (!File.Exists(targetExe) || !File.Exists(stampFile) || File.ReadAllText(stampFile) != exeStamp)
                            {
                                if (Directory.Exists(appDir))
                                {
                                    try { Directory.Delete(appDir, true); } catch { }
                                }
                                Directory.CreateDirectory(appDir);

                                using (Stream stream = Assembly.GetExecutingAssembly().GetManifestResourceStream("payload.zip"))
                                {
                                    if (stream == null)
                                    {
                                        throw new Exception("Embedded runtime payload not found.");
                                    }
                                    using (ZipArchive archive = new ZipArchive(stream))
                                    {
                                        archive.ExtractToDirectory(appDir);
                                    }
                                }
                                try { File.WriteAllText(stampFile, exeStamp); } catch { }
                            }

                            ProcessStartInfo psi = new ProcessStartInfo();
                            psi.FileName = targetExe;
                            psi.WorkingDirectory = Path.GetDirectoryName(targetExe);
                            psi.UseShellExecute = false;

                            if (args != null && args.Length > 0)
                            {
                                psi.Arguments = string.Join(" ", Array.ConvertAll(args, a => a.Contains(" ") ? "\"" + a + "\"" : a));
                            }

                            using (Process proc = Process.Start(psi))
                            {
                                proc.WaitForExit();
                                return proc.ExitCode;
                            }
                        }
                        catch (Exception ex)
                        {
                            System.Windows.Forms.MessageBox.Show("启动 PixEz 失败: " + ex.Message, "PixEz Error", System.Windows.Forms.MessageBoxButtons.OK, System.Windows.Forms.MessageBoxIcon.Error);
                            return 1;
                        }
                    }
                }
            }
            """.trimIndent(),
        )

        val command = mutableListOf(
            csc.absolutePath,
            "/target:winexe",
            "/optimize+",
            "/platform:x64",
            "/r:System.dll",
            "/r:System.IO.Compression.dll",
            "/r:System.IO.Compression.FileSystem.dll",
            "/r:System.Windows.Forms.dll",
            "/resource:${zipFile.absolutePath},payload.zip",
            "/out:${targetExe.absolutePath}",
        )
        if (iconFile.exists()) {
            command.add("/win32icon:${iconFile.absolutePath}")
        }
        command.add(sourceFile.absolutePath)

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw GradleException("Failed to compile single-file EXE: \n$output")
        }

        println("Successfully generated Single-File Self-Contained EXE: ${targetExe.absolutePath} (${targetExe.length() / 1024 / 1024} MB)")
    }
}

