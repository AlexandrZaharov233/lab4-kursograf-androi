package md.kursograf.lab4.data

import java.time.LocalDate
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import md.kursograf.lab4.domain.Currency
import md.kursograf.lab4.domain.RateArchive
import md.kursograf.lab4.domain.RatesOnDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RatesRepositoryTest {
    private val today = LocalDate.of(2026, 11, 1)

    @Test
    fun fetchesThirtyDatesAndRestoresTheSameArchiveAfterAnOfflineRestart() = runBlocking {
        val disk = JsonMemoryCache()
        val online = RatesRepository(RatesSource(::sample), disk)
        val read = online.refresh(today, online.cached())
        val archive = requireNotNull(read.archive)
        assertEquals(30, archive.byRequestDate.size)
        assertFalse(read.usingCache)
        assertNull(read.problem)

        val restarted = RatesRepository(RatesSource { throw RatesFailure(RateProblem.NETWORK) }, disk)
        val offline = restarted.refresh(today.plusDays(1), restarted.cached())
        assertEquals(archive, offline.archive)
        assertEquals(today, offline.archive?.latestDate)
        assertTrue(offline.usingCache)
        assertEquals(RateProblem.NETWORK, offline.problem)
    }

    @Test
    fun canStartOfflineWithAnEmptyCache() = runBlocking {
        val store = JsonMemoryCache()
        val repo = RatesRepository(RatesSource { throw RatesFailure(RateProblem.NETWORK) }, store)

        val result = repo.refresh(today, repo.cached())
        assertNull(result.archive)
        assertFalse(result.usingCache)
        assertEquals(RateProblem.NETWORK, result.problem)
    }

    @Test
    fun reportsAnInvalidHistoricalReplyAndKeepsAllValidDates() = runBlocking {
        val disk = JsonMemoryCache()
        val missing = today.minusDays(9)
        val repo = RatesRepository(RatesSource { date ->
            if (date == missing) throw RatesFailure(RateProblem.INVALID)
            sample(date)
        }, disk)

        val result = repo.refresh(today, null)
        assertEquals(RateProblem.INVALID, result.problem)
        assertEquals(29, result.archive?.byRequestDate?.size)
        assertEquals(result.archive, repo.cached())
    }

    @Test
    fun keepsHistoricalDownloadsAndRefreshesTheNewestDay() = runBlocking {
        val requests = ConcurrentLinkedQueue<LocalDate>()
        val disk = JsonMemoryCache()
        val repo = RatesRepository(RatesSource { date ->
            requests.add(date)
            sample(date)
        }, disk)
        val first = repo.refresh(today, null)
        requests.clear()

        val second = repo.refresh(today.plusDays(1), first.archive)
        assertEquals(listOf(today.plusDays(1)), requests.toList())
        assertEquals(30, second.archive?.byRequestDate?.size)
        assertEquals(today.plusDays(1), second.archive?.through)
    }

    @Test
    fun retriesATemporaryServerFailureOnceAndStoresTheRecoveredDay() = runBlocking {
        val attempts = AtomicInteger()
        val store = JsonMemoryCache()
        val repo = RatesRepository(RatesSource { day ->
            if (day == today && attempts.incrementAndGet() == 1) {
                throw RatesFailure(RateProblem.SERVER, canRetry = true)
            }
            sample(day)
        }, store)

        val result = repo.refresh(today, null)
        assertEquals(2, attempts.get())
        assertEquals(30, result.archive?.byRequestDate?.size)
        assertNull(result.problem)
    }

    private fun sample(day: LocalDate) = RatesOnDate(day, mapOf(Currency.EUR to "20.20".toBigDecimal()))

    private class JsonMemoryCache : RatesCache {
        private var json: String? = null

        override fun read(): RateArchive? = json?.let(::decodeArchive)

        override fun write(archive: RateArchive) {
            json = encodeArchive(archive)
        }
    }
}
