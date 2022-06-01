package com.example.gpsservice

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationRequest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.gpsservice.databinding.ActivityMapsBinding
import com.example.gpsservice.db.LocationDatabase
import com.example.gpsservice.entity.Location
import com.example.gpsservice.service.GpsService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var binding: ActivityMapsBinding
    private val SOLICITA_GPS = 1

    companion object {
        lateinit var mMap: GoogleMap
        lateinit var locationClient: FusedLocationProviderClient
        lateinit var locationRequest: LocationRequest
        lateinit var locationCallback: LocationCallback
        //instancia de BD
        lateinit var locationDatabase: LocationDatabase
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Inicializacion del database
        locationDatabase = LocationDatabase.getInstance(this)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        this.validaPermisos()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        this.iniciaServicio()
        this.recuperarPuntos()
    }

    /**
     * Obtener los puntos de la ubicacion que estan en la BD y mostrarlos en el mapa
     */
    fun recuperarPuntos(){
        val locations = locationDatabase.locationDao.query()
        for(location in locations){
            val currentLocation = LatLng(location.latitude, location.longitude)
            mMap.addMarker(MarkerOptions().position(currentLocation).title("Marker"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))
        }
    }
    /**
     * Hace un filtro del broadcast GPS (com.example.gpsservice.GPS_EVENT)
     * e inicia el servicio (startService) GpsServise
     */
    fun iniciaServicio(){
        val filtro = IntentFilter()
        filtro.addAction(GpsService.gps)
        val recibidor = ProgressReceiver()
        registerReceiver(recibidor, filtro)
        startService(Intent(this, GpsService::class.java))
    }
    /**
     * Valida los permisos de ACCESS_FINE_LOCATION Y ACCESS_COARSE_LOCATION
     * si no tiene permisos solicita al usuario permisos (requestPermissions)
     */
    fun validaPermisos(){
        if(ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED)
        {
            //No tengo permisos
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                SOLICITA_GPS
            )
        }else {
            locationClient.requestLocationUpdates(
                locationRequest,
                locationCallback, null
            )
        }
    }

    /**
     * Validar que se le dieron permisos al app
     */
    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            SOLICITA_GPS -> {
                if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //El usuario dio permisos
                    locationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback, null
                    )

                }else {//El usuario no dio permiso
                    System.exit(1)
                }
            }
        }
    }
    /**
     * Es la clase para recibir los mensajes de broadcast de gps
     */
    class ProgressReceiver : BroadcastReceiver(){
        /**
         * Se obtiene el parametro enviado por el servicio (Location)
         *
         */
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action.equals(GpsService.gps)) {
                val mLocation: Location? =
                    intent!!.getSerializableExtra("gps") as Location?

                val punto = mLocation?.let { LatLng(it.latitude, it.longitude) }
                mMap.addMarker(MarkerOptions().position(punto).title("Marker"))
                mMap.moveCamera(CameraUpdateFactory.newLatLng(punto))
                if (mLocation != null) {
                    locationDatabase.locationDao.insert(mLocation)
                }
            }
        }
    }
}