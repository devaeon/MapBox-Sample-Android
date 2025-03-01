package com.devaeon.mapbox

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.devaeon.mapbox.databinding.ActivityMainBinding
import com.devaeon.mapbox.utils.LocationPermissionHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class MainActivity : AppCompatActivity(), OnMapReadyCallback, MapboxMap.OnMapClickListener {

    private lateinit var binding: ActivityMainBinding
    private val locationPermissionHelper by lazy { LocationPermissionHelper(this@MainActivity) }

    private lateinit var mapboxMap: MapboxMap
    private lateinit var navigation: MapboxNavigation
    private var currentRoute: DirectionsRoute? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var userLocation: Point? = null
    private var destinationLocation: Point? = null

    private val locationEngineCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            Timber.i("onSuccess: locationEngine: ${result?.lastLocation}")
            result?.lastLocation?.let { location ->
                checkArrival(location)
                trimRoute(location)
            }
        }

        override fun onFailure(exception: Exception) {
            Timber.e("Location Engine Error: ${exception.message}")
        }
    }

    private fun checkArrival(currentLocation: Location) {
        if (destinationLocation != null) {
            val distance = calculateDistance(
                Point.fromLngLat(currentLocation.longitude, currentLocation.latitude),
                destinationLocation!!
            )

            Timber.i("Current Distance to Destination: $distance meters")

            if (distance <= 100) {
                showArrivalDialog()
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationPermissionHelper.checkPermission {
            binding.mapView.apply {
                onCreate(savedInstanceState)
                getMapAsync(this@MainActivity)
            }
        }

        navigation = MapboxNavigation(this, getString(R.string.mapbox_access_token)).apply {
            locationEngine.requestLocationUpdates(
                LocationEngineRequest.Builder(1000L)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .build(),
                locationEngineCallback,
                Looper.getMainLooper()
            )
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS) { style ->
            enableLocationComponent(style)
        }
        mapboxMap.addOnMapClickListener(this)
    }


    private fun trimRoute(currentLocation: Location) {
        val newOrigin = Point.fromLngLat(currentLocation.longitude, currentLocation.latitude)

        if (destinationLocation != null) {
            val distance = calculateDistance(newOrigin, destinationLocation!!)

            if (distance <= 100) {
                showArrivalDialog()
                return
            }

            NavigationRoute.builder(this)
                .accessToken(getString(R.string.mapbox_access_token))
                .origin(newOrigin)
                .destination(destinationLocation!!)
                .build()
                .getRoute(object : Callback<DirectionsResponse> {
                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            val routes = response.body()!!.routes()
                            if (routes.isNotEmpty()) {
                                currentRoute = routes[0]
                                updateRouteOnMap(currentRoute!!)
                            }
                        }
                    }

                    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                        t.printStackTrace()
                    }
                })
        }
    }

    private fun calculateDistance(origin: Point, destination: Point): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            origin.latitude(), origin.longitude(),
            destination.latitude(), destination.longitude(),
            results
        )
        return results[0].toDouble()
    }


    private fun updateRouteOnMap(updatedRoute: DirectionsRoute) {
        mapboxMap.getStyle { style ->
            val routeSource = style.getSourceAs<GeoJsonSource>("route-source")
            routeSource?.setGeoJson(LineString.fromPolyline(updatedRoute.geometry()!!, PRECISION_6))
        }
    }


    private fun enableLocationComponent(loadedMapStyle: Style) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationComponent = mapboxMap.locationComponent
            locationComponent.activateLocationComponent(
                LocationComponentActivationOptions.builder(this, loadedMapStyle).build()
            )
            locationComponent.isLocationComponentEnabled = true
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS

            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    userLocation = Point.fromLngLat(it.longitude, it.latitude)
                    Timber.i("User Location: $userLocation")

                    // Animate camera to user location
                    mapboxMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(it.latitude, it.longitude), 15.0
                        ), 2000
                    )
                }
            }
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        destinationLocation = Point.fromLngLat(point.longitude, point.latitude)
        Timber.i("Destination: $destinationLocation")

        if (userLocation != null && destinationLocation != null) {
            getRoute(userLocation!!, destinationLocation!!)
        }
        return true
    }

    private fun getRoute(origin: Point, destination: Point) {
        NavigationRoute.builder(this)
            .accessToken(getString(R.string.mapbox_access_token))
            .origin(origin)
            .destination(destination)
            .build()
            .getRoute(object : Callback<DirectionsResponse> {
                override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val routes = response.body()!!.routes()
                        Timber.i("onResponse: routes: ${routes.size}")
                        if (routes.isNotEmpty()) {
                            currentRoute = routes[0]
                            startNavigation()
                        }
                    }
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    t.printStackTrace()
                }
            })
    }

    private fun startNavigation() {

        clearRouteFromMap()

        val options = NavigationLauncherOptions.builder()
            .directionsRoute(currentRoute)
            .shouldSimulateRoute(true)
            .build()

        NavigationLauncher.startNavigation(this, options)

    }

    private fun clearRouteFromMap() {
        mapboxMap.getStyle { style ->
            if (style.getSource("route-source") is GeoJsonSource) {
                (style.getSourceAs<GeoJsonSource>("route-source"))?.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
            }
        }
    }


    private fun showArrivalDialog() {
        AlertDialog.Builder(this)
            .setTitle("Destination Reached")
            .setMessage("You have successfully arrived at your destination!")
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }



    override fun onStart() {
        super.onStart(); binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume(); binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause(); binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop(); binding.mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy(); binding.mapView.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onLowMemory() {
        super.onLowMemory(); binding.mapView.onLowMemory()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionResult(requestCode, permissions, grantResults)
    }
}