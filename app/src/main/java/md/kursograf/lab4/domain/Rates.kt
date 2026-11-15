package md.kursograf.lab4.domain

import java.math.BigDecimal
import java.math.MathContext
import java.time.LocalDate

internal const val HISTORY_DAYS = 30
private val amountPattern = Regex("[0-9]+(?:[,.][0-9]+)?")

internal enum class Currency(val label: String) {
    MDL("Молдавский лей"),
    EUR("Евро"),
    USD("Доллар США"),
    RON("Румынский лей"),
    UAH("Украинская гривна"),
    GBP("Фунт стерлингов"),
}

internal data class CurrencyPair(val from: Currency, val to: Currency) {
    fun reversed() = CurrencyPair(from = to, to = from)
}

internal data class RatesOnDate(
    val date: LocalDate,
    val leiPerUnit: Map<Currency, BigDecimal>,
) {
    init {
        require(leiPerUnit.isNotEmpty())
        require(Currency.MDL !in leiPerUnit && leiPerUnit.values.all { it.signum() > 0 })
    }

    fun rateOf(pair: CurrencyPair): BigDecimal? {
        val from = valueInLei(pair.from) ?: return null
        val to = valueInLei(pair.to) ?: return null
        return from.divide(to, MathContext.DECIMAL128)
    }

    private fun valueInLei(currency: Currency): BigDecimal? =
        if (currency == Currency.MDL) BigDecimal.ONE else leiPerUnit[currency]
}

internal data class RatePoint(val date: LocalDate, val value: BigDecimal)

internal data class RateArchive(
    val through: LocalDate,
    val byRequestDate: Map<LocalDate, RatesOnDate>,
) {
    init {
        val first = through.minusDays(HISTORY_DAYS.toLong() - 1)
        require(byRequestDate.isNotEmpty())
        require(byRequestDate.all { (day, reading) -> day in first..through && reading.date <= day })
    }

    val observations: List<RatesOnDate> = byRequestDate.toSortedMap()
        .values.reversed()
        .distinctBy { it.date }
        .sortedBy { it.date }

    val latestDate: LocalDate get() = observations.last().date

    fun latestQuote(pair: CurrencyPair): RatePoint? = observations.asReversed()
        .firstNotNullOfOrNull { reading -> reading.rateOf(pair)?.let { RatePoint(reading.date, it) } }
}

internal fun parseAmount(input: String): BigDecimal? {
    val text = input.trim()
    if (!amountPattern.matches(text)) return null
    return text.replace(',', '.').toBigDecimalOrNull()?.takeIf { it.signum() > 0 }
}
