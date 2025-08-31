package com.meownit.nitweather

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import android.util.Log
import io.ktor.client.statement.bodyAsText

class NetworkClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
        }
    }

    suspend fun fetchCityCoordinates(cityName: String): CityResult? {
        val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${cityName.trim().replace(" ", "%20")}&count=1"
        return try {
            val geoResponse: GeocodingResponse = client.get(geoUrl).body()
            val result = geoResponse.results?.firstOrNull()
            Log.d("NetworkClient", "Fetched coordinates for $cityName: $result")
            result
        } catch (e: Exception) {
            Log.e("NetworkClient", "Failed to fetch city coordinates for $cityName: ${e.message}", e)
            null
        }
    }

    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherResponse {
        // Validate coordinates
        if (!latitude.isFinite() || latitude < -90.0 || latitude > 90.0) {
            throw IllegalArgumentException("Latitude must be a valid number between -90 and 90, got $latitude")
        }
        if (!longitude.isFinite() || longitude < -180.0 || longitude > 180.0) {
            throw IllegalArgumentException("Longitude must be a valid number between -180 and 180, got $longitude")
        }

//        val weatherUrl = "https://api.open-meteo.com/v1/forecast?" +
//                "latitude=$latitude&longitude=$longitude&" +
//                "current=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,is_day,weathercode,pressure_msl&" +
//                "daily=temperature_2m_max,temperature_2m_min,wind_speed_10m_max&" +
//                "hourly=temperature_2m,relative_humidity_2m,wind_speed_10m&" +
//                "timezone=auto"


        val weatherUrl = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=$latitude&longitude=$longitude&" +
                "current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,pressure_msl,is_day,weathercode&" +
                "hourly=temperature_2m,relative_humidity_2m,wind_speed_10m&" +
                "daily=temperature_2m_max,temperature_2m_min,wind_speed_10m_max,relative_humidity_2m_max,relative_humidity_2m_min&" +
                "forecast_days=7&forecast_hours=24&" +
                "timezone=auto"

        Log.d("NetworkClient", "Fetching weather with URL: $weatherUrl")

        return try {
            val response = client.get(weatherUrl)
            when (response.status) {
                HttpStatusCode.OK -> response.body<WeatherResponse>()
                HttpStatusCode.BadRequest -> {
                    val errorBody = response.bodyAsText()
                    Log.e("NetworkClient", "API returned 400: $errorBody")
                    throw IllegalArgumentException("Invalid input: $errorBody")
                }
                else -> {
                    Log.e("NetworkClient", "API request failed with status ${response.status}: ${response.bodyAsText()}")
                    throw Exception("Failed to fetch weather: ${response.status.description}")
                }
            }
        } catch (e: Exception) {
            Log.e("NetworkClient", "Failed to fetch weather for lat=$latitude, lon=$longitude: ${e.message}", e)
            throw e
        }
    }

    fun close() {
        client.close()
    }
}