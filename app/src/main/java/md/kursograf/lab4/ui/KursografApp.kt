package md.kursograf.lab4.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import md.kursograf.lab4.KursografState
import md.kursograf.lab4.domain.Currency
import md.kursograf.lab4.domain.CurrencyPair

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KursografApp(state: KursografState, onRefresh: () -> Unit) {
    var from by rememberSaveable { mutableStateOf(Currency.EUR) }
    var to by rememberSaveable { mutableStateOf(Currency.MDL) }
    var amount by rememberSaveable { mutableStateOf("100") }
    val pair = CurrencyPair(from, to)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Курсограф", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onRefresh, enabled = !state.updating) {
                        Text("Обновить")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                .imePadding().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DataStatus(state)
                ConverterScreen(
                    amount = amount,
                    pair = pair,
                    quote = state.archive?.latestQuote(pair),
                    updating = state.updating,
                    onAmount = { amount = it },
                    onPair = {
                        from = it.from
                        to = it.to
                    },
                )
            }
        }
    }
}

@Composable
private fun DataStatus(state: KursografState) {
    val date = state.archive?.latestDate?.let(::fullDate)
    val summary = when {
        state.usingCache && date != null -> "Данные из кэша · $date"
        date != null -> "Официальные курсы НБМ · $date"
        state.updating -> "Загружаем курсы НБМ"
        else -> "Курсов на устройстве пока нет"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(summary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            state.problem?.let {
                Text(it.message, style = MaterialTheme.typography.bodySmall)
            }
            if (state.updating) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}
