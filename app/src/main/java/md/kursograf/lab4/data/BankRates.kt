package md.kursograf.lab4.data

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.StringReader
import java.math.BigDecimal
import java.math.MathContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale
import md.kursograf.lab4.domain.Currency
import md.kursograf.lab4.domain.RatesOnDate
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory

internal enum class RateProblem(val message: String) {
    NETWORK("Нет соединения с Национальным банком Молдовы."),
    SERVER("Сервис курсов Национального банка Молдовы недоступен."),
    EMPTY("НБМ не вернул котировки за одну или несколько дат."),
    INVALID("НБМ вернул данные в неожиданном формате."),
    STORAGE("Котировки получены, но Android не смог записать кэш."),
}

internal class RatesFailure(val problem: RateProblem, cause: Throwable? = null) :
    Exception(problem.message, cause)

internal fun interface RatesSource {
    fun get(day: LocalDate): RatesOnDate
}

private val bankDateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ROOT)
    .withResolverStyle(ResolverStyle.STRICT)
private val bankDecimal = Regex("[0-9]{1,12}(?:[.][0-9]{1,12})?")
private val supportedAbbreviations = Currency.entries.filter { it != Currency.MDL }
    .associateBy { it.name }

internal class BankRatesSource : RatesSource {
    override fun get(day: LocalDate): RatesOnDate {
        val formatted = day.format(bankDateFormat)
        val address = "https://www.bnm.md/ro/official_exchange_rates?get_xml=1&date=$formatted"
        val text = try {
            readReply(address)
        } catch (error: IOException) {
            throw RatesFailure(RateProblem.NETWORK, error)
        }
        val rates = parseBankRates(text)
        if (rates.date > day) throw RatesFailure(RateProblem.INVALID)
        return rates
    }

    private fun readReply(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 7_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/xml, text/xml")
            connection.setRequestProperty("User-Agent", "Kursograf-Android/1.0")
            if (connection.responseCode !in 200..299) throw RatesFailure(RateProblem.SERVER)
            return connection.inputStream.use { stream ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                val maximumBytes = 128 * 1024
                while (true) {
                    val read = stream.read(buffer)
                    if (read == -1) break
                    if (output.size() + read > maximumBytes) throw RatesFailure(RateProblem.INVALID)
                    output.write(buffer, 0, read)
                }
                output.toString(Charsets.UTF_8.name())
            }
        } finally {
            connection.disconnect()
        }
    }
}

internal fun parseBankRates(response: String): RatesOnDate {
    val body = response.removePrefix("\uFEFF")
    if (body.isBlank()) throw RatesFailure(RateProblem.EMPTY)
    if ("<!DOCTYPE" in body) throw RatesFailure(RateProblem.INVALID)

    try {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(StringReader(body))
        parser.nextTag()
        parser.require(XmlPullParser.START_TAG, null, "ValCurs")

        val date = parser.getAttributeValue(null, "Date")
            ?: throw RatesFailure(RateProblem.INVALID)
        val rates = mutableMapOf<Currency, BigDecimal>()
        while (parser.nextTag() == XmlPullParser.START_TAG) {
            val entry = parseCurrency(parser) ?: continue
            if (rates.put(entry.first, entry.second) != null) {
                throw RatesFailure(RateProblem.INVALID)
            }
        }
        parser.require(XmlPullParser.END_TAG, null, "ValCurs")
        if (parser.next() != XmlPullParser.END_DOCUMENT) throw RatesFailure(RateProblem.INVALID)
        val effectiveDate = LocalDate.parse(date, bankDateFormat)
        if (rates.isEmpty()) throw RatesFailure(RateProblem.EMPTY)
        return RatesOnDate(effectiveDate, rates)
    } catch (error: XmlPullParserException) {
        throw RatesFailure(RateProblem.INVALID, error)
    } catch (error: IOException) {
        throw RatesFailure(RateProblem.INVALID, error)
    } catch (error: DateTimeParseException) {
        throw RatesFailure(RateProblem.INVALID, error)
    } catch (error: ArithmeticException) {
        throw RatesFailure(RateProblem.INVALID, error)
    }
}

private fun parseCurrency(parser: XmlPullParser): Pair<Currency, BigDecimal>? {
    parser.require(XmlPullParser.START_TAG, null, "Valute")
    var abbreviation: String? = null
    var nominal: String? = null
    var published: String? = null

    while (parser.nextTag() == XmlPullParser.START_TAG) {
        val field = parser.name
        val content = parser.nextText().trim()
        when (field) {
            "CharCode" -> abbreviation = content
            "Nominal" -> nominal = content
            "Value" -> published = content
        }
    }
    parser.require(XmlPullParser.END_TAG, null, "Valute")
    if (abbreviation.isNullOrEmpty()) throw RatesFailure(RateProblem.INVALID)
    val currency = supportedAbbreviations[abbreviation] ?: return null
    return currency to positiveDecimal(published).divide(positiveDecimal(nominal), MathContext.DECIMAL128)
}

private fun positiveDecimal(raw: String?): BigDecimal {
    if (raw == null || !bankDecimal.matches(raw)) throw RatesFailure(RateProblem.INVALID)
    return raw.toBigDecimal().takeIf { it.signum() > 0 } ?: throw RatesFailure(RateProblem.INVALID)
}
