package com.perol.pixez.shared.platform

import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * Desktop(JVM) 平台实现：使用 AWT 系统剪贴板复制与读取文本及位图图片。
 */
actual class IllustClipboard {
    actual fun copy(text: String) {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(text), null)
    }

    actual fun copyImage(imageBytes: ByteArray) {
        val bufferedImage = ImageIO.read(ByteArrayInputStream(imageBytes))
            ?: throw IllegalStateException("无法将字节流解析为图片位图")
        val transferable = ImageTransferable(bufferedImage)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
    }

    actual fun getText(): String? {
        return runCatching {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                clipboard.getData(DataFlavor.stringFlavor) as? String
            } else {
                null
            }
        }.getOrNull()
    }

    private class ImageTransferable(private val image: Image) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
        override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = flavor == DataFlavor.imageFlavor
        override fun getTransferData(flavor: DataFlavor?): Any {
            if (flavor == DataFlavor.imageFlavor) return image
            throw UnsupportedFlavorException(flavor)
        }
    }
}
