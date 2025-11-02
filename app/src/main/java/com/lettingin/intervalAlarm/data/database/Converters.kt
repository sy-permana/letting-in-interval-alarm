package com.lettingin.intervalAlarm.data.database

import androidx.room.TypeConverter
import com.lettingin.intervalAlarm.data.model.NotificationType
import com.lettingin.intervalAlarm.data.model.ThemeMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    
    @TypeConverter
    fun fromLocalTime(value: LocalTime?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it) }
    }
    
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }
    
    @TypeConverter
    fun fromDayOfWeekSet(value: Set<DayOfWeek>?): String? {
        return value?.joinToString(",") { it.value.toString() }
    }
    
    @TypeConverter
    fun toDayOfWeekSet(value: String?): Set<DayOfWeek>? {
        return value?.split(",")
            ?.filter { it.isNotEmpty() }
            ?.map { DayOfWeek.of(it.toInt()) }
            ?.toSet()
    }
    
    @TypeConverter
    fun fromNotificationType(value: NotificationType?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toNotificationType(value: String?): NotificationType? {
        return value?.let { NotificationType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromThemeMode(value: ThemeMode?): String? {
        return value?.name
    }
    
    @TypeConverter
    fun toThemeMode(value: String?): ThemeMode? {
        return value?.let { ThemeMode.valueOf(it) }
    }
}
