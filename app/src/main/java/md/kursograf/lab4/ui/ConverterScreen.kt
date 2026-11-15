package md.kursograf.lab4.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import md.kursograf.lab4.R
import md.kursograf.lab4.domain.Currency
import md.kursograf.lab4.domain.CurrencyPair
import md.kursograf.lab4.domain.RatePoint
import md.kursograf.lab4.domain.parseAmount

@Composable
internal fun ConverterScreen(
    amount: String,
    pair: CurrencyPair,
    quote: RatePoint?,
    updating: Boolean,
    onAmount: (String) -> Unit,
    onPair: (CurrencyPair) -> Unit,
) {
    val value = remember(amount) { parseAmount(amount) }
    val invalid = amount.isNotBlank() && value == null
    Text("Конвертер валют", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                CurrencyPicker("Из", pair.from, Modifier.weight(1f)) { onPair(pair.copy(from = it)) }
                OutlinedIconButton(onClick = { onPair(pair.reversed()) }) {
                    Icon(painterResource(R.drawable.ic_swap), contentDescription = "Поменять валюты местами")
                }
                CurrencyPicker("В", pair.to, Modifier.weight(1f)) { onPair(pair.copy(to = it)) }
            }
            OutlinedTextField(
                value = amount,
                onValueChange = onAmount,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Сумма в ${pair.from}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                isError = invalid,
                supportingText = if (invalid) {
                    { Text("Введите число больше нуля. Подходит точка или запятая.") }
                } else {
                    null
                },
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF103B33), contentColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("Сумма в ${pair.to}", style = MaterialTheme.typography.labelLarge, color = Color(0xFFB6E8DB))
            val result = if (value != null && quote != null) {
                number(value.multiply(quote.value), maximumDecimals = 2, minimumDecimals = 2) + " " + pair.to
            } else {
                "—"
            }
            Text(result, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            if (quote != null) {
                Text(
                    "1 ${pair.from} = ${exchangeRate(quote.value)} ${pair.to}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text("Курс на ${fullDate(quote.date)}", style = MaterialTheme.typography.bodySmall)
            } else if (!updating) {
                Text("Для этой пары нет доступного курса.")
            }
        }
    }
}

@Composable
internal fun CurrencyPicker(
    label: String,
    value: Currency,
    modifier: Modifier = Modifier,
    onSelected: (Currency) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(value.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text("▾", color = MaterialTheme.colorScheme.primary)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                for (currency in Currency.entries) {
                    DropdownMenuItem(
                        text = { Text("${currency.name} · ${currency.label}") },
                        onClick = {
                            expanded = false
                            onSelected(currency)
                        },
                    )
                }
            }
        }
    }
}
