package com.meownit.nitweather

import android.content.Context
import androidx.room.*

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    @ColumnInfo(name = "city_name") val cityName: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "is_current_location") val isCurrentLocation: Boolean,

    // Current Weather
    @Embedded(prefix = "current_")
    val currentWeather: CurrentWeatherEntity,

    // Daily Forecast
    @ColumnInfo(name = "daily_time") val dailyTime: List<String>,
    @ColumnInfo(name = "daily_temp_max") val dailyTempMax: List<Double>,
    @ColumnInfo(name = "daily_temp_min") val dailyTempMin: List<Double>,
    @ColumnInfo(name = "daily_wind_max") val dailyWindMax: List<Double>,

    // Hourly Forecast
    @ColumnInfo(name = "hourly_time") val hourlyTime: List<String>,
    @ColumnInfo(name = "hourly_temp") val hourlyTemp: List<Double>,
    @ColumnInfo(name = "hourly_humidity") val hourlyHumidity: List<Int>,
    @ColumnInfo(name = "hourly_wind_speed") val hourlyWindSpeed: List<Double>
)


data class CurrentWeatherEntity(
    @ColumnInfo(name = "temperature") val temperature: Double,
    @ColumnInfo(name = "humidity") val humidity: Int,
    @ColumnInfo(name = "apparent_temperature") val apparentTemperature: Double,
    @ColumnInfo(name = "wind_speed") val windSpeed: Double,
    @ColumnInfo(name = "is_day") val isDay: Int,
    @ColumnInfo(name = "weathercode") val weathercode: Int,
    @ColumnInfo(name = "pressure_msl") val pressureMsl: Double
)



@Dao
interface WeatherDao {
    @Query("SELECT * FROM locations")
    suspend fun getAllLocations(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun getLocationById(id: Int): LocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity)

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Delete
    suspend fun deleteLocation(location: LocationEntity)

    @Query("DELETE FROM locations WHERE id = :id")
    suspend fun deleteLocationById(id: Int)
}

@Database(
    entities = [LocationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun weatherDao(): WeatherDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "weather_database"
                )
                    .addTypeConverter(Converters())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}