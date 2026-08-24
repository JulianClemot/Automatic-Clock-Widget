package com.julian.automaticclockwidget.airports.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.julian.automaticclockwidget.airports.Airport
import kotlinx.datetime.TimeZone

@Entity(tableName = "airports")
data class AirportEntity(
    @PrimaryKey val iataCode: String,
    val name: String,
    val city: String,
    val country: String,
    val timezone: String,
)

fun AirportEntity.toAirport() = Airport(
    iataCode = iataCode,
    name = name,
    city = city,
    country = country,
    timezone = TimeZone.of(timezone),
)
