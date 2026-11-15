package md.kursograf.lab4.domain

import java.math.BigDecimal
import java.math.MathContext

internal enum class HistoryPeriod(val days: Int) {
    WEEK(7),
    MONTH(HISTORY_DAYS),
}

internal enum class Trend { GROWTH, DECLINE, FLAT }

internal data class PeriodChange(val absolute: BigDecimal, val percent: BigDecimal) {
    val trend: Trend get() = when (absolute.signum()) {
        1 -> Trend.GROWTH
        -1 -> Trend.DECLINE
        else -> Trend.FLAT
    }
}

internal data class HistoryStatistics(
    val minimum: BigDecimal,
    val maximum: BigDecimal,
    val average: BigDecimal,
    val change: PeriodChange?,
)

internal fun RateArchive.pointsFor(pair: CurrencyPair, period: HistoryPeriod): List<RatePoint> {
    val first = through.minusDays(period.days.toLong() - 1)
    return observations.asSequence()
        .filter { it.date in first..through }
        .mapNotNull { reading -> reading.rateOf(pair)?.let { RatePoint(reading.date, it) } }
        .toList()
}

internal fun calculateStatistics(points: List<RatePoint>): HistoryStatistics? {
    if (points.isEmpty()) return null

    val sorted = points.sortedBy { it.date }
    val values = sorted.map { it.value }
    val first = values.first()
    val change = if (values.size < 2) {
        null
    } else {
        val absolute = values.last().subtract(first)
        val percent = absolute.movePointRight(2).divide(first, MathContext.DECIMAL128)
        PeriodChange(absolute, percent)
    }
    val average = values.reduce(BigDecimal::add)
        .divide(values.size.toBigDecimal(), MathContext.DECIMAL128)
    return HistoryStatistics(values.min(), values.max(), average, change)
}
