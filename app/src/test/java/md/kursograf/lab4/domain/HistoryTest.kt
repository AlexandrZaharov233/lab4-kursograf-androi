package md.kursograf.lab4.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryTest {
    private val date = LocalDate.of(2026, 10, 30)

    @Test
    fun calculatesMetricsInDateOrder() {
        val readings = listOf(point("20", 0), point("24", 6), point("22", 2))
        val statistics = requireNotNull(calculateStatistics(readings))
        val change = requireNotNull(statistics.change)

        assertDecimal("20", statistics.minimum)
        assertDecimal("24", statistics.maximum)
        assertDecimal("22", statistics.average)
        assertDecimal("4", change.absolute)
        assertDecimal("20", change.percent)
        assertEquals(Trend.GROWTH, change.trend)
    }

    @Test
    fun distinguishesDeclineAndAnUnchangedRate() {
        val decline = requireNotNull(calculateStatistics(listOf(point("20", 0), point("19", 1)))?.change)
        assertDecimal("-1", decline.absolute)
        assertDecimal("-5", decline.percent)
        assertEquals(Trend.DECLINE, decline.trend)

        val same = requireNotNull(calculateStatistics(listOf(point("19", 0), point("19.00", 1)))?.change)
        assertDecimal("0", same.absolute)
        assertDecimal("0", same.percent)
        assertEquals(Trend.FLAT, same.trend)
    }

    @Test
    fun returnsNoStatisticsForAnEmptyPeriod() {
        assertNull(calculateStatistics(emptyList()))
    }

    @Test
    fun singlePointProvidesStatisticsWithoutAssigningTrend() {
        val result = requireNotNull(calculateStatistics(listOf(point("18.93", 0))))
        assertDecimal("18.93", result.minimum)
        assertDecimal("18.93", result.maximum)
        assertDecimal("18.93", result.average)
        assertNull(result.change)
    }

    @Test
    fun selectsSevenOrThirtyInclusiveCalendarDays() {
        val all = (0 until HISTORY_DAYS).associate { offset ->
            val day = date.minusDays(offset.toLong())
            day to RatesOnDate(day, mapOf(Currency.EUR to "20".toBigDecimal()))
        }
        val archive = RateArchive(date, all)
        val pair = CurrencyPair(Currency.EUR, Currency.MDL)
        val week = archive.pointsFor(pair, HistoryPeriod.WEEK)
        val month = archive.pointsFor(pair, HistoryPeriod.MONTH)

        assertEquals(7, week.size)
        assertEquals(date.minusDays(6), week.first().date)
        assertEquals(date, week.last().date)
        assertEquals(30, month.size)
        assertEquals(date.minusDays(29), month.first().date)
        assertEquals(date, month.last().date)
        assertNotNull(archive.latestQuote(pair))
    }

    @Test
    fun usesLatestReadingForTheSameEffectiveDateAndNeverFillsMissingDates() {
        val effective = date.minusDays(1)
        val archive = RateArchive(
            date,
            mapOf(
                effective to RatesOnDate(effective, mapOf(Currency.EUR to "19".toBigDecimal())),
                date to RatesOnDate(effective, mapOf(Currency.EUR to "20".toBigDecimal())),
            ),
        )
        val points = archive.pointsFor(CurrencyPair(Currency.EUR, Currency.MDL), HistoryPeriod.WEEK)
        assertEquals(1, points.size)
        assertEquals(effective, points.single().date)
        assertDecimal("20", points.single().value)
    }

    private fun point(value: String, offset: Long) = RatePoint(date.plusDays(offset), value.toBigDecimal())

    private fun assertDecimal(expected: String, actual: BigDecimal) {
        assertEquals(0, expected.toBigDecimal().compareTo(actual))
    }
}
