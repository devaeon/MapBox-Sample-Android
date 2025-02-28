package com.devaeon.mapbox.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.devaeon.mapbox.extensions.showDebugToast
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager

class LocationPermissionHelper(private val activity: Activity) {
    private lateinit var permissionsManager: PermissionsManager

    fun checkPermission(onMapReady: () -> Unit) {
        if (PermissionsManager.areLocationPermissionsGranted(activity)) {
            requestOptionalPermissions(onMapReady)
        } else {
            permissionsManager = PermissionsManager(object : PermissionsListener {
                override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
                    activity.showDebugToast("You need to accept location permission")
                }

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        requestOptionalPermissions(onMapReady)
                        
                    } else {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                            showPermissionDeniedDialog()
                        } else {
                            activity.showDebugToast("Permission denied. Some features may not work.")
                        }
                    }
                }
            })
        permissionsManager.requestLocationPermissions(activity)
        }
    }

    fun onRequestPermissionResult(
        requestCode: Int,
        permission: Array<out String>,
        grantResult: IntArray
    ) {

        permissionsManager.onRequestPermissionsResult(requestCode, permission, grantResult)
    }

    private fun requestOptionalPermissions(onMapReady: () -> Unit) {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissionsToRequest.isNotEmpty()) ActivityCompat.requestPermissions(
            activity,
            permissionsToRequest.toTypedArray(),
            10
        ) else onMapReady.invoke()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(activity)
            .setTitle("Permission Required")
            .setMessage("Location permission is required for this feature. Please enable it in settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("Cancel", ){_,_ ->
                activity.finishAndRemoveTask()
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }
}