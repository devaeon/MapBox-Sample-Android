package com.devaeon.mapbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.devaeon.mapbox.databinding.ActivityMainBinding
import com.devaeon.mapbox.utils.LocationPermissionHelper
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val locationPermissionHelper by lazy { LocationPermissionHelper(this@MainActivity) }

    private val mapView by lazy { binding.mapView }
    private lateinit var mapboxMap: MapboxMap
    private lateinit var navigation: MapboxNavigation
    private var currentRoute: DirectionsRoute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync {
            this.mapboxMap = it
            mapboxMap.setStyle(Style.MAPBOX_STREETS) {
                getRoute()
            }
        }
        locationPermissionHelper.checkPermission {}

        // Initialize navigation
        navigation = MapboxNavigation(this, getString(R.string.mapbox_access_token))

    }

    private fun getRoute() {
        val origin = com.mapbox.geojson.Point.fromLngLat(-122.4194, 37.7749) // Example: San Francisco
        val destination = com.mapbox.geojson.Point.fromLngLat(-122.4081, 37.7835) // Example: Another point

        NavigationRoute.builder(this)
            .accessToken(getString(R.string.mapbox_access_token))
            .origin(origin)
            .destination(destination)
            .build()
            .getRoute(object : Callback<DirectionsResponse> { // Change to DirectionsResponse
                override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                    if (response.body() != null && response.body()!!.routes().isNotEmpty()) {
                        currentRoute = response.body()!!.routes()[0] // Get the first route
                        startNavigation()
                    }
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }


    private fun startNavigation() {
        val options = NavigationLauncherOptions.builder()
            .directionsRoute(currentRoute)
            .shouldSimulateRoute(true)
            .build()

        NavigationLauncher.startNavigation(this, options)
    }

    override fun onStart() {
        super.onStart(); mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionResult(requestCode, permissions, grantResults)
    }
}