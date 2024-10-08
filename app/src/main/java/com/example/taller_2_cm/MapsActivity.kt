package com.example.taller_2_cm

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller_2_cm.Datos.Data
import android.view.inputmethod.EditorInfo


import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.taller_2_cm.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.tasks.Task
import java.io.IOException
import kotlin.math.roundToInt

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var positionMarker: Marker? = null
    private var textMarker: Marker? = null
    private var touchMarker: Marker? = null
    private lateinit var binding: ActivityMapsBinding

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback

    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensor: Sensor
    private lateinit var lightSensorListener: SensorEventListener

    private var latitud = 0.0
    private var longitud = 0.0
    private val RADIUS_OF_EARTH_KM = 6371
    private var lastLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                Log.i("LOCATION", "Location update in the callback: $location")
                if (location != null) {
                    val newLatLng = LatLng(location.latitude, location.longitude)
                    // Si es la primera vez, simplemente inicializa el marcador
                    if (lastLocation == null) {
                        lastLocation = newLatLng
                        setLocation()
                    } else {
                        // Calcula la distancia entre la nueva ubicación y la última
                        val distance = distance(lastLocation!!.latitude, lastLocation!!.longitude, newLatLng.latitude, newLatLng.longitude)
                        if (distance >= 0.03) { // 0.03 km = 30 metros
                            lastLocation = newLatLng
                            setLocation()
                        }
                    }
                }
            }
        }

        pedirPermiso(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) , "se necesita de este permiso", Data.MY_PERMISSION_REQUEST_LOC)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        //Sensores
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)!!
        //listener
        luzSensor()

        val mGeocoder = Geocoder(baseContext)
        textPosition(mGeocoder)

    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
        sensorManager.registerListener(lightSensorListener, lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL)
    }
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        sensorManager.unregisterListener(lightSensorListener)
    }


    private fun luzSensor(){
        lightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (mMap != null) {
                    if (event.values[0] < 5000) {
                        Log.i("MAPS", "DARK MAP " + event.values[0])
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(baseContext, R.raw.dark_mode))
                    } else {
                        Log.i("MAPS", "LIGHT MAP " + event.values[0])
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(baseContext, R.raw.default_mode))
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
    }

    private fun textPosition(mGeocoder: Geocoder){
        binding.texto.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val addressString = binding.texto.text.toString()
                if (addressString.isNotEmpty()) {
                    try {
                        val addresses = mGeocoder.getFromLocationName(addressString, 2)
                        if (addresses != null && addresses.isNotEmpty()) {
                            val addressResult = addresses[0]
                            val position = LatLng(addressResult.latitude, addressResult.longitude)
                            // Verifica si el mapa ha sido inicializado antes de usarlo
                            if (::mMap.isInitialized) {

                                if (textMarker != null) {
                                    textMarker?.position = position
                                } else {
                                    // Si no existe, crea un nuevo marcador
                                    textMarker = mMap.addMarker(MarkerOptions().position(position).title("Estás aquí"))
                                }

                                // Mueve la cámara a la nueva posición
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
                            } else {
                                Toast.makeText(this, "El mapa no ha sido inicializado", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                        Toast.makeText(this, "Error al buscar la dirección", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "La dirección está vacía", Toast.LENGTH_SHORT).show()
                }
                true  // Indica que manejaste el evento
            } else {
                false  // No manejaste la acción
            }
        }
    }


    private fun stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        mMap.setOnMapLongClickListener { latLng ->
            if (touchMarker != null) {
                touchMarker?.position = latLng
                touchMarker?.title = getPlaceName(latLng)
            } else {
                // Si no existe, crea un nuevo marcador
                touchMarker = mMap.addMarker(MarkerOptions().position(latLng).title(getPlaceName(latLng)))
            }

            var distancia = distance(latLng.latitude, latLng.longitude, latitud, longitud)
            Toast.makeText(this, "La distancia es ${distancia}km", Toast.LENGTH_SHORT).show()
            // Mueve la cámara a la nueva posición
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            mMap.addMarker(MarkerOptions().position(latLng))
        }

    }

    private fun getPlaceName(latLng: LatLng): String? {
        val geocoder = Geocoder(baseContext)
        try {
            // Obtén la lista de direcciones a partir de las coordenadas
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val address = addresses?.get(0)
                    val placeName = address?.getAddressLine(0)
                    return placeName// O puedes usar address.locality, address.countryName, etc.

                } else {
                    Log.i("PLACE_NAME", "No se encontraron direcciones para esta ubicación")
                }
            }
        } catch (e: Exception) {
            Log.e("GeocoderError", "Error al obtener el nombre del lugar: ${e.message}")
        }
        return "Not found"
    }

    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = (Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2))
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val result = RADIUS_OF_EARTH_KM * c
        return (result * 100.0).roundToInt() / 100.0
    }

    private fun createLocationRequest(): LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,10000).apply{
        setMinUpdateIntervalMillis(5000)
    }.build()

    fun setLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mFusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            Log.i("LOCATION", "onSuccess location")
            if (location != null) {
                Log.i("LOCATION", "Longitud: " + location.longitude)
                Log.i("LOCATION", "Latitud: " + location.latitude)

                // Actualiza las coordenadas
                longitud = location.longitude
                latitud = location.latitude

                // Verifica si el mapa está listo para usar
                if (::mMap.isInitialized) {
                    // Crea una nueva posición
                    val nuevaPosicion = LatLng(latitud, longitud)

                    if (positionMarker != null) {
                        positionMarker?.position = nuevaPosicion
                    } else {
                        // Si no existe, crea un nuevo marcador
                        positionMarker = mMap.addMarker(MarkerOptions().position(nuevaPosicion).title("Estás aquí"))
                    }

                    // Mueve la cámara a la nueva posición
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(nuevaPosicion, 15f))
                }
            }
        }

    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        }
    }


    private fun pedirPermiso(context: Activity, permisos: Array<String>, justificacion: String, idCode: Int) {
        val permisosNoConcedidos = permisos.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permisosNoConcedidos.isNotEmpty()) {
            if (permisosNoConcedidos.any { ActivityCompat.shouldShowRequestPermissionRationale(context, it) }) {
                // Mostrar justificación adicional si es necesario.
            }
            ActivityCompat.requestPermissions(context, permisosNoConcedidos.toTypedArray(), idCode)
        } else {
            setLocation()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)


        when (requestCode) {
            Data.MY_PERMISSION_REQUEST_LOC -> {
                var permisosConcedidos = true
                for (i in grantResults.indices) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        permisosConcedidos = false
                        break
                    }
                }

                if (permisosConcedidos) {
                    Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
                    setLocation()
                } else {
                    Toast.makeText(this, "Permisos denegados", Toast.LENGTH_SHORT).show()

                }
            }
            else -> {
                // Manejar otras solicitudes de permisos si es necesario.
            }
        }
    }



}