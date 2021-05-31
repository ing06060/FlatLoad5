package com.example.flatload

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolClickListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin
import com.mapbox.mapboxsdk.plugins.localization.MapLocale
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils

class MapActivity : AppCompatActivity(),PermissionsListener,OnMapReadyCallback {

    private var mapView: MapView? = null
    private lateinit var routeCoordinates: ArrayList<Point>
    private var permissionsManager: PermissionsManager? = null
    private var mapboxMap: MapboxMap? = null
    private lateinit var LocList: PairList

    private val SOURCE_ID = "SOURCE_ID"
    private val ICON_ID = "ICON_ID"
    private val LAYER_ID = "LAYER_ID"

    private val MAKI_ICON_CAFE = "cafe-15"
    private val MAKI_ICON_HARBOR = "harbor-15"
    private val MAKI_ICON_AIRPORT = "airport-15"
    private val MAKI_ICON_BARRIER = "barrier-15"
    private var symbolManager: SymbolManager? = null
    private var symbol: Symbol? = null

    private var localizationPlugin: LocalizationPlugin? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token))

        setContentView(R.layout.activity_map)
        val i = intent
        LocList = i.getSerializableExtra("pairList") as PairList

        Log.i("pairList", LocList.toString())
        initRouteCoordinates(LocList)
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    private fun initRouteCoordinates(locList: PairList) {
        //routeCoordinates: ArrayList<Point>? = null
        Log.i("확인", locList.toString())
        Log.i("확인", locList.pairList.size.toString())
        Log.i("확인", locList.pairList[0].first.toString())
        Log.i("확인", locList.pairList[0].second.toString())

        routeCoordinates = ArrayList<Point>()
        for (i in 0..locList.pairList.size-1){
            routeCoordinates.add(Point.fromLngLat(locList.pairList[i].first,locList.pairList[i].second))
            Log.i("LocList 확인:", routeCoordinates.get(i).toString())
        }

        Log.i("LocList 확인:",routeCoordinates.toString())
//        // Create a list to store our line coordinates.
//        routeCoordinates = ArrayList<Point>()
//        routeCoordinates?.add(Point.fromLngLat(127.074475, 37.547962))
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap

        mapboxMap?.setStyle(Style.MAPBOX_STREETS, object: Style.OnStyleLoaded {
            override fun onStyleLoaded(style: Style) {
                // 언어 설정
                localizationPlugin = LocalizationPlugin(mapView!!, mapboxMap, style)
                localizationPlugin!!.setMapLanguage(MapLocale.KOREA)

                // 마커 추가
                symbolManager = SymbolManager(mapView!!, mapboxMap, style)

                symbolManager!!.iconAllowOverlap = true
                symbolManager!!.textAllowOverlap = true
                // Add symbol at specified lat/lon

                symbol = symbolManager!!.create(
                    SymbolOptions()
                        .withLatLng(LatLng(37.547147, 127.074148)) //위험 요소 마커 추가
                        .withIconImage(MAKI_ICON_HARBOR)
                        .withIconSize(2.0f)
                        .withDraggable(true)
                )
                symbolManager!!.addClickListener(object : OnSymbolClickListener {
                    override fun onAnnotationClick(symbol: Symbol): Boolean {
                        Toast.makeText(
                            this@MapActivity,
                            "click marker symbol", Toast.LENGTH_SHORT
                        ).show()
                        //symbol.iconImage = MAKI_ICON_CAFE
                        //symbolManager!!.update(symbol)
//                        val i = Intent(this,MapActivity::class.java)
//                        startActivity(i)
                        changeActivity(symbol.latLng)
                        return true
                    }
                })
                enableLocationComponent(style)
                // Create the LineString from the list of coordinates and then make a GeoJSON
                // FeatureCollection so we can add the line to our map as a layer.
                style.addSource(
                    GeoJsonSource("line-source",
                        FeatureCollection.fromFeatures(arrayOf(
                            Feature.fromGeometry(
                                LineString.fromLngLats(routeCoordinates!!)
                            ))))
                )
                // The layer properties for our line. This is where we make the line dotted, set the
                // color, etc.
                style.addLayer(
                    LineLayer("linelayer", "line-source").withProperties(
                        PropertyFactory.lineDasharray(arrayOf(0.01f, 2f)),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                        PropertyFactory.lineWidth(5f),
                        PropertyFactory.lineColor(Color.parseColor("#e55e5e"))
                    ))
            }
        })
    }

    private fun changeActivity(location: LatLng) {
        val i = Intent(this,MarkerResultActivity::class.java)
        i.putExtra("markerLocation", location.toString())
        startActivity(i)
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        Toast.makeText(this, "user_location_permission_explanation", Toast.LENGTH_LONG).show();
    }

    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            mapboxMap!!.getStyle { style -> enableLocationComponent(style) }
        } else {
            Toast.makeText(this, "user_location_permission_not_granted", Toast.LENGTH_LONG)
                .show()
            finish()
        }
    }

    @SuppressWarnings( "MissingPermission")
    private fun enableLocationComponent(loadedMapStyle: Style) {
// Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

// Enable the most basic pulsing styling by ONLY using
// the `.pulseEnabled()` method
            val customLocationComponentOptions =
                LocationComponentOptions.builder(this)
                    .pulseEnabled(true).bearingTintColor(Color.BLUE).backgroundTintColor(Color.BLUE)
                    .build()

// Get an instance of the component
            val locationComponent = mapboxMap!!.locationComponent

// Activate with options
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, loadedMapStyle)
                    .locationComponentOptions(customLocationComponentOptions)
                    .build()
            )

// Enable to make component visible
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            locationComponent.isLocationComponentEnabled = true

// Set the component's camera mode
            locationComponent.cameraMode = CameraMode.TRACKING

// Set the component's render mode
            locationComponent.renderMode = RenderMode.COMPASS
            //initLocationEngine()
        } else {
            permissionsManager = PermissionsManager(this)
            permissionsManager!!.requestLocationPermissions(this)
        }
    }

//    @SuppressLint("MissingPermission")
//    private fun initLocationEngine() {
//        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
//
//        val request: LocationEngineRequest = Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
//            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
//            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()
//
//        locationEngine.requestLocationUpdates(request, callback, mainLooper)
//        locationEngine.getLastLocation(callback)
//    }


    override fun onResume() {
        super.onResume()
        mapView!!.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView!!.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView!!.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView!!.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView!!.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState!!)
        mapView!!.onSaveInstanceState(outState)
    }

}