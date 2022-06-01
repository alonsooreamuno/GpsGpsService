package com.example.gpsservice.entity
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "locations_table")
data class Location (
    @PrimaryKey(autoGenerate = true)
    val locationId: Long?,
    val latitude: Double,
    val longitude:Double) : Serializable