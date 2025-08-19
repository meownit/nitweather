package com.example.weather2

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WeatherApp(viewModel: WeatherViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    var showAddCityDialog by remember { mutableStateOf(false) }
    val locations by viewModel.locationsState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { locations.size })
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Error -> {
                android.widget.Toast.makeText(context, state.message, android.widget.Toast.LENGTH_SHORT).show()
                viewModel.messageShown()
            }
            is UiState.NavigateToPage -> {
                scope.launch { pagerState.animateScrollToPage(state.page) }
                viewModel.messageShown()
            }
            is UiState.Success -> {
                android.widget.Toast.makeText(context, state.message, android.widget.Toast.LENGTH_SHORT).show()
                viewModel.messageShown()
            }
            else -> {}
        }
    }

    LaunchedEffect(locations.size) {
        if (locations.isNotEmpty() && uiState !is UiState.NavigateToPage) {
            pagerState.animateScrollToPage(locations.size - 1)
        }
    }

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (locations.isEmpty() && uiState !is UiState.Loading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No cities added yet.", style = MaterialTheme.typography.bodyLarge)
                    Text("Press the menu button to add one.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
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
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                LocationButton(viewModel = viewModel)
                if (pagerState.pageCount > 1) {
                    PagerIndicator(pagerState = pagerState, modifier = Modifier.align(Alignment.Center))
                }
            }

            AnimatedActionMenu(
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                onAddClick = { showAddCityDialog = true },
                onRemoveClick = {
                    if (locations.isNotEmpty()) {
                        viewModel.removeLocation(pagerState.currentPage)
                    }
                },
                onRefreshClick = {
                    if (locations.isNotEmpty()) {
                        viewModel.refreshLocation(pagerState.currentPage)
                    }
                },
                isActionEnabled = locations.isNotEmpty()
            )

            if (uiState is UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

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
fun AnimatedActionMenu(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onRefreshClick: () -> Unit,
    isActionEnabled: Boolean
) {
    var isMenuExpanded by remember { mutableStateOf(false) }

    val animatedBgColor by animateColorAsState(
        targetValue = if (isMenuExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f) else Color.Transparent,
        label = "MenuBackgroundColorAnimation"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = animatedBgColor,
        tonalElevation = if (isMenuExpanded) 0.dp else 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(0 .dp)
        ) {
            IconButton(onClick = { isMenuExpanded = !isMenuExpanded }) {
                AnimatedContent(
                    targetState = isMenuExpanded,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut() using SizeTransform(clip = false)
                    }, label = "MenuIconAnimation"
                ) { expanded ->
                    if (expanded) {
                        Icon(Icons.Default.Close, contentDescription = "Close Menu")
                    } else {
                        Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                    }
                }
            }

            AnimatedVisibility(
                visible = isMenuExpanded,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    IconButton(onClick = {
                        onAddClick()
                        isMenuExpanded = false
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add City")
                    }
                    IconButton(
                        onClick = {
                            if (isActionEnabled) onRemoveClick()
                            isMenuExpanded = false
                        },
                        enabled = isActionEnabled
                    ) {
                        Icon(
                            Icons.Default.Remove,
                            contentDescription = "Remove City",
                            tint = if (isActionEnabled) LocalContentColor.current else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = {
                            if (isActionEnabled) onRefreshClick()
                            isMenuExpanded = false
                        },
                        enabled = isActionEnabled
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Weather",
                            tint = if (isActionEnabled) LocalContentColor.current else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
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
                    } ?: android.widget.Toast.makeText(context, "Could not retrieve location.", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Failed to get location: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.widget.Toast.makeText(context, "Location permission not granted.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }) {
        Icon(Icons.Default.LocationOn, contentDescription = "Get Current Location")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.Center, modifier = modifier) {
        repeat(pagerState.pageCount) { iteration ->
            val color = if (pagerState.currentPage == iteration) Color(0xFFFFFFFF) else Color(0xFF999999)
            Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(color).size(10.dp))
        }
    }
}


@Composable
fun WeatherPage(locationWeather: LocationWeather, modifier: Modifier = Modifier) {
    val current = locationWeather.weather.current
    val daily = locationWeather.weather.daily
    val hourly = locationWeather.weather.hourly

    val isDay = current.is_day == 1
    val textColor = Color.White
    val backgroundGradient = if (isDay) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF44C9FF), Color(0xFF058AFF)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFF101040), Color(0xFF303040)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
        )
    }
    val secondaryBackgroundColor = if (isDay) {
        Color(0x4D87CEFA) // Light sky blue with 30% opacity
    } else {
        Color(0x4D2F4F4F) // Dark slate gray with 30% opacity
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(backgroundGradient)
            .padding(horizontal = 16.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = locationWeather.city.name,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 32.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    if (locationWeather.isCurrentLocation) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Current Location",
                            modifier = Modifier.size(28.dp).padding(start = 8.dp),
                            tint = textColor
                        )
                    }
                }
                Text(
                    text = "${current.temperature}°C",
                    style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Light),
                    color = textColor,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color.Transparent, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getWeatherCondition(current.weathercode),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherDetailItem(icon = Icons.Default.Thermostat, label = "Feels Like", value = "${current.apparentTemperature}°C", textColor = textColor)
            WeatherDetailItem(icon = Icons.Default.WaterDrop, label = "Humidity", value = "${current.humidity}%", textColor = textColor)
            WeatherDetailItem(icon = Icons.Default.Air, label = "Wind", value = "${current.windSpeed} km/h", textColor = textColor)
            WeatherDetailItem(icon = Icons.Default.Cloud, label = "Pressure", value = "${current.pressure_msl} hPa", textColor = textColor)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Spacer(modifier = Modifier.height(16.dp))

        HourlyForecastSection(hourlyForecast = hourly, isDay = isDay)
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.height(16.dp))

        ForecastSection(dailyForecast = daily, hourlyForecast = hourly, isDay = isDay)
    }
}

@Composable
fun WeatherDetailItem(icon: ImageVector, label: String, value: String, textColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = textColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = textColor)
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = textColor)
    }
}

@Composable
fun HourlyForecastSection(hourlyForecast: HourlyForecast, isDay: Boolean) {
    val scrollState = rememberScrollState()
    val hoursToShow = remember(hourlyForecast) { parseHourlyForecast(hourlyForecast) }
    val textColor = Color.White
    val secondaryBackgroundColor = if (isDay) {
        Color(0x3DbFbFbF) // Light sky blue with 30% opacity
    } else {
        Color(0x2D5F5F5F) // Dark slate gray with 30% opacity
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .background(secondaryBackgroundColor)
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "Hourly Forecast",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = textColor
                )
                Row(
                    modifier = Modifier
                        .horizontalScroll(scrollState)
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            top = 0.dp,
                            bottom = 0.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hoursToShow.forEach { item ->
                        key(item.time) {
                            HourlyForecastItem(
                                time = item.time,
                                temperature = item.temperature,
                                humidity = item.humidity,
                                windSpeed = item.windSpeed,
                                textColor = textColor,
                                secondaryBackgroundColor = secondaryBackgroundColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyForecastItem(time: String, temperature: Double, humidity: Int, windSpeed: Double, textColor: Color, secondaryBackgroundColor: Color) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .width(100.dp)
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Thermostat,
                    contentDescription = "Temperature",
                    modifier = Modifier.size(16.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${temperature}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Humidity",
                    modifier = Modifier.size(16.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$humidity%",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = "Wind Speed",
                    modifier = Modifier.size(16.dp),
                    tint = textColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$windSpeed km/h",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun ForecastSection(dailyForecast: DailyForecast, hourlyForecast: HourlyForecast, isDay: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val daysToShow = if (expanded) dailyForecast.time.size else 3
    val textColor = Color.White
    val secondaryBackgroundColor = if (isDay) {
        Color(0x3DbFbFbF) // Light sky blue with 30% opacity
    } else {
        Color(0x2D5F5F5F) // Dark slate gray with 30% opacity
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = Modifier
                    .background(secondaryBackgroundColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Forecast",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = textColor
                )
                for (i in 0 until dailyForecast.time.size) {
                    AnimatedVisibility(visible = i < daysToShow) {
                        ForecastItem(
                            date = dailyForecast.time[i],
                            maxTemp = dailyForecast.temperatureMax[i],
                            minTemp = dailyForecast.temperatureMin[i],
                            humidity = calculateDailyHumidityAverage(dailyForecast.time[i], hourlyForecast),
                            windSpeed = dailyForecast.wind_speed_10m_max.getOrNull(i) ?: 0.0,
                            textColor = textColor
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = textColor.copy(alpha = 0.5f)
                )

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
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun ForecastItem(date: String, maxTemp: Double, minTemp: Double, humidity: Int, windSpeed: Double, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDayOfWeek(date),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
            Text(
                text = "Humidity: $humidity%",
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
            Text(
                text = "Wind: $windSpeed km/h",
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            Text(
                text = "${maxTemp}°",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${minTemp}°",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor.copy(alpha = 0.7f)
            )
        }
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
        confirmButton = { Button(onClick = { if (text.isNotBlank()) { onConfirm(text) } }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}