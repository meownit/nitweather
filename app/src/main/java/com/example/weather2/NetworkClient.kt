package com.example.weather2

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class NetworkClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
    }

    suspend fun fetchCityCoordinates(cityName: String): CityResult? {
        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${cityName.trim()}&count=1"
        val geoResponse: GeocodingResponse = client.get(geoUrl).body()
        return geoResponse.results?.firstOrNull()
    }

    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherResponse {
        val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
        return client.get(weatherUrl).body()
    }

    fun close() {
        client.close()
    }
}