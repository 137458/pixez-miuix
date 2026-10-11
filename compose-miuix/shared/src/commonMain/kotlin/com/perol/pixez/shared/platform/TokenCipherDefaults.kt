package com.perol.pixez.shared.platform

/**
 * 凭据加解密协议常量（架构审查 中-2 自 ui 层下沉）。
 *
 * 密文前缀是双端 [PlatformTokenCipher] actual 共用的存储协议，正名落位于 platform 包
 * 单点维护，不再反向依赖 ui 层 [com.perol.pixez.shared.ui.AppConstants]；
 * UI 侧经 `AppConstants.Auth` 保留的委托别名访问，调用点不变，随全量移包清理。
 */
object TokenCipherDefaults {

    /** 密文版本前缀：带此前缀的字符串视为已加密密文，无前缀视为历史明文。 */
    const val TOKEN_ENCRYPTION_PREFIX = "enc_v1:"

    /** Android KeyStore 凭证主密钥别名。 */
    const val KEYSTORE_ALIAS = "PixEzTokenMasterKey"
}
