package com.example.weather2

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
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
                // The HorizontalPager provides the slidable interface
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    WeatherPage(locationWeather = locations[page])
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
fun WeatherPage(locationWeather: LocationWeather) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
            style = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Light)
        )
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
