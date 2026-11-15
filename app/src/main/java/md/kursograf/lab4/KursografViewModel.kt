package md.kursograf.lab4

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import md.kursograf.lab4.data.BankRatesSource
import md.kursograf.lab4.data.LocalRatesCache
import md.kursograf.lab4.data.RateProblem
import md.kursograf.lab4.data.RatesRepository
import md.kursograf.lab4.domain.RateArchive

internal data class KursografState(
    val archive: RateArchive? = null,
    val updating: Boolean = true,
    val usingCache: Boolean = false,
    val problem: RateProblem? = null,
)

internal class KursografViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RatesRepository(BankRatesSource(), LocalRatesCache(application.filesDir))
    private val mutableState = MutableStateFlow(KursografState())
    val state = mutableState.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            val existing = mutableState.value.archive
            val previous = existing ?: repository.cached()
            mutableState.value = KursografState(
                archive = previous,
                usingCache = previous != null,
            )
            val chisinauToday = LocalDate.now(ZoneId.of("Europe/Chisinau"))
            val update = repository.refresh(chisinauToday, previous)
            mutableState.value = KursografState(update.archive, false, update.usingCache, update.problem)
        }
    }
}
