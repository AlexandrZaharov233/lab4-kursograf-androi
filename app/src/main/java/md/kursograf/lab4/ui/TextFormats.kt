package md.kursograf.lab4.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val locale = Locale.forLanguageTag("ru-RU")
private val dateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu", locale)

internal fun fullDate(date: LocalDate): String = date.format(dateFormat)

internal fun number(value: BigDecimal, maximumDecimals: Int, minimumDecimals: Int = 0): String =
    NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = maximumDecimals
        minimumFractionDigits = minimumDecimals
        roundingMode = RoundingMode.HALF_UP
    }.format(value)

internal fun exchangeRate(value: BigDecimal): String = number(value, maximumDecimals = 8)
