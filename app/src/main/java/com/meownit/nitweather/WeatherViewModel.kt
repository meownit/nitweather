package com.meownit.nitweather

import android.app.Application
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
    private val storageManager = StorageManager(application, _locationsState, viewModelScope)

    init {
        storageManager.loadLocations { city, isCurrentLocation ->
            fetchWeatherForCoordinates(city, isCurrentLocation, addNew = false)
        }
    }

    fun addCity(cityName: String) {
        viewModelScope.launch {
            val existingIndex = _locationsState.value.indexOfFirst { it.city.name.equals(cityName, ignoreCase = true) }
            if (existingIndex != -1) {
                _uiState.value = UiState.NavigateToPage(existingIndex)
                return@launch
            }

            _uiState.value = UiState.Loading
            try {
                val city = networkClient.fetchCityCoordinates(cityName)
                if (city != null) {
                    fetchWeatherForCoordinates(city, isCurrentLocation = false, addNew = true)
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
                _uiState.value = UiState.NavigateToPage(existingIndex)
                return@launch
            }

            _uiState.value = UiState.Loading
            val cityName = getCityNameFromCoordinates(lat, lon)
            val city = CityResult(cityName, lat, lon)
            fetchWeatherForCoordinates(city, isCurrentLocation = true, addNew = true)
        }
    }

    fun removeLocation(index: Int) {
        viewModelScope.launch {
            val currentList = _locationsState.value.toMutableList()
            if (index in currentList.indices) {
                currentList.removeAt(index)
                _locationsState.value = currentList
                storageManager.saveLocations()
            }
        }
    }

    fun refreshLocation(index: Int) {
        viewModelScope.launch {
            val currentList = _locationsState.value
            if (index in currentList.indices) {
                _uiState.value = UiState.Loading
                val location = currentList[index]
                try {
                    val weatherResponse = networkClient.fetchWeather(location.city.latitude, location.city.longitude)
                    val updatedLocation = LocationWeather(location.city, weatherResponse, location.isCurrentLocation)
                    val updatedList = _locationsState.value.toMutableList()
                    updatedList[index] = updatedLocation
                    _locationsState.value = updatedList
                    storageManager.saveLocations()
                    _uiState.value = UiState.Success("Updated ${location.city.name}")
                } catch (e: Exception) {
                    _uiState.value = UiState.Error(e.message ?: "Failed to refresh ${location.city.name}")
                }
            }
        }
    }

    private fun fetchWeatherForCoordinates(city: CityResult, isCurrentLocation: Boolean, addNew: Boolean) {
        viewModelScope.launch {
            try {
                val weatherResponse = networkClient.fetchWeather(city.latitude, city.longitude)
                val newLocationWeather = LocationWeather(city, weatherResponse, isCurrentLocation)

                if (addNew) {
                    _locationsState.value += newLocationWeather
                    storageManager.saveLocations()
                } else {
                    val index = _locationsState.value.indexOfFirst { it.city.name == city.name }
                    if (index != -1) {
                        val updatedList = _locationsState.value.toMutableList()
                        updatedList[index] = newLocationWeather
                        _locationsState.value = updatedList
                        storageManager.saveLocations()
                    }
                }
                _uiState.value = UiState.Success("Updated ${city.name}")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown network error occurred")
            }
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
}