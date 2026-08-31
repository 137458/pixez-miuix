package com.perol.pixez.shared.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.perol.pixez.shared.data.model.UserPreview
import com.perol.pixez.shared.data.model.isR18
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * 用户预览列表项：头像、名称、账号，以及最近几张作品预览。
 */
@Composable
internal fun UserPreviewItem(
    preview: UserPreview,
    onClick: () -> Unit,
) {
    val settings = LocalSettingsRepository.current
    val hIsNotAllow = settings?.hIsNotAllow ?: false
    val banAIIllust = settings?.banAIIllust ?: false

    val displayIllusts = remember(preview.illusts, hIsNotAllow, banAIIllust, settings?.changeVersion) {
        preview.illusts.filter { illust ->
            (!hIsNotAllow || !illust.isR18()) &&
                (!banAIIllust || illust.illustAIType != 2)
        }.take(3)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PixivAsyncImage(
            model = preview.user.profileImageUrls.medium,
            contentDescription = preview.user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = preview.user.name,
                style = MiuixTheme.textStyles.body1,
            )
            Text(
                text = "@${preview.user.account}",
                style = MiuixTheme.textStyles.footnote1,
            )
        }
        displayIllusts.forEach { illust ->
            PixivAsyncImage(
                model = illust.imageUrls.squareMedium,
                contentDescription = illust.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}

