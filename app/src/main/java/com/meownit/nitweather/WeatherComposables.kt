package com.meownit.nitweather

import android.app.Activity
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
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
    // Collect the new state here
    val isInitialLoadComplete by viewModel.isInitialLoadComplete.collectAsState()
    val pagerState = rememberPagerState(pageCount = { locations.size })
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE) }

    // Track the current page to trigger updates only when necessary
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
        prefs.edit().putInt("last_page", pagerState.currentPage).apply()
    }

    // Assuming you have this logic to handle the background gradient in WeatherPage
    val isDay = remember {
        derivedStateOf {
            if (locations.isNotEmpty() && pagerState.currentPage < locations.size) {
                locations[pagerState.currentPage].weather.current.is_day == 1
            } else {
                true // Default to day mode if no locations are present
            }
        }
    }.value

    val dayTopColor = Color(0xFF44C9FF)
    val dayBottomColor = Color(0xFF058AFF)
    val nightTopColor = Color(0xFF101040)
    val nightBottomColor = Color(0xFF202040)

    val topColor by animateColorAsState(if (isDay) dayTopColor else nightTopColor)
    val bottomColor by animateColorAsState(if (isDay) dayBottomColor else nightBottomColor)

    val backgroundGradient = Brush.linearGradient(
        colors = listOf(topColor, bottomColor),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY)
    )

    // Use a SideEffect to manage the status bar icons
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDay
        }
    }

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

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            // This is the updated condition
            if (isInitialLoadComplete && locations.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                Text(
                    text = "No cities added yet.",
                    // Apply the white color and shadow style
                    color = Color.White,
                    style = shadowedTextStyle(isDay = isDay, style = MaterialTheme.typography.bodyLarge)
                )
                Text(
                    text = "Press the menu button to add one.",
                    // Apply the white color and shadow style here as well
                    color = Color.White,
                    style = shadowedTextStyle(isDay = isDay, style = MaterialTheme.typography.bodyMedium)
                )
            }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().padding(innerPadding)
                ) { page ->
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

// Replace your top Row with this Box
            Box(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Left-aligned item
                LocationButton(
                    viewModel = viewModel,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Center-aligned item
                if (pagerState.pageCount > 1) {
                    PagerIndicator(
                        pagerState = pagerState,
                        modifier = Modifier
                            .align(Alignment.TopCenter) // This is now in the correct context
                            .padding(top = 16.dp)
                    )
                }

                // Right-aligned item
                AnimatedActionMenu(
                    modifier = Modifier.align(Alignment.TopEnd),
                    onAddClick = { showAddCityDialog = true },
                    onRemoveClick = {
                        if (locations.isNotEmpty() && pagerState.currentPage < locations.size) {
                            val currentPage = pagerState.currentPage
                            viewModel.removeLocation(currentPage)
                            // Adjust pager state after removal
                            scope.launch {
                                val newSize = locations.size - 1
                                if (newSize > 0) {
                                    val newPage = when {
                                        currentPage < newSize -> currentPage
                                        else -> newSize - 1
                                    }
                                    pagerState.animateScrollToPage(newPage)
                                }
                            }
                        }
                    },
                    onRefreshClick = {
                        if (locations.isNotEmpty() && pagerState.currentPage < locations.size) {
                            viewModel.refreshLocation(pagerState.currentPage)
                        }
                    },
                    isActionEnabled = locations.isNotEmpty(),
                    locationWeather = if (locations.isNotEmpty() && pagerState.currentPage < locations.size) {
                        locations[pagerState.currentPage]
                    } else null
                )
            }

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
    isActionEnabled: Boolean,
    locationWeather: LocationWeather?
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    val isDay = locationWeather?.weather?.current?.is_day?.let { it == 1 } ?: true

    // Animation for background color
    val animatedBgColor by animateColorAsState(
        targetValue = if (isMenuExpanded) {
            when (isDay) {
                true -> Color(0xFF333333).copy(alpha = 1F)
                else -> Color(0xFF555577).copy(alpha = 1F)
            }
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 300),
        label = "MenuBackgroundColorAnimation"
    )

    // Animation for icon rotation
    val iconRotation by animateFloatAsState(
        targetValue = if (isMenuExpanded) 180f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "MenuIconRotation"
    )

    // Animation for icon scale (optional bounce effect)
    val iconScale by animateFloatAsState(
        targetValue = if (isMenuExpanded) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "MenuIconScale"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = animatedBgColor,
        tonalElevation = if (isMenuExpanded) 8.dp else 0.dp
    ) {
        // A Box allows us to align the collapsed and expanded states differently
        Box(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            // STATE 1: Menu is COLLAPSED (Center-aligned icon with animations)
            AnimatedVisibility(
                visible = !isMenuExpanded,
                enter = fadeIn(animationSpec = tween(durationMillis = 200, delayMillis = 150)) +
                        scaleIn(animationSpec = tween(durationMillis = 200, delayMillis = 150)),
                exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                        scaleOut(animationSpec = tween(durationMillis = 150)),
                modifier = Modifier.align(Alignment.Center)
            ) {
                IconButton(
                    onClick = { isMenuExpanded = true },
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = iconRotation
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                ) {
                    ShadowedIcon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Menu",
                        tint = Color.White,
                        isDay = isDay
                    )
                }
            }

            // STATE 2: Menu is EXPANDED (Left-aligned column of actions)
            AnimatedVisibility(
                visible = isMenuExpanded,
                enter = expandVertically(
                    expandFrom = Alignment.Top,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 300, delayMillis = 100)
                ),
                exit = shrinkVertically(
                    shrinkTowards = Alignment.Top,
                    animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 200)
                ),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                ) {
                    // Row 1: Close action with hover animation
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isMenuExpanded = false
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .animateContentSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShadowedIcon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Icon",
                            tint = Color.White,
                            isDay = isDay
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Close",
                            color = Color.White,
                            style = shadowedTextStyle(isDay, MaterialTheme.typography.bodyLarge)
                        )
                    }

                    // Row 2: Add action with staggered animation
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onAddClick()
                                isMenuExpanded = false
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .animateContentSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShadowedIcon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Icon",
                            tint = Color.White,
                            isDay = isDay
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Add",
                            color = Color.White,
                            style = shadowedTextStyle(isDay, MaterialTheme.typography.bodyLarge)
                        )
                    }

                    // Row 3: Remove action
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = isActionEnabled) {
                                if (isActionEnabled) onRemoveClick()
                                isMenuExpanded = false
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .animateContentSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val color = if (isActionEnabled) Color.White else Color.Gray
                        ShadowedIcon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Remove Icon",
                            tint = color,
                            isDay = isDay
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Remove",
                            color = color,
                            style = shadowedTextStyle(isDay, MaterialTheme.typography.bodyLarge)
                        )
                    }

                    // Row 4: Refresh action
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = isActionEnabled) {
                                if (isActionEnabled) onRefreshClick()
                                isMenuExpanded = false
                            }
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                            .animateContentSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val color = if (isActionEnabled) Color.White else Color.Gray
                        ShadowedIcon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Icon",
                            tint = color,
                            isDay = isDay
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "Refresh",
                            color = color,
                            style = shadowedTextStyle(isDay, MaterialTheme.typography.bodyLarge)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationButton(viewModel: WeatherViewModel, modifier: Modifier = Modifier) {
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
    },
        modifier = modifier,
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = Color.White,
            containerColor = Color.Transparent
        )) {
        Icon(Icons.Default.LocationOn, contentDescription = "Get Current Location")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PagerIndicator(pagerState: PagerState, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.Center, modifier = modifier) {
        repeat(pagerState.pageCount) { iteration ->
            val color = if (pagerState.currentPage == iteration) Color(0xFFFFFFFF) else Color(0x88FFFFFF)
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
    val secondaryBackgroundColor = if (isDay) {
        Color(0x4D87CEFA) // Light sky blue with 30% opacity
    } else {
        Color(0x4D2F4F4F) // Dark slate gray with 30% opacity
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 92.dp, bottom = 16.dp),
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
                        style = shadowedTextStyle(
                            isDay = isDay,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                        ),
                        color = textColor,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                    if (locationWeather.isCurrentLocation) {
                        ShadowedIcon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Current Location",
                            modifier = Modifier.size(28.dp).padding(start = 8.dp),
                            tint = textColor,
                            isDay = isDay
                        )
                    }
                }
                Text(
                    text = "${current.temperature}°C",
                    style = shadowedTextStyle(
                        isDay = isDay,
                        style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Light)
                    ),
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
                AsyncImage(
                    model = getWeatherIconResource(current.weathercode, isDay),
                    contentDescription = getWeatherCondition(current.weathercode),
                    modifier = Modifier.size(300.dp).padding(end = 16.dp),
                    placeholder = painterResource(R.drawable.cloudy_day),
                    error = painterResource(R.drawable.cloudy_day)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            WeatherDetailItem(icon = Icons.Default.Thermostat, label = "Feels Like", value = "${current.apparentTemperature}°C", textColor = textColor, isDay = isDay)
            WeatherDetailItem(icon = Icons.Default.WaterDrop, label = "Humidity", value = "${current.humidity}%", textColor = textColor, isDay = isDay)
            WeatherDetailItem(icon = Icons.Default.Air, label = "Wind", value = "${current.windSpeed} km/h", textColor = textColor, isDay = isDay)
            WeatherDetailItem(icon = Icons.Default.Cloud, label = "Pressure", value = "${current.pressure_msl} hPa", textColor = textColor, isDay = isDay)
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
fun WeatherDetailItem(icon: ImageVector, label: String, value: String, textColor: Color, isDay: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ShadowedIcon(imageVector = icon, contentDescription = label, modifier = Modifier.size(24.dp), tint = textColor, isDay = isDay)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = shadowedTextStyle(
                isDay = isDay,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            ),
            color = textColor
        )
        Text(
            text = label,
            style = shadowedTextStyle(
                isDay = isDay,
                style = MaterialTheme.typography.bodySmall
            ),
            color = textColor
        )
    }
}

@Composable
fun HourlyForecastSection(hourlyForecast: HourlyForecast, isDay: Boolean) {
    val scrollState = rememberScrollState()
    val hoursToShow = remember(hourlyForecast) { parseHourlyForecast(hourlyForecast) }
    val textColor = Color.White
    val secondaryBackgroundColor = if (isDay) {
        Color(0x2DFFFFFF)
    } else {
        Color(0x2D5F5F5F)
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
                    text = "Hourly Forecast",
                    style = shadowedTextStyle(
                        isDay = isDay,
                        style = MaterialTheme.typography.titleMedium
                    ),
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
                                secondaryBackgroundColor = secondaryBackgroundColor,
                                isDay = isDay
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyForecastItem(time: String, temperature: Double, humidity: Int, windSpeed: Double, textColor: Color, secondaryBackgroundColor: Color, isDay: Boolean) {
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
                style = shadowedTextStyle(
                    isDay = isDay,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                ),
                color = textColor
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShadowedIcon(
                    imageVector = Icons.Default.Thermostat,
                    contentDescription = "Temperature",
                    modifier = Modifier.size(16.dp),
                    tint = textColor,
                    isDay = isDay
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${temperature}°C",
                    style = shadowedTextStyle(
                        isDay = isDay,
                        style = MaterialTheme.typography.bodySmall
                    ),
                    color = textColor
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShadowedIcon(
                    imageVector = Icons.Default.WaterDrop,
                    contentDescription = "Humidity",
                    modifier = Modifier.size(16.dp),
                    tint = textColor,
                    isDay = isDay
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$humidity%",
                    style = shadowedTextStyle(
                        isDay = isDay,
                        style = MaterialTheme.typography.bodySmall
                    ),
                    color = textColor
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShadowedIcon(
                    imageVector = Icons.Default.Air,
                    contentDescription = "Wind Speed",
                    modifier = Modifier.size(16.dp),
                    tint = textColor,
                    isDay = isDay
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$windSpeed km/h",
                    style = shadowedTextStyle(
                        isDay = isDay,
                        style = MaterialTheme.typography.bodySmall
                    ),
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
        Color(0x2DFFFFFF)
    } else {
        Color(0x2D5F5F5F)
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
                    style = shadowedTextStyle(
                        isDay = isDay,
                        style = MaterialTheme.typography.titleMedium
                    ),
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
                            textColor = textColor,
                            isDay = isDay
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
                        style = shadowedTextStyle(
                            isDay = isDay,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        ),
                        color = textColor
                    )
                    ShadowedIcon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = textColor,
                        isDay = isDay
                    )
                }
            }
        }
    }
}

@Composable
fun ForecastItem(date: String, maxTemp: Double, minTemp: Double, humidity: Int, windSpeed: Double, textColor: Color, isDay: Boolean) {
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
                style = shadowedTextStyle(
                    isDay = isDay,
                    style = MaterialTheme.typography.bodyLarge
                ),
                color = textColor
            )
            Text(
                text = "Humidity: $humidity%",
                style = shadowedTextStyle(
                    isDay = isDay,
                    style = MaterialTheme.typography.bodySmall
                ),
                color = textColor
            )
            Text(
                text = "Wind: $windSpeed km/h",
                style = shadowedTextStyle(
                    isDay = isDay,
                    style = MaterialTheme.typography.bodySmall
                ),
                color = textColor
            )
        }
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
            Text(
                text = "${maxTemp}°",
                style = shadowedTextStyle(
                    isDay = isDay,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                ),
                color = textColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${minTemp}°",
                style = shadowedTextStyle(
                    isDay = isDay,
                    style = MaterialTheme.typography.bodyLarge
                ),
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

@Composable
fun ShadowedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color,
    isDay: Boolean
) {
    if (isDay) {
        Box(modifier = modifier) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.offset(x = 1.dp, y = 1.dp),
                tint = Color.Black.copy(alpha = 0.2f)
            )
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint
            )
        }
    } else {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint
        )
    }
}

fun shadowedTextStyle(isDay: Boolean, style: TextStyle): TextStyle {
    return if (isDay) {
        style.copy(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.2f),
                offset = Offset(4f, 4f),
                blurRadius = 4f
            )
        )
    } else {
        style
    }
}
