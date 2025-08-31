package com.meownit.nitweather


import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    // For List<Double>
    @TypeConverter
    fun fromDoubleList(list: List<Double>?): String? {
        return if (list == null) null else gson.toJson(list)
    }

    @TypeConverter
    fun toDoubleList(json: String?): List<Double> {
        return if (json == null) emptyList() else gson.fromJson(json, object : TypeToken<List<Double>>() {}.type)
    }

    // For List<Int>
    @TypeConverter
    fun fromIntList(list: List<Int>?): String? {
        return if (list == null) null else gson.toJson(list)
    }

    @TypeConverter
    fun toIntList(json: String?): List<Int> {
        return if (json == null) emptyList() else gson.fromJson(json, object : TypeToken<List<Int>>() {}.type)
    }

    // For List<String>
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return if (list == null) null else gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        return if (json == null) emptyList() else gson.fromJson(json, object : TypeToken<List<String>>() {}.type)
    }
}