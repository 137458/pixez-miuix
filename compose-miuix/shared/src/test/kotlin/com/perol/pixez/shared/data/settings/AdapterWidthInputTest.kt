package com.perol.pixez.shared.data.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdapterWidthInputTest {

    @Test
    fun `valid digits are parsed as is`() {
        assertEquals(160, parseCrossAdapterWidthInput("160"))
        assertEquals(SettingsRepository.CROSS_ADAPTER_WIDTH_MIN, parseCrossAdapterWidthInput("50"))
        assertEquals(SettingsRepository.CROSS_ADAPTER_WIDTH_MAX, parseCrossAdapterWidthInput("2160"))
    }

    @Test
    fun `surrounding whitespace is trimmed`() {
        assertEquals(160, parseCrossAdapterWidthInput("  160 "))
        assertEquals(200, parseCrossAdapterWidthInput("\t200\n"))
    }

    @Test
    fun `out of range input is coerced into allowed range`() {
        assertEquals(SettingsRepository.CROSS_ADAPTER_WIDTH_MIN, parseCrossAdapterWidthInput("1"))
        assertEquals(SettingsRepository.CROSS_ADAPTER_WIDTH_MAX, parseCrossAdapterWidthInput("99999"))
    }

    @Test
    fun `invalid input returns null`() {
        assertNull(parseCrossAdapterWidthInput(""))
        assertNull(parseCrossAdapterWidthInput("   "))
        assertNull(parseCrossAdapterWidthInput("abc"))
        assertNull(parseCrossAdapterWidthInput("16.5"))
        assertNull(parseCrossAdapterWidthInput("-160"))
        assertNull(parseCrossAdapterWidthInput("160dp"))
        assertNull(parseCrossAdapterWidthInput("１２３"))
        assertNull(parseCrossAdapterWidthInput("999999999999999999999"))
    }
}
