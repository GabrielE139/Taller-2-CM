package com.example.taller_2_cm.Logica

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import android.preference.PreferenceManager
import com.example.taller_2_cm.R
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.views.overlay.Polyline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.BoundingBox

class MapsOSM : AppCompatActivity() {

    private lateinit var map: MapView
    private var startPoint: GeoPoint? = null
    private var endPoint: GeoPoint? = null
    private var roadOverlay: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_osm_maps)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setBuiltInZoomControls(true)
        map.setMultiTouchControls(true)

        val startLat = intent.getDoubleExtra("start_lat", 0.0)
        val startLng = intent.getDoubleExtra("start_lng", 0.0)
        val endLat = intent.getDoubleExtra("end_lat", 0.0)
        val endLng = intent.getDoubleExtra("end_lng", 0.0)

        startPoint = GeoPoint(startLat, startLng)
        endPoint = GeoPoint(endLat, endLng)

        val startMarker = Marker(map)
        startMarker.position = startPoint
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(startMarker)

        val endMarker = Marker(map)
        endMarker.position = endPoint
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(endMarker)

        map.controller.setZoom(15.0)
        map.controller.setCenter(startPoint)

        CoroutineScope(Dispatchers.Main).launch {
            drawRoute(startPoint!!, endPoint!!)

            val midLat = (startPoint!!.latitude + endPoint!!.latitude) / 2
            val midLng = (startPoint!!.longitude + endPoint!!.longitude) / 2
            val midPoint = GeoPoint(midLat, midLng)


            var boundingBox = BoundingBox.fromGeoPoints(listOf(startPoint, endPoint))

            val margin = 0.01
            boundingBox = BoundingBox(
                boundingBox.latNorth + margin,
                boundingBox.lonEast + margin,
                boundingBox.latSouth - margin,
                boundingBox.lonWest - margin
            )

            map.controller.setCenter(midPoint)
            map.zoomToBoundingBox(boundingBox, true)
        }
    }

    private suspend fun drawRoute(start: GeoPoint, finish: GeoPoint) {
        val road = withContext(Dispatchers.IO) {
            val roadManager = OSRMRoadManager(this@MapsOSM, "YOUR_USER_AGENT")
            val waypoints = ArrayList<GeoPoint>()
            waypoints.add(start)
            waypoints.add(finish)
            roadManager.getRoad(waypoints)
        }

        if (road.mStatus != Road.STATUS_OK) {
            // Handle the error
            return
        }

        if (::map.isInitialized) {
            roadOverlay?.let { map.overlays.remove(it) }
            roadOverlay = RoadManager.buildRoadOverlay(road).apply {
                outlinePaint.color = resources.getColor(R.color.blue, theme)
                outlinePaint.strokeWidth = 10f
            }
            map.overlays.add(roadOverlay)
            map.invalidate()
        }
    }


}