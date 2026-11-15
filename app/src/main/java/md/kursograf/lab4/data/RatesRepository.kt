package md.kursograf.lab4.data

import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import md.kursograf.lab4.domain.HISTORY_DAYS
import md.kursograf.lab4.domain.RateArchive
import md.kursograf.lab4.domain.RatesOnDate

internal data class RatesUpdate(
    val archive: RateArchive?,
    val usingCache: Boolean,
    val problem: RateProblem? = null,
)

internal class RatesRepository(private val source: RatesSource, private val cache: RatesCache) {
    suspend fun cached(): RateArchive? = withContext(Dispatchers.IO) { cache.read() }

    suspend fun refresh(today: LocalDate, previous: RateArchive?): RatesUpdate =
        withContext(Dispatchers.IO) {
            val todayReply = request(today)
            if (todayReply.problem == RateProblem.NETWORK) {
                return@withContext RatesUpdate(previous, previous != null, todayReply.problem)
            }

            val first = today.minusDays(HISTORY_DAYS.toLong() - 1)
            val saved = previous?.byRequestDate.orEmpty().filterKeys { it in first..today }
                .toMutableMap()
            todayReply.reading?.let { saved[today] = it }
            var fetched = todayReply.reading != null
            var problem = todayReply.problem
            val missing = (1 until HISTORY_DAYS)
                .map { today.minusDays(it.toLong()) }
                .filter { it !in saved }

            // Limit concurrency and reuse every successful date already on disk.
            for (batch in missing.chunked(4)) {
                val replies = coroutineScope { batch.map { day -> async { request(day) } }.awaitAll() }
                for (reply in replies) {
                    if (reply.reading != null) {
                        saved[reply.day] = reply.reading
                        fetched = true
                    } else if (problem == null) {
                        problem = reply.problem
                    }
                }
                if (replies.all { it.problem == RateProblem.NETWORK }) break
            }

            if (!fetched) return@withContext RatesUpdate(previous, previous != null, problem)

            val archive = RateArchive(today, saved)
            try {
                cache.write(archive)
            } catch (_: IOException) {
                problem = RateProblem.STORAGE
            }
            RatesUpdate(archive, usingCache = false, problem)
        }

    private fun request(day: LocalDate): DailyReply = try {
        DailyReply(day, reading = source.get(day))
    } catch (failure: RatesFailure) {
        DailyReply(day, problem = failure.problem)
    }

    private data class DailyReply(
        val day: LocalDate,
        val reading: RatesOnDate? = null,
        val problem: RateProblem? = null,
    )
}
