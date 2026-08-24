package com.julian.automaticclockwidget.airports.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AirportDao {

    @Query("SELECT * FROM airports WHERE iataCode = :iataCode")
    suspend fun findByIata(iataCode: String): AirportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(airports: List<AirportEntity>)

    @Query("DELETE FROM airports")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(airports: List<AirportEntity>) {
        deleteAll()
        insertAll(airports)
    }
}
