package com.perol.pixez.shared.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.perol.pixez.shared.data.model.SauceNaoResult
import com.perol.pixez.shared.ui.components.EmptyPlaceholder
import com.perol.pixez.shared.ui.i18n.LocalStrings
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * SauceNAO 以图搜图底部抽屉面板。
 * 遵循 MIUIX 设计规范，支持展示上传进度、异常重试与解析后的作品结果列表。
 */
@Composable
internal fun SauceNaoBottomSheet(
    isLoading: Boolean,
    error: String?,
    results: List<SauceNaoResult>,
    onDismissRequest: () -> Unit,
    onRetry: () -> Unit,
    onPickAnother: () -> Unit,
    onIllustClick: (Long) -> Unit,
) {
    val strings = LocalStrings.current
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    OverlayBottomSheet(
        show = true,
        title = strings.menuSauceNao,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 16.dp + navBarBottom,
                ),
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            InfiniteProgressIndicator()
                            Text(
                                text = strings.sauceNaoUploading,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Report,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.error,
                                modifier = Modifier.size(40.dp),
                            )
                            Text(
                                text = error,
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = onRetry,
                                    colors = ButtonDefaults.buttonColors(
                                        color = MiuixTheme.colorScheme.primary,
                                        contentColor = MiuixTheme.colorScheme.onPrimary,
                                    ),
                                ) {
                                    Text(text = strings.retry)
                                }
                                TextButton(
                                    text = strings.sauceNaoPickImage,
                                    onClick = onPickAnother,
                                )
                            }
                        }
                    }
                }

                results.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        EmptyPlaceholder(message = strings.searchEmptyIllust)
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onPickAnother,
                            colors = ButtonDefaults.buttonColors(
                                color = MiuixTheme.colorScheme.primary,
                                contentColor = MiuixTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(text = strings.sauceNaoPickImage)
                        }
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${strings.menuSauceNao} (${results.size})",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        TextButton(
                            text = strings.sauceNaoPickImage,
                            onClick = onPickAnother,
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(results, key = { it.illustId }) { item ->
                            SauceNaoResultCard(
                                item = item,
                                onClick = {
                                    onDismissRequest()
                                    onIllustClick(item.illustId)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SauceNaoResultCard(
    item: SauceNaoResult,
    onClick: () -> Unit,
) {
    val strings = LocalStrings.current
    val thumbShape = remember { RoundedCornerShape(8.dp) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!item.thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(thumbShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title?.ifBlank { null } ?: "Pixiv #${item.illustId}",
                    style = MiuixTheme.textStyles.title4,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!item.author.isNullOrBlank()) {
                    Text(
                        text = item.author,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!item.similarity.isNullOrBlank()) {
                        val simText = if (item.similarity.contains('%')) item.similarity else "${item.similarity}%"
                        Text(
                            text = "${strings.sauceNaoSimilarity}: $simText",
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = "#${item.illustId}",
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }

            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
