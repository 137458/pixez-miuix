package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import com.itos.coui.layout.CouiPageDefaults
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.ui.components.PixivAsyncImage

/** 特辑卡片：16:9 封面加标题副标题，仅特辑列表使用。 */

/**
 * COUI Spotlight 文章卡片：16:9 封面 + 标题/副标题。
 * 圆角与底色取官方 Group 卡片口径（CouiPageDefaults.shape、themeColors().card），
 * 文字块按官方行版式取 rowPadding/contentPadding 与 summarySpacing。
 */
@Composable
internal fun CouiSpotlightCard(
    article: SpotlightArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageColors = CouiPageDefaults.themeColors()
    val pressInteraction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CouiPageDefaults.shape)
            .background(pageColors.card)
            .couiPressFeedback(pressInteraction)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        PixivAsyncImage(
            model = article.thumbnail,
            contentDescription = article.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = CouiPageDefaults.rowPadding,
                    vertical = CouiPageDefaults.contentPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(CouiPageDefaults.summarySpacing),
        ) {
            Text(
                text = article.title,
                kind = CouiTextKind.ItemTitle,
                color = pageColors.title,
                maxLines = 2,
            )
            if (article.pureTitle.isNotBlank() && article.pureTitle != article.title) {
                Text(
                    text = article.pureTitle,
                    kind = CouiTextKind.ItemSummary,
                    color = pageColors.summary,
                    maxLines = 2,
                )
            }
        }
    }
}
