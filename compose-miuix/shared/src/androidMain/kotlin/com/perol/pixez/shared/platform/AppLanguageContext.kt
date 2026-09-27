package com.perol.pixez.shared.platform

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import com.perol.pixez.shared.data.settings.SettingsFactory
import com.perol.pixez.shared.data.settings.SettingsRepository

/**
 * 按应用内设定语言包装 Context，供通知、系统分享面板、Toast 等平台层文案取值。
 *
 * Android 的资源解析只认 `Configuration` 里的设备语言，而应用内语言是独立的 `languageNum` 设置，
 * 两者不一致时（设备中文、界面选英文）平台层会串语言，必须在 `getString` 前覆盖一次 `Configuration`。
 * 语言序号见 [languageTagForNum]。
 */
internal object AppLanguageContext {

    @Volatile
    private var repository: SettingsRepository? = null

    private fun settings(context: Context): SettingsRepository {
        repository?.let { return it }
        synchronized(this) {
            repository?.let { return it }
            return SettingsRepository(SettingsFactory(context.applicationContext).createSettings())
                .also { repository = it }
        }
    }

    /** 序号越界（无对应语言）时原样返回，不创建包装 Context。 */
    fun decorate(context: Context): Context {
        val tag = languageTagForNum(settings(context).languageNum) ?: return context
        val base = context.applicationContext ?: context
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocales(LocaleList.forLanguageTags(tag))
        return base.createConfigurationContext(configuration)
    }
}

/** [AppLanguageContext.decorate] 的中缀写法。 */
internal fun Context.localizedForAppLanguage(): Context = AppLanguageContext.decorate(this)
