package com.meownit.nitweather

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResponse(val results: List<CityResult>? = null)

@Serializable
data class CityResult(val name: String, val latitude: Double, val longitude: Double)

@Serializable
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("current")
    val current: CurrentWeather,
    @SerialName("daily")
    val daily: DailyForecast,
    @SerialName("hourly")
    val hourly: HourlyForecast
)

@Serializable
data class CurrentWeather(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Int,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("wind_speed_10m") val windSpeed: Double,
    @SerialName("is_day") val is_day: Int,
    @SerialName("weathercode") val weathercode: Int,
    @SerialName("pressure_msl") val pressure_msl: Double
)

@Serializable
data class DailyForecast(
    val time: List<String>,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double>,
    @SerialName("wind_speed_10m_max") val wind_speed_10m_max: List<Double>
)

@Serializable
data class HourlyForecast(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature: List<Double>,
    @SerialName("relative_humidity_2m") val humidity: List<Int>,
    @SerialName("wind_speed_10m") val windSpeed: List<Double>
)

@Serializable
data class LocationWeather(val city: CityResult, val weather: WeatherResponse, val isCurrentLocation: Boolean = false)


data class HourlyForecastData(
    val time: String,
    val temperature: Double,
    val humidity: Int,
    val windSpeed: Double
)