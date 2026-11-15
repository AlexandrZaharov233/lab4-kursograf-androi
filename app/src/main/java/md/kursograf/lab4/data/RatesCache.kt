package md.kursograf.lab4.data

import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeParseException
import md.kursograf.lab4.domain.Currency
import md.kursograf.lab4.domain.HISTORY_DAYS
import md.kursograf.lab4.domain.RateArchive
import md.kursograf.lab4.domain.RatesOnDate
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal interface RatesCache {
    fun read(): RateArchive?
    fun write(archive: RateArchive)
}

internal class LocalRatesCache(directory: File) : RatesCache {
    private val file = AtomicFile(File(directory, "official-rates-v1.json"))

    override fun read(): RateArchive? = try {
        decodeArchive(file.readFully().decodeToString())
    } catch (_: IOException) {
        null
    } catch (_: JSONException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: DateTimeParseException) {
        null
    }

    override fun write(archive: RateArchive) {
        val content = encodeArchive(archive).encodeToByteArray()
        val stream = file.startWrite()
        try {
            stream.write(content)
            file.finishWrite(stream)
        } catch (error: IOException) {
            file.failWrite(stream)
            throw error
        }
    }
}

internal fun encodeArchive(archive: RateArchive): String {
    val rows = JSONArray()
    for ((day, reading) in archive.byRequestDate.toSortedMap()) {
        val rates = JSONObject()
        reading.leiPerUnit.forEach { (currency, value) ->
            rates.put(currency.name, value.toPlainString())
        }
        rows.put(
            JSONObject()
                .put("requestDate", day.toString())
                .put("rateDate", reading.date.toString())
                .put("leiPerUnit", rates),
        )
    }
    return JSONObject().put("through", archive.through.toString()).put("days", rows).toString()
}

internal fun decodeArchive(content: String): RateArchive {
    val root = JSONObject(content)
    val through = LocalDate.parse(root.getString("through"))
    val days = root.getJSONArray("days")
    require(days.length() in 1..HISTORY_DAYS)

    val readings = buildMap {
        for (index in 0 until days.length()) {
            val row = days.getJSONObject(index)
            val requestDate = LocalDate.parse(row.getString("requestDate"))
            val rateDate = LocalDate.parse(row.getString("rateDate"))
            val rates = row.getJSONObject("leiPerUnit")
            val values = rates.keys().asSequence().associate { code ->
                Currency.valueOf(code) to rates.getString(code).toBigDecimal()
            }
            require(put(requestDate, RatesOnDate(rateDate, values)) == null)
        }
    }
    return RateArchive(through, readings)
}
