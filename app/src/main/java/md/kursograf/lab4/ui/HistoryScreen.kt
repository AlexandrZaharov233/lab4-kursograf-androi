package md.kursograf.lab4.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import md.kursograf.lab4.domain.CurrencyPair
import md.kursograf.lab4.domain.HistoryPeriod
import md.kursograf.lab4.domain.HistoryStatistics
import md.kursograf.lab4.domain.RateArchive
import md.kursograf.lab4.domain.Trend
import md.kursograf.lab4.domain.calculateStatistics
import md.kursograf.lab4.domain.pointsFor

@Composable
internal fun HistoryScreen(
    period: HistoryPeriod,
    pair: CurrencyPair,
    archive: RateArchive?,
    updating: Boolean,
    onPair: (CurrencyPair) -> Unit,
) {
    val title = when (period) {
        HistoryPeriod.WEEK -> "Сравнение за неделю"
        HistoryPeriod.MONTH -> "Сравнение за месяц"
    }
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CurrencyPicker("Из", pair.from, Modifier.weight(1f)) { onPair(pair.copy(from = it)) }
        CurrencyPicker("В", pair.to, Modifier.weight(1f)) { onPair(pair.copy(to = it)) }
    }

    val points = remember(archive, pair, period) { archive?.pointsFor(pair, period).orEmpty() }
    val statistics = remember(points) { calculateStatistics(points) }
    if (archive == null || statistics == null) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Text(
                if (updating) "Загружаем историю курса." else "За этот период для выбранной пары нет котировок.",
                modifier = Modifier.padding(18.dp),
            )
        }
        return
    }

    val lastDay = archive.through
    val firstDay = lastDay.minusDays(period.days.toLong() - 1)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "1 ${pair.from} в ${pair.to}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${fullDate(firstDay)} — ${fullDate(lastDay)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RateChart(points, firstDay, lastDay)
            Text(
                "Котировок за период: ${points.size} из ${period.days}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    HistorySummary(statistics, units = pair.to.name)
}

@Composable
private fun HistorySummary(statistics: HistoryStatistics, units: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Metric("Минимум", exchangeRate(statistics.minimum), Modifier.weight(1f))
                Metric("Максимум", exchangeRate(statistics.maximum), Modifier.weight(1f))
                Metric("Среднее", exchangeRate(statistics.average), Modifier.weight(1f))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text("Изменение за период", style = MaterialTheme.typography.titleMedium)
            val change = statistics.change
            if (change == null) {
                Text("Для расчёта изменения и тенденции нужны две даты.")
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Metric("В $units", signed(change.absolute, decimals = 8), Modifier.weight(1f))
                    Metric("В процентах", signed(change.percent, decimals = 4) + "%", Modifier.weight(1f))
                }
                TrendIndicator(change.trend)
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TrendIndicator(trend: Trend) {
    val (label, background, foreground) = when (trend) {
        Trend.GROWTH -> Triple("Рост", Color(0xFFD8F4EB), Color(0xFF075447))
        Trend.DECLINE -> Triple("Падение", Color(0xFFFFE7E1), Color(0xFF853027))
        Trend.FLAT -> Triple("Без изменений", Color(0xFFE7EEF0), Color(0xFF374D53))
    }
    Surface(
        color = background,
        contentColor = foreground,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun signed(value: BigDecimal, decimals: Int): String =
    if (value.signum() > 0) "+" + number(value, decimals) else number(value, decimals)
