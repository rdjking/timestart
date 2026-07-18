package com.example.timestart.platform.holiday

import com.example.timestart.data.local.HolidayCalendarDao
import com.example.timestart.data.local.HolidayCalendarEntity
import com.example.timestart.domain.holiday.HolidayCalendar
import com.example.timestart.domain.holiday.HolidayDataSource
import com.example.timestart.domain.holiday.HolidayDayInfo
import com.example.timestart.domain.holiday.HolidayDayType
import com.example.timestart.domain.holiday.LocalWeekPatternHolidayCalendar
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/** HTTPS client for timor.tech with a durable per-date cache and a weekday/weekend fallback. */
class TimorHolidayCalendar(
    private val holidayCalendarDao: HolidayCalendarDao,
    private val endpoint: String = "https://timor.tech/api/holiday/info",
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : HolidayCalendar {
    override fun dayInfo(date: LocalDate): HolidayDayInfo {
        fetchDayType(date)?.let { type ->
            holidayCalendarDao.upsert(
                HolidayCalendarEntity(
                    calendarDate = date.toString(),
                    dayType = type.apiValue,
                    updatedAt = nowMillis(),
                ),
            )
            return HolidayDayInfo(type, HolidayDataSource.NETWORK)
        }

        holidayCalendarDao.get(date.toString())
            ?.let { HolidayDayType.fromApiValue(it.dayType) }
            ?.let { return HolidayDayInfo(it, HolidayDataSource.CACHE) }

        return LocalWeekPatternHolidayCalendar.dayInfo(date)
    }

    private fun fetchDayType(date: LocalDate): HolidayDayType? = runCatching {
        val connection = (URL("$endpoint/$date").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode !in 200..299) return null
            val payload = connection.inputStream.bufferedReader().use { reader -> reader.readText() }
            val response = JSONObject(payload)
            if (response.optInt("code", -1) != 0) return null
            HolidayDayType.fromApiValue(response.optJSONObject("type")?.optInt("type", -1) ?: -1)
        } finally {
            connection.disconnect()
        }
    }.getOrNull()

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 5_000
        const val READ_TIMEOUT_MILLIS = 5_000
    }
}
