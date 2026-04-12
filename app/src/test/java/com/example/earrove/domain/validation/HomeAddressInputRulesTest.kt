package com.example.earrove.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAddressInputRulesTest {

    @Test
    fun normalize_trims_and_collapses_whitespace() {
        assertEquals("北京市 海淀区", HomeAddressInputRules.normalize("  北京市   海淀区  "))
    }

    @Test
    fun normalize_strips_control_chars() {
        assertEquals("北京市", HomeAddressInputRules.normalize("北京\u0000市"))
    }

    @Test
    fun isFormatValid_requires_ideograph_length_bounds() {
        assertFalse(HomeAddressInputRules.isFormatValid("abcd"))
        assertFalse(HomeAddressInputRules.isFormatValid("北京"))
        assertTrue(HomeAddressInputRules.isFormatValid("北京市海淀区"))
        assertFalse(HomeAddressInputRules.isFormatValid("北".repeat(201)))
    }
}
