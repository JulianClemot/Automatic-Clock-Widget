package com.julian.automaticclockwidget.airports.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AirportEntity::class], version = 1, exportSchema = false)
abstract class AirportDatabase : RoomDatabase() {
    abstract fun airportDao(): AirportDao
}
