package com.example.flatload

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private var currentRoute: DirectionsRoute? = null
    private var client: MapboxDirections? = null
    //var destinationX=0.0  // longitude
    //var destinationY=0.0 // latitude

    // 내 gps 위치 (임의지정)
    val origin: Point = Point.fromLngLat(126.831478, 37.3200456)
    // 도착지 gps 위치
    //val destination: Point = Point.fromLngLat(destinationX, destinationY)
    val destination: Point = Point.fromLngLat(126.848678, 37.3167894)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // 맵박스 사용하기 위한 접근 토큰 지정
        Mapbox.getInstance(this, getString(R.string.access_token))
        // 아래 함수로 통해 목적지 주소값을 위도 경도 값으로 변경
        // getPointFromGeoCoder("서울특별시 송파구 방이동 112-1")

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        this.map = mapboxMap
        // 카메라 위치 고정(내 gps 위치로 임의지정)
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom( // 카메라는 반대의 값으로 적어줄 것
                // 뒤에 숫자 15은 카메라 확대 배수이다( 15가 적당 )
                LatLng(37.3200456, 126.831478), 15.0
            )
        )
        // Add origin and destination to the map
        mapboxMap?.addMarker(
            MarkerOptions()
                .position(
                    LatLng(
                        origin.latitude(),
                        origin.longitude()
                    )
                ) // 타이틀은 상호명 건물명, snippet은 설명 그에 대한 설명이다
                // 출발지
                .title("출발지")
                .snippet("start")
        )
        mapboxMap?.addMarker(
            MarkerOptions() // 목적지
                .position(
                    LatLng(
                        destination.latitude(),
                        destination.longitude()
                    )
                )
                .title("도착지")
                .snippet("end")
        )
        // Get route from API
        getRoute(origin, destination)
    }
    private fun getRoute(
        origin: Point,
        destination: Point
    ) {
        client = MapboxDirections.builder()
            .origin(origin)
            .destination(destination)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_WALKING)
            .accessToken(getString(R.string.access_token))
            .build()

        client?.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                //System.out.println(call.request().url().toString())
                // You can get the generic HTTP info about the response
                //Log.i("Map", "Response code: " + response.code())
                if (response.body() == null) {
                    Log.i("response_null","No routes found, make sure you set the right user and access token.")
                    return
                } else if (response.body()!!.routes().size < 1) {
                    Log.i("response_small", "No routes found")
                    return
                }
                // Print some info about the route
                currentRoute = response.body()!!.routes().get(0)
                Log.i("complete", "Distance: " + currentRoute?.distance())
                // Draw the route on the map
                drawRoute(currentRoute!!)
            }

            override fun onFailure(
                call: Call<DirectionsResponse?>?,
                throwable: Throwable
            ) {
                Log.e("fail", "Error: " + throwable.message)
                Toast.makeText(this@MapActivity, "Error: " + throwable.message, Toast.LENGTH_SHORT).show()
            }
        })
    }
    private fun drawRoute(route: DirectionsRoute) {
        val lineString = LineString.fromPolyline(route.geometry()!!, Constants.PRECISION_6)
        val coordinates = lineString.coordinates()
        val points = ArrayList<LatLng>(coordinates.size)
        //val points : MutableList<LatLng> = mutableListOf(LatLng(0.0,0.0))
        for (i in 0 until coordinates.size) {
            points.add(LatLng(
                coordinates[i].latitude(),
                coordinates[i].longitude()
            ))
        }
        // Draw Points on MapView
        map!!.addPolyline(
            PolylineOptions()
                .addAll(points)
                .color(Color.parseColor("#009688"))
                .width(5f)
        )
    }
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


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView!!.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel the directions API request
        client?.cancelCall()
        mapView!!.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView!!.onLowMemory()
    }


    // 목적지 주소값을 통해 목적지 위도 경도를 얻어오는 구문
    /*
    fun getPointFromGeoCoder(addr: String) {
        val geocoder = Geocoder(this)
        var listAddress: List<Address>? = null
        try {
            listAddress = geocoder.getFromLocationName(addr, 1)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        destinationX = listAddress!![0].getLongitude()
        destinationY = listAddress!![0].getLatitude()
        //println("$addr's Destination x, y = $destinationX, $destinationY")
        Log.i("stringloc",listAddress.toString())
    }*/
}