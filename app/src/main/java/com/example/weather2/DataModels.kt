package com.example.weather2

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
    val daily: DailyForecast
)

@Serializable
data class CurrentWeather(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Int,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("wind_speed_10m") val windSpeed: Double
)

@Serializable
data class DailyForecast(
    val time: List<String>,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double>
)

@Serializable
data class LocationWeather(val city: CityResult, val weather: WeatherResponse, val isCurrentLocation: Boolean = false)