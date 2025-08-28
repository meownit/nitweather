package com.meownit.nitweather

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.io.FileNotFoundException

class StorageManager(
    private val application: Application,
    private val locationsState: MutableStateFlow<List<LocationWeather>>,
    private val scope: CoroutineScope
) {
    private val json = Json { prettyPrint = true }
    private val fileName = "locations.json"

    fun saveLocations() {
        scope.launch(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(serializer(), locationsState.value)
                application.applicationContext.openFileOutput(fileName, android.content.Context.MODE_PRIVATE).use {
                    it.write(jsonString.toByteArray())
                }
            } catch (e: Exception) {
                Log.e("StorageManager", "Failed to save locations", e)
            }
        }
    }

    fun loadLocations(onLocationLoaded: (CityResult, Boolean) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val jsonString = application.applicationContext.openFileInput(fileName).bufferedReader().useLines { lines ->
                    lines.fold("") { some, text -> "$some\n$text" }
                }
                val locations = json.decodeFromString<List<LocationWeather>>(jsonString)
                locationsState.value = locations
                locations.forEach { location ->
                    onLocationLoaded(location.city, location.isCurrentLocation)
                }
            } catch (e: FileNotFoundException) {
                Log.d("StorageManager", "No saved locations file found.")
                locationsState.value = emptyList()
            } catch (e: Exception) {
                Log.e("StorageManager", "Failed to load locations", e)
                locationsState.value = emptyList()
            }
        }
    }
}