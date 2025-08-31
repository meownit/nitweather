package com.meownit.nitweather

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.location.Geocoder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val _locationsState = MutableStateFlow<List<LocationWeather>>(emptyList())
    val locationsState = _locationsState.asStateFlow()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val networkClient = NetworkClient()
    private val database = AppDatabase.getDatabase(application)
    private val weatherDao = database.weatherDao()
    private val prefs: SharedPreferences = application.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
    private val visitedCities = mutableSetOf<Int>() // Track pages visited and updated this session

    init {
        viewModelScope.launch {
            // Load all locations from database into memory at startup
            val entities = weatherDao.getAllLocations()
            _locationsState.value = entities.map { toLocationWeather(it) }

            // Update only the first page (index 0) from API at startup
            if (_locationsState.value.isNotEmpty()) {
                updateCity(0)
                visitedCities.add(0) // Mark first page as visited and updated
            }
        }
    }

    fun addCity(cityName: String) {
        viewModelScope.launch {
            val existingIndex = _locationsState.value.indexOfFirst { it.city.name.equals(cityName, ignoreCase = true) }
            if (existingIndex != -1) {
                _uiState.value = UiState.NavigateToPage(existingIndex)
                if (existingIndex !in visitedCities) {
                    updateCity(existingIndex)
                    visitedCities.add(existingIndex)
                }
                return@launch
            }

            _uiState.value = UiState.Loading
            try {
                val city = networkClient.fetchCityCoordinates(cityName)
                if (city != null) {
                    val weatherResponse = networkClient.fetchWeather(city.latitude, city.longitude)
                    val newLocationWeather = LocationWeather(city, weatherResponse, false)
                    newLocationWeather.lastUpdated = System.currentTimeMillis()
                    val list = _locationsState.value.toMutableList()
                    list.add(newLocationWeather)
                    _locationsState.value = list
                    val insertedId = weatherDao.insertLocation(toEntity(newLocationWeather))
                    newLocationWeather.id = insertedId.toInt()
                    visitedCities.add(list.size - 1) // Mark as visited and updated
                    _uiState.value = UiState.Success("Added $cityName")
                } else {
                    _uiState.value = UiState.Error("City not found: $cityName")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown network error occurred")
            }
        }
    }

    fun addCurrentLocation(lat: Double, lon: Double) {
        viewModelScope.launch {
            val existingIndex = _locationsState.value.indexOfFirst { isNearby(it.city.latitude, it.city.longitude, lat, lon) }
            if (existingIndex != -1) {
                val list = _locationsState.value.toMutableList()
                val loc = list[existingIndex]
                if (!loc.isCurrentLocation) {
                    list[existingIndex] = loc.copy(isCurrentLocation = true)
                    weatherDao.updateLocation(toEntity(list[existingIndex]))
                }
                _locationsState.value = list
                _uiState.value = UiState.NavigateToPage(existingIndex)
                if (existingIndex !in visitedCities) {
                    updateCity(existingIndex)
                    visitedCities.add(existingIndex)
                }
                return@launch
            }

            _uiState.value = UiState.Loading
            val cityName = getCityNameFromCoordinates(lat, lon)
            val city = CityResult(cityName, lat, lon)

            // Set other current locations to false
            val list = _locationsState.value.toMutableList()
            for (i in list.indices) {
                if (list[i].isCurrentLocation) {
                    list[i] = list[i].copy(isCurrentLocation = false)
                    weatherDao.updateLocation(toEntity(list[i]))
                }
            }
            _locationsState.value = list

            try {
                val weatherResponse = networkClient.fetchWeather(lat, lon)
                val newLocationWeather = LocationWeather(city, weatherResponse, true)
                newLocationWeather.lastUpdated = System.currentTimeMillis()
                list.add(newLocationWeather)
                _locationsState.value = list
                val insertedId = weatherDao.insertLocation(toEntity(newLocationWeather))
                newLocationWeather.id = insertedId.toInt()
                visitedCities.add(list.size - 1) // Mark as visited and updated
                _uiState.value = UiState.Success("Added current location")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown network error occurred")
            }
        }
    }

    fun removeLocation(index: Int) {
        viewModelScope.launch {
            val list = _locationsState.value.toMutableList()
            if (index in list.indices) {
                val loc = list[index]
                if (loc.id != null) {
                    weatherDao.deleteLocationById(loc.id!!)
                }
                list.removeAt(index)
                _locationsState.value = list
                visitedCities.remove(index)
                // Adjust indices in visitedCities
                val newVisitedCities = mutableSetOf<Int>()
                visitedCities.forEach { oldIndex ->
                    if (oldIndex > index) {
                        newVisitedCities.add(oldIndex - 1)
                    } else if (oldIndex < index) {
                        newVisitedCities.add(oldIndex)
                    }
                }
                visitedCities.clear()
                visitedCities.addAll(newVisitedCities)
            }
        }
    }

    fun refreshLocation(index: Int) {
        viewModelScope.launch {
            updateCity(index)
            if (index !in visitedCities) {
                visitedCities.add(index)
            }
        }
    }

    fun onPageChanged(index: Int) {
        viewModelScope.launch {
            if (index !in visitedCities) {
                updateCity(index)
                visitedCities.add(index)
            }
        }
    }

    private suspend fun updateCity(index: Int) {
        val list = _locationsState.value.toMutableList()
        if (index !in list.indices) return

        val location = list[index]
        // Check if recently updated
        if (System.currentTimeMillis() - location.lastUpdated < 120000) {
            _uiState.value = UiState.Success("Already up to date")
            return
        }

        _uiState.value = UiState.Loading
        try {
            val weatherResponse = networkClient.fetchWeather(location.city.latitude, location.city.longitude)
            val updatedLocation = location.copy(weather = weatherResponse)
            updatedLocation.lastUpdated = System.currentTimeMillis()
            list[index] = updatedLocation
            _locationsState.value = list
            weatherDao.updateLocation(toEntity(updatedLocation))
            _uiState.value = UiState.Success("Updated ${location.city.name}")
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e.message ?: "Failed to refresh ${location.city.name}")
        }
    }

    private suspend fun getCityNameFromCoordinates(lat: Double, lon: Double): String {
        return withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(getApplication(), Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                addresses?.firstOrNull()?.locality ?: "Current Location"
            } catch (e: Exception) { "Unknown Location" }
        }
    }

    private fun isNearby(lat1: Double, lon1: Double, lat2: Double, lon2: Double, thresholdKm: Double = 10.0): Boolean {
        val R = 6371
        val dLat = deg2rad(lat2 - lat1)
        val dLon = deg2rad(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (R * c) < thresholdKm
    }

    private fun deg2rad(deg: Double): Double = deg * (Math.PI / 180)

    fun messageShown() { _uiState.value = UiState.Idle }

    override fun onCleared() {
        super.onCleared()
        networkClient.close()
    }

    private fun toEntity(lw: LocationWeather): LocationEntity {
        return LocationEntity(
            id = lw.id ?: 0,
            cityName = lw.city.name,
            latitude = lw.city.latitude,
            longitude = lw.city.longitude,
            isCurrentLocation = lw.isCurrentLocation,
            currentWeather = CurrentWeatherEntity(
                temperature = lw.weather.current.temperature,
                humidity = lw.weather.current.humidity,
                apparentTemperature = lw.weather.current.apparentTemperature,
                windSpeed = lw.weather.current.windSpeed,
                isDay = lw.weather.current.is_day,
                weathercode = lw.weather.current.weathercode,
                pressureMsl = lw.weather.current.pressure_msl
            ),
            dailyTime = lw.weather.daily.time,
            dailyTempMax = lw.weather.daily.temperatureMax,
            dailyTempMin = lw.weather.daily.temperatureMin,
            dailyWindMax = lw.weather.daily.wind_speed_10m_max,
            hourlyTime = lw.weather.hourly.time,
            hourlyTemp = lw.weather.hourly.temperature,
            hourlyHumidity = lw.weather.hourly.humidity,
            hourlyWindSpeed = lw.weather.hourly.windSpeed,
            lastUpdated = lw.lastUpdated
        )
    }

    private fun toLocationWeather(entity: LocationEntity): LocationWeather {
        val city = CityResult(entity.cityName, entity.latitude, entity.longitude)
        val current = CurrentWeather(
            temperature = entity.currentWeather.temperature,
            humidity = entity.currentWeather.humidity,
            apparentTemperature = entity.currentWeather.apparentTemperature,
            windSpeed = entity.currentWeather.windSpeed,
            is_day = entity.currentWeather.isDay,
            weathercode = entity.currentWeather.weathercode,
            pressure_msl = entity.currentWeather.pressureMsl
        )
        val daily = DailyForecast(
            time = entity.dailyTime,
            temperatureMax = entity.dailyTempMax,
            temperatureMin = entity.dailyTempMin,
            wind_speed_10m_max = entity.dailyWindMax
        )
        val hourly = HourlyForecast(
            time = entity.hourlyTime,
            temperature = entity.hourlyTemp,
            humidity = entity.hourlyHumidity,
            windSpeed = entity.hourlyWindSpeed
        )
        val weather = WeatherResponse(
            latitude = entity.latitude,
            longitude = entity.longitude,
            current = current,
            daily = daily,
            hourly = hourly
        )
        return LocationWeather(city, weather, entity.isCurrentLocation, entity.id, entity.lastUpdated)
    }
}
