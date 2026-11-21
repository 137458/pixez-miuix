package com.perol.pixez.shared.ui.coui.component

/** 表情插入结果：新文本与插入后的光标位置（UTF-16 下标）。 */
internal data class CouiEmojiInsertion(val text: String, val cursor: Int)

/**
 * 评论输入栏的纯规则：发送门槛与表情插入位置。
 *
 * 从 CouiCommentsScreen 里抽出，便于脱离 Compose 单测；判定口径与页面改造前一致——
 * 草稿只做空白判定不裁剪，正文原样提交给发表接口。
 */
internal object CouiCommentComposer {

    /** 发送门槛：非空白草稿、不在发送中、且已登录。 */
    fun canSend(text: String, isSending: Boolean, isLoggedIn: Boolean?): Boolean =
        !isSending && text.isNotBlank() && isLoggedIn == true

    /**
     * 把 [emoji] 插入到 [text] 的 [selectionStart]..[selectionEnd] 区间，返回新文本与光标。
     *
     * 选区来自输入框，可能越界或因拖拽而逆序，这里统一钳制到 0..length 并按左闭右开处理，
     * 避免 replaceRange 抛 IndexOutOfBoundsException 或误删文本。
     */
    fun placeEmoji(
        text: CharSequence,
        selectionStart: Int,
        selectionEnd: Int,
        emoji: String,
    ): CouiEmojiInsertion {
        val length = text.length
        val boundedStart = selectionStart.coerceIn(0, length)
        val boundedEnd = selectionEnd.coerceIn(0, length)
        // 光标锚点取较小者：逆序选区等价于正序替换。
        val start = minOf(boundedStart, boundedEnd)
        val end = maxOf(boundedStart, boundedEnd)
        return CouiEmojiInsertion(text.replaceRange(start, end, emoji).toString(), start + emoji.length)
    }
}
