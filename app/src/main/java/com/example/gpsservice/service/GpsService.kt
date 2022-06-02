package com.example.gpsservice.service

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Intent
import android.os.Looper
import com.example.gpsservice.MapsActivity
import com.example.gpsservice.db.LocationDatabase
import com.example.gpsservice.entity.Location
import com.google.android.gms.location.*

class GpsService : IntentService("GpsService") {

    companion object {
        val gps = "com.example.gpsservice.GPS_EVENT"
    }

    override fun onHandleIntent(intent: Intent?) {
        this.getLocation()
    }

    /**
     * Inicializa los atributos locationCallback y fusedLocationClient
     * coloca un intervalo de actualizacion de 10000 u una prioridad
     * de PRIORITY_HIGH_ACCURACY
     * recibe la ubicacion de GPS mediante un onLocationResult y envia un
     * broadcast con una instacia de Location y la accion GPS(com.example.gpsservice.GPS_EVENT)
     * ademas guarda la localizacion en la BD
     */
    @SuppressLint("MissingPermission")
    fun getLocation (){
        MapsActivity.locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                if(locationResult.equals(null)){
                    return
                }
                for (location in locationResult.locations) {
                    val nLocation = Location(null, location.latitude, location.longitude)
                    MapsActivity.locationDatabase.locationDao.insert(nLocation)
                    val bcIntent = Intent()
                    bcIntent.action = gps
                    bcIntent.putExtra("gps", nLocation)
                    sendBroadcast(bcIntent)
                }
            }
        }
        MapsActivity.locationClient = LocationServices.getFusedLocationProviderClient(this)

        MapsActivity.locationRequest = LocationRequest.create()
        MapsActivity.locationRequest.interval = 10000
        MapsActivity.locationRequest.fastestInterval = 5000
        MapsActivity.locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        LocationSettingsRequest.Builder().addLocationRequest(MapsActivity.locationRequest)
        MapsActivity.locationClient.requestLocationUpdates(
            MapsActivity.locationRequest,
            MapsActivity.locationCallback,
            null)
        Looper.loop()
    }
}