package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.perol.pixez.shared.data.model.Illust

@Composable
actual fun Modifier.illustDragAndDropSource(illust: Illust): Modifier = this
