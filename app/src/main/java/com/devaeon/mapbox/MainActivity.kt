package com.devaeon.mapbox

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.devaeon.mapbox.databinding.ActivityMainBinding
import com.devaeon.mapbox.utils.LocationPermissionHelper
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.style
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val locationPermissionHelper by lazy { LocationPermissionHelper(this@MainActivity) }
    private lateinit var map: MapboxMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        locationPermissionHelper.checkPermission {
           // onMapReady(binding.mapView.getMapboxMap())
        }
    }

    private fun onMapReady(mapboxMap: MapboxMap) {
        Log.d("TAG", "onMapReady: ready")
        map = mapboxMap
        map.setCamera(CameraOptions.Builder().center(Point.fromLngLat(LONGITUDE, LATITUDE)).zoom(ZOOM).build())
        map.loadStyle((style(styleUri = Style.MAPBOX_STREETS) {}))
    }

    private fun initNavigation() {
        MapboxNavigationApp.setup(
            NavigationOptions.Builder(this).accessToken(getString(R.string.mapbox_access_token))
                .build()
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionHelper.onRequestPermissionResult(requestCode,permissions,grantResults)
    }

    companion object {
        private const val LATITUDE = 41.55758213979963
        private const val LONGITUDE = 60.588573882438624
        private const val ZOOM = 14.0
    }
}