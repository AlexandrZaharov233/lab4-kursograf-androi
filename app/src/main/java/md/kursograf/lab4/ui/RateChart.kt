package md.kursograf.lab4.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import md.kursograf.lab4.domain.RatePoint

private val chartDateFormat = DateTimeFormatter.ofPattern("dd.MM", Locale.ROOT)

@Composable
internal fun RateChart(points: List<RatePoint>, firstDay: LocalDate, lastDay: LocalDate) {
    val colors = MaterialTheme.colorScheme
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = colors.onSurfaceVariant, fontSize = 11.sp)
    val scale = remember(points) { chartScale(points) }
    val days = ChronoUnit.DAYS.between(firstDay, lastDay)
    val dateTicks = listOf(firstDay, firstDay.plusDays(days / 2), lastDay)
    val maximum = exchangeRate(points.maxOf { it.value })
    val minimum = exchangeRate(points.minOf { it.value })

    Canvas(
        modifier = Modifier.fillMaxWidth().height(176.dp).semantics {
            contentDescription = "Линейный график за $days с интервалом в ${days + 1} день. " +
                "Котировок ${points.size}. Минимум $minimum, максимум $maximum."
        },
    ) {
        val yLabels = listOf(scale.high, (scale.high + scale.low) / 2, scale.low).map { tick ->
            textMeasurer.measure(number(tick.toBigDecimal(), maximumDecimals = 6), labelStyle)
        }
        val left = yLabels.maxOf { it.size.width } + 9.dp.toPx()
        val right = size.width - 4.dp.toPx()
        val top = 13.dp.toPx()
        val bottom = size.height - 24.dp.toPx()

        fun x(date: LocalDate): Float {
            val fraction = ChronoUnit.DAYS.between(firstDay, date).toFloat() / days
            return left + (right - left) * fraction
        }

        fun y(value: Double): Float {
            val fraction = ((value - scale.low) / (scale.high - scale.low)).toFloat()
            return bottom - (bottom - top) * fraction
        }

        yLabels.forEachIndexed { index, label ->
            val tickY = top + (bottom - top) * index / (yLabels.size - 1)
            drawLine(colors.outlineVariant, Offset(left, tickY), Offset(right, tickY), 1.dp.toPx())
            drawText(label, topLeft = Offset(0f, tickY - label.size.height / 2f))
        }
        for (day in dateTicks) {
            val label = textMeasurer.measure(day.format(chartDateFormat), labelStyle)
            val labelX = (x(day) - label.size.width / 2f).coerceIn(0f, size.width - label.size.width)
            drawText(label, topLeft = Offset(labelX, bottom + 6.dp.toPx()))
        }

        val coordinates = points.map { point -> Offset(x(point.date), y(point.value.toDouble())) }
        val start = coordinates.first()
        val end = coordinates.last()
        if (coordinates.size > 1) {
            val line = Path().apply {
                moveTo(start.x, start.y)
                coordinates.drop(1).forEach { point -> lineTo(point.x, point.y) }
            }
            val fill = Path().apply {
                addPath(line)
                lineTo(end.x, bottom)
                lineTo(start.x, bottom)
                close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(colors.primary.copy(alpha = 0.27f), colors.primary.copy(alpha = 0.04f)), top, bottom))
            drawPath(line, colors.primary, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        drawCircle(colors.primary, radius = 4.dp.toPx(), center = end)
        drawCircle(colors.surface, radius = 1.5.dp.toPx(), center = end)
    }
}

private data class ChartRange(val low: Double, val high: Double)

private fun chartScale(points: List<RatePoint>): ChartRange {
    val low = points.minOf { it.value }.toDouble()
    val high = points.maxOf { it.value }.toDouble()
    val margin = if (high == low) maxOf(abs(high) * 0.0015, 0.000001) else (high - low) * 0.10
    return ChartRange(low - margin, high + margin)
}
