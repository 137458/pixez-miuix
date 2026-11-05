package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager

@Composable
actual fun rememberDirectoryPicker(
    initialDirectory: String?,
    dialogTitle: String?,
    onResult: (String?) -> Unit,
): () -> Unit {
    // platform 层不读 ui 的 CompositionLocal（架构审查 DN15）：初始目录与标题均由调用方传入。
    val currentOnResult = rememberUpdatedState(onResult)
    return remember(initialDirectory, dialogTitle) {
        {
            SwingUtilities.invokeLater {
                runCatching {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
                }
                val initialPath = initialDirectory?.takeIf { it.isNotBlank() }
                    ?: getDefaultPictureDirectory()
                val title = dialogTitle?.takeIf { it.isNotBlank() }
                    ?: PlatformDialogDefaults.PICK_DIRECTORY_TITLE
                val initialDir = File(initialPath).let { if (it.exists()) it else it.parentFile }
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    // 显式调用 setter：局部参数 dialogTitle 会遮蔽 apply 接收者的同名属性。
                    setDialogTitle(title)
                    if (initialDir != null && initialDir.exists()) {
                        currentDirectory = initialDir
                    }
                }
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    currentOnResult.value(chooser.selectedFile.absolutePath)
                } else {
                    currentOnResult.value(null)
                }
            }
        }
    }
}
