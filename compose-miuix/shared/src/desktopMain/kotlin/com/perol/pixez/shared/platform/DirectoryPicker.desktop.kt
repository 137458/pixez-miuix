package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

@Composable
actual fun rememberDirectoryPicker(onResult: (String?) -> Unit): () -> Unit {
    return remember {
        {
            SwingUtilities.invokeLater {
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = "选择保存目录"
                }
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    onResult(chooser.selectedFile.absolutePath)
                } else {
                    onResult(null)
                }
            }
        }
    }
}
