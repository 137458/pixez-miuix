package com.perol.pixez.android

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.perol.pixez.PixEzApp
import com.perol.pixez.shared.AppDependencies
import com.perol.pixez.shared.ui.navigation.Config
import com.perol.pixez.shared.ui.navigation.RootComponent
import com.perol.pixez.shared.ui.navigation.decodeWindowConfig
import com.perol.pixez.shared.ui.navigation.encodeWindowConfig
import com.perol.pixez.shared.ui.navigation.animation.system.SystemIllustCardTransition

/**
 * 独立作品详情窗口：用于承接系统级原生窗口卡片转场（ITGSA / Oplus / Vivo / Xiaomi）。
 * 栈内退回根节点时直接 finish() 交由系统窗口引擎执行收回动画。
 */
class IllustDetailActivity : ComponentActivity() {

    private lateinit var dependencies: AppDependencies
    private lateinit var rootComponent: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialConfig = decodeWindowConfig(intent.getStringExtra(EXTRA_CONFIG_JSON))
            ?: intent.getLongExtra(EXTRA_ILLUST_ID, -1L).takeIf { it > 0L }?.let { Config.IllustDetail(it) }
        if (initialConfig == null) {
            finish()
            return
        }
        enableEdgeToEdge()
        dependencies = PixEzApplication.dependencies
        rootComponent = RootComponent(
            componentContext = defaultComponentContext(),
            settingsRepository = dependencies.settingsRepository,
            initialConfig = initialConfig,
            onExitRoot = { finish() },
        )
        attachIllustWindowInterceptor(this, rootComponent)
        applyWindowDisplayMode(this, dependencies.settingsRepository)
        observeWindowDisplayModeChanges(this, dependencies.settingsRepository)

        val sessionToken = intent.getStringExtra(SystemIllustCardTransition.EXTRA_SESSION)
        setContent {
            PixEzApp(
                dependencies = dependencies,
                rootComponent = rootComponent,
                systemTransitionSessionToken = sessionToken,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (::dependencies.isInitialized) {
            applyWindowDisplayMode(this, dependencies.settingsRepository)
        }
        SystemIllustCardTransition.refreshReturn(this, reason = "on_resume")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        SystemIllustCardTransition.refreshReturn(this, reason = "configuration_changed")
    }

    override fun finish() {
        val preparingToFinish = !isFinishing
        if (preparingToFinish) {
            SystemIllustCardTransition.refreshReturn(
                activity = this,
                reason = "before_finish",
                preparingToFinish = true,
            )
        }
        super.finish()
        if (preparingToFinish) {
            SystemIllustCardTransition.afterFinish(this)
        }
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) {
            SystemIllustCardTransition.release(
                intent.getStringExtra(SystemIllustCardTransition.EXTRA_SESSION),
            )
        }
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ILLUST_ID = "pixez_illust_id"
        private const val EXTRA_CONFIG_JSON = "pixez_window_config_json"

        fun createIntent(context: Context, illustId: Long): Intent =
            createIntent(context, Config.IllustDetail(illustId))

        fun createIntent(context: Context, config: Config): Intent =
            Intent(context, IllustDetailActivity::class.java)
                .putExtra(EXTRA_CONFIG_JSON, encodeWindowConfig(config))
                .apply {
                    if (config is Config.IllustDetail) {
                        putExtra(EXTRA_ILLUST_ID, config.illustId)
                    }
                }
    }
}
