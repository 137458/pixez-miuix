package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberDirectoryPicker(
    initialDirectory: String?,
    dialogTitle: String?,
    onResult: (String?) -> Unit,
): () -> Unit {
    return remember {
        {
            onResult(null)
        }
    }
}
