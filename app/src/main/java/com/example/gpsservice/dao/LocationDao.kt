package com.example.gpsservice.dao

import androidx.room.*
import com.example.gpsservice.entity.Location

@Dao
interface LocationDao {
    @Insert
    fun insert(location: Location)

    @Query("select * from locations_table")
    fun query(): List<Location>
}