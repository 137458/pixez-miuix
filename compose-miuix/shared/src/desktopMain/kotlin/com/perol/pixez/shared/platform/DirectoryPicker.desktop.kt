package com.perol.pixez.shared.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.perol.pixez.shared.data.settings.LocalSettingsRepository
import com.perol.pixez.shared.ui.i18n.LocalStrings
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities
import javax.swing.UIManager

@Composable
actual fun rememberDirectoryPicker(onResult: (String?) -> Unit): () -> Unit {
    val strings = LocalStrings.current
    val settings = LocalSettingsRepository.current
    val currentOnResult = rememberUpdatedState(onResult)
    return remember(strings, settings) {
        {
            SwingUtilities.invokeLater {
                runCatching {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
                }
                val initialPath = settings?.storePath?.takeIf { it.isNotBlank() }
                    ?: getDefaultPictureDirectory()
                val initialDir = File(initialPath).let { if (it.exists()) it else it.parentFile }
                val chooser = JFileChooser().apply {
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    dialogTitle = strings.dialogSavePath
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
