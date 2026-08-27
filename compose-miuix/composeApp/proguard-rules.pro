# ==============================================================================
# PixEz MIUIX - ProGuard / R8 优化与混淆规则
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. 基础配置与通用属性
# ------------------------------------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**

# ------------------------------------------------------------------------------
# 2. Android 核心组件与小部件
# ------------------------------------------------------------------------------
-keep class com.perol.pixez.android.MainActivity { *; }
-keep class com.perol.pixez.android.widget.PixEzAppWidgetProvider { *; }
-keep class com.perol.pixez.shared.platform.ShareActionReceiver { *; }
-keep class androidx.core.content.FileProvider { *; }

# ------------------------------------------------------------------------------
# 3. Kotlin & Kotlinx 协程与序列化
# ------------------------------------------------------------------------------
# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class **$serializer {
    public static final **$serializer INSTANCE;
}
-keepattributes *Annotation*
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Kotlin Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ------------------------------------------------------------------------------
# 4. Decompose & Essenty 状态与导航
# ------------------------------------------------------------------------------
-dontwarn com.arkivanov.decompose.**
-dontwarn com.arkivanov.essenty.**
-keep class com.perol.pixez.shared.ui.navigation.RootComponent$Config** { *; }
-keep class com.perol.pixez.shared.data.model.** { *; }

# ------------------------------------------------------------------------------
# 5. SQLDelight 数据库
# ------------------------------------------------------------------------------
-dontwarn app.cash.sqldelight.**
-keep class com.perol.pixez.shared.data.local.** { *; }
-keep class * extends app.cash.sqldelight.db.SqlDriver { *; }
-keep class * extends app.cash.sqldelight.db.SqlSchema { *; }

# ------------------------------------------------------------------------------
# 6. Ktor 3 & OkHttp 客户端
# ------------------------------------------------------------------------------
-dontwarn io.ktor.**
-dontwarn okhttp3.**
-dontwarn okio.**

# OkHttp 反射与平台接入
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

-keep class io.ktor.** { *; }
-keep class okhttp3.** { *; }

# ------------------------------------------------------------------------------
# 7. Coil 3 图像加载
# ------------------------------------------------------------------------------
-dontwarn coil3.**
-keep class coil3.** { *; }

# ------------------------------------------------------------------------------
# 8. MIUIX & Backdrop 特效
# ------------------------------------------------------------------------------
-dontwarn top.yukonga.miuix.**
-dontwarn com.kyant.backdrop.**
-keep class top.yukonga.miuix.** { *; }
-keep class com.kyant.backdrop.** { *; }

# ------------------------------------------------------------------------------
# 9. Napier 日志
# ------------------------------------------------------------------------------
-dontwarn io.github.aakira.napier.**
-keep class io.github.aakira.napier.** { *; }

# ------------------------------------------------------------------------------
# 10. Multiplatform Settings
# ------------------------------------------------------------------------------
-dontwarn com.russhwolf.settings.**
-keep class com.russhwolf.settings.** { *; }

# ------------------------------------------------------------------------------
# 11. Release 剥离冗余调试日志 (Log Stripping)
# ------------------------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
