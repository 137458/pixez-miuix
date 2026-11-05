package com.perol.pixez.shared.ui.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 多语言覆盖总检查（IB-2）：逐语言枚举 [AppStrings] 全部成员，而不是抽检固定几个键。
 *
 * 接口的默认实现（67 个 get() 默认值）会让未翻译成员静默回退：用户看到中文或英文。
 * Kotlin 在 -Xjvm-default=disable 模式下会为「未显式 override 的默认成员」在实现类上
 * 生成一个 ACC_BRIDGE 方法委托到 AppStrings$DefaultImpls，因此 Java 反射的
 * Method.isBridge 就是「该语言没有真正写这条词条」的机械判据；委托写法（by EnStrings）
 * 同样会生成桥方法，一并被拦截。
 */
class AppStringsCompletenessTest {

    /** 13 种语言的实现体；新增语言必须在此登记（下方注册表数量断言会兜底）。 */
    private val implementations: Map<String, AppStrings> = linkedMapOf(
        "zh-CN" to ZhCnStrings,
        "zh-TW" to ZhTwStrings,
        "en-US" to EnStrings,
        "ja" to JaStrings,
        "ko" to KoStrings,
        "ru" to RuStrings,
        "es" to EsStrings,
        "tr" to TrStrings,
        "id" to IdStrings,
        "fil" to FilStrings,
        "de" to DeStrings,
        "vi" to ViStrings,
        "fa" to FaStrings,
    )

    /** 接口全部无参 String 属性访问器（如 getRelatedUsers）。 */
    private val interfaceGetters: List<String> = AppStrings::class.java.declaredMethods
        .filter { it.parameterCount == 0 && it.returnType == String::class.java }
        .map { it.name }

    private fun displayName(getter: String): String =
        getter.removePrefix("get").replaceFirstChar { it.lowercaseChar() }

    @Test
    fun `接口访问器规模与语言数量符合预期`() {
        assertTrue(interfaceGetters.size > 600, "AppStrings 词条数量异常: " + interfaceGetters.size)
        assertEquals(13, implementations.size, "语言实现体数量应为 13")
    }

    @Test
    fun `每种语言都显式实现接口全部词条`() {
        val fallbacks = mutableListOf<String>()
        for ((language, implementation) in implementations) {
            val declaredGetters = implementation.javaClass.declaredMethods
                .filter { it.parameterCount == 0 }
                .associateBy { it.name }
            for (getter in interfaceGetters) {
                val declared = declaredGetters[getter]
                when {
                    declared == null -> fallbacks += language + "." + displayName(getter) + "（未声明）"
                    declared.isBridge -> fallbacks += language + "." + displayName(getter) + "（继承接口默认值）"
                }
            }
        }
        assertEquals(
            emptyList(),
            fallbacks,
            "存在未翻译而静默回退的词条 " + fallbacks.size + " 处: " + fallbacks,
        )
    }

    @Test
    fun `每种语言的每条词条都非空`() {
        val empties = mutableListOf<String>()
        for ((language, implementation) in implementations) {
            for (getter in interfaceGetters) {
                val value = implementation.javaClass.getMethod(getter).invoke(implementation) as? String
                if (value.isNullOrBlank()) empties += language + "." + displayName(getter)
            }
        }
        assertEquals(emptyList(), empties, "存在空词条 " + empties.size + " 处: " + empties)
    }

    @Test
    fun `语言注册表与语言实现体一一对应`() {
        assertEquals(
            implementations.size,
            LANGUAGE_OPTIONS.size,
            "LANGUAGE_OPTIONS 的语言数量与 AppStrings 实现体数量不一致",
        )
        val resolved = (0 until LANGUAGE_OPTIONS.size).map { AppStrings.fromLanguageNum(it) }
        assertEquals(
            implementations.size,
            resolved.toSet().size,
            "fromLanguageNum 未能覆盖全部语言实现体: " + resolved.map { it::class.simpleName },
        )
    }
}
