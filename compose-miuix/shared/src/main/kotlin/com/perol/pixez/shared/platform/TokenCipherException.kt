package com.perol.pixez.shared.platform

/**
 * 平台凭据加解密失败（中-17 fail-fast）。
 *
 * 由 [PlatformTokenCipher] 各端实现在密钥不可用、密文损坏或平台加密服务异常时抛出，
 * 取代旧的「加密失败返回明文、解密失败返回密文」静默降级：
 * - 加密失败上抛后存储层保存中断，明文凭证不会落库；
 * - 解密失败上抛后存储层将该账号按「凭证失效」清理（清空 token、保留账号轮廓），
 *   密文不再被当作凭证原样返回给网络层。
 *
 * [stage] 标识失败环节，取值见 [STAGE_ENCRYPT] / [STAGE_DECRYPT]。
 */
class TokenCipherException(
    val stage: String,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause) {
    companion object {
        const val STAGE_ENCRYPT = "encrypt"
        const val STAGE_DECRYPT = "decrypt"
    }
}
