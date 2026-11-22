package com.perol.pixez.shared.ui.coui.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.THANKS_PEOPLES
import com.perol.pixez.shared.ui.coui.core.CouiItemSummary
import com.perol.pixez.shared.ui.coui.core.CouiRowDivider
import com.perol.pixez.shared.ui.coui.core.CouiSettingsGroup
import com.perol.pixez.shared.ui.coui.core.CouiSettingsScaffold
import com.perol.pixez.shared.ui.coui.core.CouiValueRow
import com.perol.pixez.shared.ui.i18n.LocalStrings

/**
 * COUI 致谢页：折叠顶栏 + 致谢说明与人员名单两个分组。
 *
 * 名单数据与顺序沿用 [THANKS_PEOPLES]，不做分页或筛选；分组与行版式交给 core 的
 * [CouiSettingsScaffold] 与官方 Group/Item，页面不再自铺底色、不自带顶栏与护栏。
 */
@Composable
internal fun CouiThanksScreen(
    onBack: () -> Unit,
) {
    val strings = LocalStrings.current

    CouiSettingsScaffold(
        title = strings.settingThanks,
        onBack = onBack,
    ) {
        item(key = "coui_thanks_desc", contentType = "section") {
            CouiSettingsGroup {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    CouiItemSummary(text = strings.aboutThanksDesc)
                }
            }
        }

        item(key = "coui_thanks_people", contentType = "section") {
            CouiSettingSection(title = strings.settingThanks) {
                THANKS_PEOPLES.forEachIndexed { index, name ->
                    if (index > 0) CouiRowDivider()
                    CouiValueRow(title = name)
                }
            }
        }
    }
}
