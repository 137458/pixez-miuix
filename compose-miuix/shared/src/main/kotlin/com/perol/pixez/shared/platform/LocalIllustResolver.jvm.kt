package com.perol.pixez.shared.platform

/**
 * 判断本地文件名是否按数字边界包含目标作品 ID [idStr]。
 * 避免 `123456` 误匹配 `9123456_p0.jpg` 等含同一数字子串的其它作品文件。
 */
internal fun containsExactIllustId(fileName: String, idStr: String): Boolean {
    if (idStr.isEmpty()) return false
    var index = fileName.indexOf(idStr)
    while (index >= 0) {
        val beforeIsDigit = index > 0 && fileName[index - 1].isDigit()
        val afterIndex = index + idStr.length
        val afterIsDigit = afterIndex < fileName.length && fileName[afterIndex].isDigit()
        if (!beforeIsDigit && !afterIsDigit) {
            return true
        }
        index = fileName.indexOf(idStr, index + 1)
    }
    return false
}
