package com.example.weather2

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
<<<<<<< HEAD
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
=======
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
>>>>>>> 5b2d031 (detailed information in weather)
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
<<<<<<< HEAD
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
=======
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
>>>>>>> 5b2d031 (detailed information in weather)
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
<<<<<<< HEAD
=======
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
>>>>>>> 5b2d031 (detailed information in weather)
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather2.ui.theme.Weather2Theme
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
<<<<<<< HEAD

// --- DATA CLASSES FOR API RESPONSES ---

// Represents the response from the Geocoding API
@Serializable
data class GeocodingResponse(
    val results: List<CityResult>? = null
)

// Represents a single city result from the Geocoding API
@Serializable
data class CityResult(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

// Represents the response from the Weather Forecast API
@Serializable
data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("current")
    val current: CurrentWeather
)

// Represents the current weather data block
@Serializable
data class CurrentWeather(
    @SerialName("temperature_2m")
    val temperature: Double
)

// A combined data class to hold both city info and its weather for the UI
data class LocationWeather(
    val city: CityResult,
    val weather: WeatherResponse
)
=======
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

// --- DATA CLASSES ---
@Serializable data class GeocodingResponse(val results: List<CityResult>? = null)
@Serializable data class CityResult(val name: String, val latitude: Double, val longitude: Double)

// Updated to include daily forecast data
@Serializable data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    @SerialName("current")
    val current: CurrentWeather,
    @SerialName("daily")
    val daily: DailyForecast
)

// Updated to include more current weather details
@Serializable data class CurrentWeather(
    @SerialName("temperature_2m") val temperature: Double,
    @SerialName("relative_humidity_2m") val humidity: Int,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("wind_speed_10m") val windSpeed: Double
)

// New data class for the daily forecast
@Serializable data class DailyForecast(
    val time: List<String>,
    @SerialName("temperature_2m_max") val temperatureMax: List<Double>,
    @SerialName("temperature_2m_min") val temperatureMin: List<Double>
)

@Serializable
data class LocationWeather(val city: CityResult, val weather: WeatherResponse, val isCurrentLocation: Boolean = false)
>>>>>>> 5b2d031 (detailed information in weather)

// --- VIEWMODEL ---
class WeatherViewModel : ViewModel() {

    // Holds the list of locations the user has added.
    private val _locationsState = MutableStateFlow<List<LocationWeather>>(emptyList())
    val locationsState = _locationsState.asStateFlow()

    // Holds the transient UI state, like loading indicators or error messages.
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Ktor HTTP client
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true // Helps parse slightly malformed JSON
            })
        }
    }

    // Searches for a city and fetches its weather.
    fun addCity(cityName: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // 1. Geocoding request to find the city's coordinates
                val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${cityName.trim()}&count=1"
                val geoResponse: GeocodingResponse = client.get(geoUrl).body()
                val city = geoResponse.results?.firstOrNull()

                if (city != null) {
                    // 2. Weather forecast request using the found coordinates
                    val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}&current=temperature_2m"
                    val weatherResponse: WeatherResponse = client.get(weatherUrl).body()

                    // 3. Create the combined data object and add it to our list
                    val newLocationWeather = LocationWeather(city, weatherResponse)
                    _locationsState.value += newLocationWeather // Appends the new location to the list
                    _uiState.value = UiState.Success("Added ${city.name}")
                } else {
                    _uiState.value = UiState.Error("City not found: $cityName")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown network error occurred")
            }
        }
    }

    // Resets the UI state, e.g., after an error message has been shown.
    fun messageShown() {
        _uiState.value = UiState.Idle
    }

<<<<<<< HEAD
    override fun onCleared() {
        super.onCleared()
        client.close()
    }
=======
    private fun fetchWeatherForCoordinates(city: CityResult, isCurrentLocation: Boolean, addNew: Boolean) {
        viewModelScope.launch {
            try {
                // Updated URL to fetch more current data and the daily forecast
                val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}&current=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m&daily=temperature_2m_max,temperature_2m_min&timezone=auto"
                val weatherResponse: WeatherResponse = client.get(weatherUrl).body()
                val newLocationWeather = LocationWeather(city, weatherResponse, isCurrentLocation)

                if (addNew) {
                    _locationsState.value += newLocationWeather
                    saveLocations()
                } else {
                    val index = _locationsState.value.indexOfFirst { it.city.name == city.name }
                    if (index != -1) {
                        val updatedList = _locationsState.value.toMutableList()
                        updatedList[index] = newLocationWeather
                        _locationsState.value = updatedList
                        saveLocations()
                    }
                }
                _uiState.value = UiState.Success("Updated ${city.name}")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown network error occurred")
            }
        }
    }

    private fun saveLocations() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = json.encodeToString(_locationsState.value)
                getApplication<Application>().applicationContext.openFileOutput(fileName, Context.MODE_PRIVATE).use {
                    it.write(jsonString.toByteArray())
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Failed to save locations", e)
            }
        }
    }

    private fun loadLocations() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = getApplication<Application>().applicationContext.openFileInput(fileName).bufferedReader().useLines { lines ->
                    lines.fold("") { some, text -> "$some\n$text" }
                }
                val locations = json.decodeFromString<List<LocationWeather>>(jsonString)
                _locationsState.value = locations
                locations.forEach { location ->
                    fetchWeatherForCoordinates(location.city, location.isCurrentLocation, addNew = false)
                }
            } catch (e: FileNotFoundException) {
                Log.d("WeatherViewModel", "No saved locations file found.")
                _locationsState.value = emptyList()
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Failed to load locations", e)
                _locationsState.value = emptyList()
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
    override fun onCleared() { super.onCleared(); client.close() }
>>>>>>> 5b2d031 (detailed information in weather)
}

// --- UI STATE ---
sealed interface UiState {
    data object Idle : UiState
    data object Loading : UiState
    data class Success(val message: String) : UiState
    data class Error(val message: String) : UiState
}

// --- MAIN ACTIVITY ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Weather2Theme {
                WeatherApp()
            }
        }
    }
}

// --- COMPOSABLES ---

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeatherApp(viewModel: WeatherViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    // State for managing the "Add City" dialog visibility
    var showAddCityDialog by remember { mutableStateOf(false) }

    // Collect states from the ViewModel
    val locations by viewModel.locationsState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Pager state to control the slidable pages
    val pagerState = rememberPagerState(pageCount = { locations.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // This effect listens for changes in the UI state to show messages (Toasts)
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.messageShown()
            }
            is UiState.Success -> {
                // You could optionally show a success toast here
                viewModel.messageShown()
            }
            else -> {}
        }
    }

    // This effect scrolls to the new page when a city is added
    LaunchedEffect(locations.size) {
        if (locations.isNotEmpty()) {
            pagerState.animateScrollToPage(locations.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather") },
                actions = {
                    IconButton(onClick = { showAddCityDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add City")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            if (uiState is UiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (locations.isEmpty()) {
                // Show a placeholder message when no cities are added
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No cities added yet.", style = MaterialTheme.typography.bodyLarge)
                    Text("Press the '+' button to add one.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
<<<<<<< HEAD
                // The HorizontalPager provides the slidable interface
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    WeatherPage(locationWeather = locations[page])
=======
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    if (page < locations.size) {
                        WeatherPage(
                            locationWeather = locations[page],
                            modifier = Modifier.graphicsLayer {
                                val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                                alpha = 1f - (pageOffset * 0.5f).coerceIn(0f, 1f)
                            }
                        )
                    }
>>>>>>> 5b2d031 (detailed information in weather)
                }
            }
        }
    }

    // Show the "Add City" dialog when `showAddCityDialog` is true
    if (showAddCityDialog) {
        AddCityDialog(
            onDismiss = { showAddCityDialog = false },
            onConfirm = { cityName ->
                viewModel.addCity(cityName)
                showAddCityDialog = false
            }
        )
    }
}

@Composable
<<<<<<< HEAD
fun WeatherPage(locationWeather: LocationWeather) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
=======
fun LocationButton(viewModel: WeatherViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    IconButton(onClick = {
        scope.launch {
            if (checkLocationPermission(context)) {
                try {
                    val location = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token
                    ).await()
                    location?.let {
                        viewModel.addCurrentLocation(it.latitude, it.longitude)
                    } ?: Toast.makeText(context, "Could not retrieve location.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Location permission not granted.", Toast.LENGTH_SHORT).show()
            }
        }
    }) {
        Icon(Icons.Default.LocationOn, contentDescription = "Get Current Location")
    }
}

fun checkLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.Center, modifier = modifier) {
        repeat(pagerState.pageCount) { iteration ->
            val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
            Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(color).size(10.dp))
        }
    }
}

// --- MODIFIED WEATHER PAGE & NEW COMPOSABLES ---

@Composable
fun WeatherPage(locationWeather: LocationWeather, modifier: Modifier = Modifier) {
    val current = locationWeather.weather.current
    val daily = locationWeather.weather.daily

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()) // Make the page scrollable
            .padding(horizontal = 16.dp, vertical = 64.dp), // Adjust padding for top bar
>>>>>>> 5b2d031 (detailed information in weather)
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
<<<<<<< HEAD
        Text(
            text = locationWeather.city.name,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "(${locationWeather.city.latitude}, ${locationWeather.city.longitude})",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
            text = "${locationWeather.weather.current.temperature}°C",
=======
        // City Name
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = locationWeather.city.name,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )
            if (locationWeather.isCurrentLocation) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Current Location",
                    modifier = Modifier.size(40.dp).padding(start = 8.dp)
                )
            }
        }

        // Main Temperature
        Text(
            text = "${current.temperature}°C",
>>>>>>> 5b2d031 (detailed information in weather)
            style = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Light)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Current Weather Details Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherDetailItem(icon = Icons.Default.Thermostat, label = "Feels Like", value = "${current.apparentTemperature}°C")
            WeatherDetailItem(icon = Icons.Default.WaterDrop, label = "Humidity", value = "${current.humidity}%")
            WeatherDetailItem(icon = Icons.Default.Air, label = "Wind", value = "${current.windSpeed} km/h")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // Daily Forecast Section
        ForecastSection(dailyForecast = daily)
    }
}

@Composable
fun WeatherDetailItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ForecastSection(dailyForecast: DailyForecast) {
    var expanded by remember { mutableStateOf(false) }
    val daysToShow = if (expanded) dailyForecast.time.size else 3

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Forecast",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                // Animate the visibility of forecast items
                for (i in 0 until dailyForecast.time.size) {
                    AnimatedVisibility(visible = i < daysToShow) {
                        ForecastItem(
                            date = dailyForecast.time[i],
                            maxTemp = dailyForecast.temperatureMax[i],
                            minTemp = dailyForecast.temperatureMin[i]
                        )
                    }
                }

                Divider(modifier = Modifier.padding(horizontal = 16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expanded) "Show Less" else "Show More",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }
        }
    }
}

@Composable
fun ForecastItem(date: String, maxTemp: Double, minTemp: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatDayOfWeek(date),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            Text(
                text = "${maxTemp}°",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${minTemp}°",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }
    }
}

// Helper function to format date string to day of the week (e.g., "Mon")
private fun formatDayOfWeek(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString // Fallback to original string if parsing fails
    }
}


@Composable
fun AddCityDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add a New City") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("City Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
