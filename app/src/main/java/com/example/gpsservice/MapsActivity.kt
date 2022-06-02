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
import android.widget.Toast
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
import com.google.maps.android.PolyUtil
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonPolygon
import org.json.JSONObject


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
        private lateinit var layer : GeoJsonLayer
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
        this.definePoligono(mMap)
        this.recuperarPuntos()
    }

    fun definePoligono(
        googleMap: GoogleMap){ val geoJsonData= JSONObject("{\n" +
            "  \"type\": \"FeatureCollection\",\n" +
            "  \"features\": [\n" +
            "    {\n" +
            "      \"type\": \"Feature\",\n" +
            "      \"properties\": {},\n" +
            "      \"geometry\": {\n" +
            "        \"type\": \"Polygon\",\n" +
            "        \"coordinates\": [\n" +
            "          [\n" +
            "            [\n" +
            "              -85.869140625,\n" +
            "              10.90883015572212\n" +
            "            ],\n" +
            "            [\n" +
            "              -85.62744140625,\n" +
            "              10.639013775260537\n" +
            "            ],\n" +
            "            [\n" +
            "              -85.8251953125,\n" +
            "              10.13111684154069\n" +
            "            ],\n" +
            "            [\n" +
            "              -82.90283203125,\n" +
            "              8.135367205502842\n" +
            "            ],\n" +
            "            [\n" +
            "              -82.90283203125,\n" +
            "              8.809082353052137\n" +
            "            ],\n" +
            "            [\n" +
            "              -82.7490234375,\n" +
            "              8.950192825865791\n" +
            "            ],\n" +
            "            [\n" +
            "              -82.99072265625,\n" +
            "              9.080400104155315\n" +
            "            ],\n" +
            "            [\n" +
            "              -82.90283203125,\n" +
            "              9.644076964907923\n" +
            "            ],\n" +
            "            [\n" +
            "              -82.562255859375,\n" +
            "              9.524914302345891\n" +
            "            ],\n" +
            "            [\n" +
            "              -83.6279296875,\n" +
            "              10.930404972955545\n" +
            "            ],\n" +
            "            [\n" +
            "              -83.86962890625,\n" +
            "              10.703791711680736\n" +
            "            ],\n" +
            "            [\n" +
            "              -84.6826171875,\n" +
            "              11.070602913977819\n" +
            "            ],\n" +
            "            [\n" +
            "              -84.88037109375,\n" +
            "              10.90883015572212\n" +
            "            ],\n" +
            "            [\n" +
            "              -85.572509765625,\n" +
            "              11.243062041947772\n" +
            "            ],\n" +
            "            [\n" +
            "              -85.869140625,\n" +
            "              10.90883015572212\n" +
            "            ]\n" +
            "          ]\n" +
            "        ]\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}")
        layer = GeoJsonLayer(googleMap, geoJsonData)
        layer.addLayerToMap()
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

        fun getPolygon(
            layer: GeoJsonLayer): GeoJsonPolygon? {
            for (feature in layer.features) {
                return feature.geometry as GeoJsonPolygon
            }
            return null
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action.equals(GpsService.gps)) {
                val mLocation: Location? =
                    intent!!.getSerializableExtra("gps") as Location?

                val coordenadas = mLocation?.let { LatLng(it.latitude, it.longitude) }
                mMap.addMarker(MarkerOptions().position(coordenadas).title("Marker"))
                mMap.moveCamera(CameraUpdateFactory.newLatLng(coordenadas))
                if (mLocation != null) {
                    locationDatabase.locationDao.insert(mLocation)

                    if(PolyUtil.containsLocation(mLocation.latitude, mLocation.longitude, getPolygon(layer)!!.outerBoundaryCoordinates, false)){
                        Toast.makeText(context,"Esta en el punto.", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(context,"NO esta en el punto.", Toast.LENGTH_SHORT).show()
                    }
                }



            }
        }
    }
}