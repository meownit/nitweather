package com.meownit.nitweather

import android.content.Context
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

fun formatDayOfWeek(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}

fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

fun calculateDailyHumidityAverage(dailyDate: String, hourlyForecast: HourlyForecast): Int {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Parse the daily date to compare
    val targetDate = try {
        dateFormat.parse(dailyDate)
    } catch (e: Exception) {
        return 0
    }

    // Filter hourly data for the given day
    val dailyHumidities = hourlyForecast.time.mapIndexedNotNull { index, time ->
        try {
            val hourlyDate = inputFormat.parse(time)
            if (hourlyDate != null && dateFormat.format(hourlyDate) == dailyDate) {
                hourlyForecast.humidity[index]
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Calculate average humidity
    return if (dailyHumidities.isNotEmpty()) {
        dailyHumidities.average().toInt()
    } else {
        0
    }
}

fun parseHourlyForecast(hourlyForecast: HourlyForecast): List<HourlyForecastData> {
    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
    val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val currentTime = Calendar.getInstance()
    val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
    val nextHour = currentHour + 1

    // Find the index of the first hour after the current time
    val startIndex = hourlyForecast.time.indexOfFirst { timeString ->
        try {
            val date = inputFormat.parse(timeString)
            date?.let {
                val calendar = Calendar.getInstance().apply { time = it }
                calendar.get(Calendar.HOUR_OF_DAY) >= nextHour
            } ?: false
        } catch (e: Exception) {
            false
        }
    }.takeIf { it >= 0 } ?: 0

    // Select up to 24 hours starting from the next hour
    return (startIndex until (startIndex + 24)).mapNotNull { index ->
        if (index < hourlyForecast.time.size) {
            try {
                val date = inputFormat.parse(hourlyForecast.time[index])
                date?.let {
                    HourlyForecastData(
                        time = outputFormat.format(it),
                        temperature = hourlyForecast.temperature[index],
                        humidity = hourlyForecast.humidity[index],
                        windSpeed = hourlyForecast.windSpeed[index]
                    )
                }
            } catch (e: Exception) {
                null
            }
        } else null
    }
}


fun getWeatherCondition(weatherCode: Int): String {
    return when (weatherCode) {
        0 -> "Clear" // sun, moon
        1, 2, 3 -> "Cloudy" // sun-cloudy, moon-cloudy
        45, 48 -> "Fog" //
        51, 53, 55, 56, 57 -> "Drizzle" // rain
        61, 63, 65, 66, 67 -> "Rain" //rain.png
        71, 73, 75, 77 -> "Snow" // snow
        80, 81, 82 -> "Showers" // rain
        85, 86 -> "Snow Showers" // snow
        95, 96, 99 -> "Thunderstorm" // thunar
        else -> "Unknown" // question mark
    }
}

// Helper function to map weather code and day/night to drawable resource
fun getWeatherIconResource(weatherCode: Int, isDay: Boolean): Int {
    return when (weatherCode) {
        0 -> if (isDay) R.drawable.sun else R.drawable.moon
        1, 2, 3 -> if (isDay) R.drawable.sun_cloudy else R.drawable.moon_cloudy
        45, 48 -> R.drawable.cloud // Fog (no specific day/night variation)
        51, 53, 55, 56, 57 -> R.drawable.rainy // Drizzle
        61, 63, 65, 66, 67 -> R.drawable.rainy // Rain
        71, 73, 75, 77 -> R.drawable.snowy // Snow
        80, 81, 82 -> R.drawable.rainy // Showers
        85, 86 -> R.drawable.snowy // Snow Showers
        95, 96, 99 -> R.drawable.thunder // Thunderstorm
        else -> R.drawable.cloud // Fallback for unknown
    }
}