package md.kursograf.lab4.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountTest {
    @Test
    fun acceptsPositiveAmountsWithEitherDecimalSeparator() {
        assertEquals("12.75".toBigDecimal(), parseAmount(" 12,75 "))
        assertEquals("12.75".toBigDecimal(), parseAmount("12.75"))
        assertEquals("725".toBigDecimal(), parseAmount("725"))
        assertEquals("0.01".toBigDecimal(), parseAmount("0,01"))
    }

    @Test
    fun rejectsMissingNonPositiveAndMalformedAmounts() {
        for (text in listOf("", " ", "0", "0,0", "-5", "5.5.2", "NaN", "1e3", "12 лей")) {
            assertNull(text, parseAmount(text))
        }
    }
}
