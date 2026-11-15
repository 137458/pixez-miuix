package com.perol.pixez.shared.ui.coui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.itos.coui.text.CouiTextKind
import com.itos.coui.text.Text
import com.itos.coui.theme.CouiTheme
import com.perol.pixez.shared.data.model.SpotlightArticle
import com.perol.pixez.shared.ui.components.PixivAsyncImage

/** 特辑卡片：16:9 封面加标题副标题，仅特辑列表使用。 */

/** COUI Spotlight 文章卡片：16:9 封面 + 标题/副标题。 */
@Composable
internal fun CouiSpotlightCard(
    article: SpotlightArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pressInteraction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CouiTheme.colorScheme.card)
            .couiPressFeedback(pressInteraction)
            .clickable(
                interactionSource = pressInteraction,
                indication = null,
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = article.title,
                kind = CouiTextKind.ItemTitle,
                color = CouiTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            if (article.pureTitle.isNotBlank() && article.pureTitle != article.title) {
                Text(
                    text = article.pureTitle,
                    kind = CouiTextKind.ItemSummary,
                    color = CouiTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}
