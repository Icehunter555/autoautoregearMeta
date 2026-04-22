package dev.wizard.meta.util

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

object TimeUtils {
    private val formatterMap = HashMap<Pair<TimeFormat, TimeUnit>, DateTimeFormatter>()

    fun getDate(): String {
        return LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
    }

    fun getDate(dateFormat: DateFormat): String {
        return LocalDate.now().format(dateFormat.formatter)
    }

    fun getTime(): String {
        return LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    }

    fun getTime(timeFormat: TimeFormat, timeUnit: TimeUnit): String {
        return LocalTime.now().format(getFormatter(timeFormat, timeUnit))
    }

    private fun getFormatter(timeFormat: TimeFormat, timeUnit: TimeUnit): DateTimeFormatter {
        return formatterMap.getOrPut(timeFormat to timeUnit) {
            val pattern = if (timeUnit == TimeUnit.H24) {
                timeFormat.pattern.replace('h', 'H')
            } else {
                "${timeFormat.pattern} a"
            }
            DateTimeFormatter.ofPattern(pattern, Locale.US)
        }
    }

    enum class DateFormat(val formatter: DateTimeFormatter) {
        DDMMYY(DateTimeFormatter.ofPattern("dd/MM/yy")),
        YYMMDD(DateTimeFormatter.ofPattern("yy/MM/dd")),
        MMDDYY(DateTimeFormatter.ofPattern("MM/dd/yy"))
    }

    enum class TimeFormat(val pattern: String) {
        HHMMSS("hh:mm:ss"),
        HHMM("hh:mm"),
        HH("hh")
    }

    enum class TimeUnit {
        H12, H24
    }
}
