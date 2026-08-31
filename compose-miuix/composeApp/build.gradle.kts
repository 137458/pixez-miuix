import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
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

    macosArm64()

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
            implementation(projects.shared)
            implementation(libs.miuix.ui)
            implementation(libs.napier)
            implementation(libs.decompose)
            implementation(libs.decompose.compose)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.profileinstaller)
        }

        desktopMain.dependencies {
            implementation(projects.shared)
            implementation(compose.desktop.currentOs)
            implementation("net.java.dev.jna:jna:5.14.0")
            implementation("net.java.dev.jna:jna-platform:5.14.0")
            implementation("com.mayakapps.compose:window-styler:0.3.2")
        }

        desktopTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.junit)
        }
    }
}

android {
    namespace = "com.perol.pixez"
    // MIUIX 0.9.4-rc01 要求 compileSdk >= 36
    compileSdk = 36

    defaultConfig {
        applicationId = "com.perol.pixez.miuix"
        minSdk = 24
        targetSdk = 35
        versionCode = 10010053
        versionName = "0.9.108.3-miuix"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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


compose.desktop {
    application {
        mainClass = "com.perol.pixez.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "PixEz"
            packageVersion = "0.9.108"
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

tasks.matching { it.name.contains("AarMetadata") }.configureEach {
    enabled = false
}

