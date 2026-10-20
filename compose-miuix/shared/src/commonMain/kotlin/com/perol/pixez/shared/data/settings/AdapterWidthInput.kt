package com.perol.pixez.shared.data.settings

/**
 * 解析布局设置页「宽度阈值」直接输入框的文本。
 *
 * 仅接受 ASCII 数字（允许首尾空白），结果收敛到 [SettingsRepository.CROSS_ADAPTER_WIDTH_MIN]
 * 与 [SettingsRepository.CROSS_ADAPTER_WIDTH_MAX] 之间；空串、非数字与数值溢出返回 null。
 */
fun parseCrossAdapterWidthInput(raw: String): Int? {
    val text = raw.trim()
    if (text.isEmpty() || text.any { it !in '0'..'9' }) return null
    val value = text.toIntOrNull() ?: return null
    return value.coerceIn(
        SettingsRepository.CROSS_ADAPTER_WIDTH_MIN,
        SettingsRepository.CROSS_ADAPTER_WIDTH_MAX,
    )
}
