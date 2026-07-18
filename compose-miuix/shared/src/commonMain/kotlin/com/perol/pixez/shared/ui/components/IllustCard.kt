package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.perol.pixez.shared.data.model.Illust
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 插画卡片：等比例展示封面缩略图，并在下方显示标题与作者。
 *
 * 长宽比由插画自身尺寸决定，以模拟原 Flutter 应用的不规则瀑布流效果。
 */
@Composable
fun IllustCard(
    illust: Illust,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 计算封面长宽比，防止除零；宽度为 0 时回退到 1:1。
    val aspectRatio = if (illust.width > 0) {
        illust.width.toFloat() / illust.height.coerceAtLeast(1).toFloat()
    } else {
        1f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio.coerceIn(0.5f, 2f))
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            PixivAsyncImage(
                model = illust.imageUrls.medium,
                contentDescription = illust.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            text = illust.title,
            style = MiuixTheme.textStyles.body2,
            maxLines = 1,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp),
        )
        Text(
            text = illust.user.name,
            style = MiuixTheme.textStyles.footnote1,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
        )
    }
}
