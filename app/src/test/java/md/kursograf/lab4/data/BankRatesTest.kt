package md.kursograf.lab4.data

import java.time.LocalDate
import md.kursograf.lab4.domain.Currency
import md.kursograf.lab4.domain.CurrencyPair
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BankRatesTest {
    @Test
    fun readsDateNormalizesNominalAndComputesCrossRate() {
        val reply = xml(
            entry("EUR", "1", "20.40") +
                entry("UAH", "10", "4.25") +
                entry("CAD", "1", "12.91"),
        )
        val rates = parseBankRates(reply)

        assertEquals(LocalDate.of(2026, 10, 8), rates.date)
        assertEquals("0.425".toBigDecimal(), rates.leiPerUnit[Currency.UAH])
        assertEquals(0, "48".toBigDecimal().compareTo(rates.rateOf(CurrencyPair(Currency.EUR, Currency.UAH))))
        assertNull(rates.rateOf(CurrencyPair(Currency.GBP, Currency.MDL)))
        assertEquals(2, rates.leiPerUnit.size)
    }

    @Test
    fun reportsAnEmptyBodyAndAnEmptySeries() {
        assertProblem("  \n", RateProblem.EMPTY)
        assertProblem(xml(""), RateProblem.EMPTY)
        assertProblem(xml(entry("CAD", "1", "12")), RateProblem.EMPTY)
    }

    @Test
    fun rejectsMalformedXmlAndInvalidDates() {
        assertProblem("<html>maintenance</html>", RateProblem.INVALID)
        assertProblem("<ValCurs Date=\"08.10.2026\"><Valute>", RateProblem.INVALID)
        assertProblem(xml(entry("EUR", "1", "20"), date = "31.02.2026"), RateProblem.INVALID)
    }

    @Test
    fun rejectsBadPricesAndAnInvalidNominal() {
        for (price in listOf("0", "-10", "NaN", "1e2000000", "")) {
            assertProblem(xml(entry("EUR", "1", price)), RateProblem.INVALID)
        }
        assertProblem(xml(entry("EUR", "0", "20")), RateProblem.INVALID)
        assertProblem(xml(entry("EUR", "?", "20")), RateProblem.INVALID)
    }

    @Test
    fun rejectsDuplicateSupportedCurrencyAndXmlDeclarationsForExternalEntities() {
        assertProblem(xml(entry("USD", "1", "18") + entry("USD", "1", "19")), RateProblem.INVALID)
        assertProblem("<!DOCTYPE ValCurs []>" + xml(""), RateProblem.INVALID)
    }

    private fun assertProblem(response: String, problem: RateProblem) {
        assertEquals(problem, assertThrows(RatesFailure::class.java) { parseBankRates(response) }.problem)
    }

    private fun xml(body: String, date: String = "08.10.2026") = "<ValCurs Date=\"$date\">$body</ValCurs>"

    private fun entry(code: String, nominal: String, published: String) =
        "<Valute><CharCode>$code</CharCode><Nominal>$nominal</Nominal><Value>$published</Value></Valute>"
}
