package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.THANKS_PEOPLES
import com.perol.pixez.shared.ui.coui.component.CouiFixedTopBar
import com.perol.pixez.shared.ui.coui.component.CouiReadableColumn
import com.perol.pixez.shared.ui.coui.component.CouiSettingsLazyColumn
import com.perol.pixez.shared.ui.i18n.LocalStrings

/**
 * COUI 致谢页：固定顶栏 + 致谢说明与人员名单卡片。
 *
 * 名单数据与顺序沿用 [THANKS_PEOPLES]，不做分页或筛选。
 */
@Composable
internal fun CouiThanksScreen(
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current
    val scheme = CouiTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        CouiFixedTopBar(
            title = strings.settingThanks,
            onBack = onBack,
        )

        CouiReadableColumn(modifier = Modifier.fillMaxSize()) {
            CouiSettingsLazyColumn {
                item(key = "coui_thanks_desc", contentType = "section") {
                    CouiSettingSection(title = strings.aboutThanksDesc) {
                        Text(
                            text = strings.aboutThanksDesc,
                            kind = CouiTextKind.ItemSummary,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }

                item(key = "coui_thanks_people", contentType = "section") {
                    CouiSettingSection(title = strings.settingThanks) {
                        THANKS_PEOPLES.forEach { name ->
                            Text(
                                text = name,
                                kind = CouiTextKind.ItemTitle,
                                color = scheme.onSurface,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
