package com.perol.pixez.shared.platform

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

/**
 * iOS 平台实现：使用 `UIActivityViewController` 展示系统分享面板。
 *
 * 在 iPad 上必须设置 `popoverPresentationController.sourceView`，
 * 否则 `UIActivityViewController` 会抛出异常导致崩溃。
 */
actual class IllustShare {
    actual fun share(text: String, subject: String?) {
        val activityItems = buildList<Any> {
            add(text)
            subject?.let { add(it) }
        }

        val activityViewController = UIActivityViewController(
            activityItems = activityItems,
            applicationActivities = null,
        )

        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: throw IllegalStateException("无法获取 rootViewController，无法展示分享面板")

        activityViewController.popoverPresentationController?.sourceView = rootViewController.view

        rootViewController.presentViewController(activityViewController, animated = true, completion = null)
    }
}
