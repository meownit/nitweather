package com.example.weather2

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import kotlin.math.absoluteValue

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
        tonalElevation = if (isMenuExpanded) 4.dp else 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.padding(0.dp)
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
            val color = if (pagerState.currentPage == iteration) Color.LightGray else Color.DarkGray
            Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(color).size(10.dp))
        }
    }
}

@Composable
fun WeatherPage(locationWeather: LocationWeather, modifier: Modifier = Modifier) {
    val current = locationWeather.weather.current
    val daily = locationWeather.weather.daily

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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

        Text(
            text = "${current.temperature}°C",
            style = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Light)
        )

        Spacer(modifier = Modifier.height(24.dp))

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