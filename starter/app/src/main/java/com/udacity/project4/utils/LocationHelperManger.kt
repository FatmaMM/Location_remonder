package com.udacity.project4.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.IntentSender
import android.location.Location
import android.location.LocationManager
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener


class LocationHelperManger(private val activity: Context, val locationManager: MLocationHelper) {
    companion object {
        private val INTERVAL = 20000L
        private val FAST_INTERVAL = 2000L

        private var fusedLocationClient: FusedLocationProviderClient? = null
        private var locationCallback: LocationCallback? = null
        private var locationRequest: LocationRequest? = null
    }

    init {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        }
        createLocationRequest()
        createLocationCallBack()
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(FAST_INTERVAL)
            .setMaxUpdateDelayMillis(INTERVAL)
            .build()
    }

    private fun enableGPS() {
        val settingsBuilder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest!!)
        settingsBuilder.setAlwaysShow(true)
        val result =
            LocationServices.getSettingsClient(activity)
                .checkLocationSettings(settingsBuilder.build())
        result.addOnCompleteListener { task ->
            try {
                task.getResult(ApiException::class.java)
            } catch (ex: ApiException) {

                when (ex.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {

                        Toast.makeText(activity, "GPS IS OFF", Toast.LENGTH_SHORT).show()

                        // Show the dialog by calling startResolutionForResult(), and check the result
                        // in onActivityResult().
                        val resolvableApiException = ex as ResolvableApiException
                        resolvableApiException.startResolutionForResult(
                            activity as Activity,
                            PermissionHelperManager.LOCATION_PERMISSIONS_REQUEST_CODE
                        )
                    } catch (e: IntentSender.SendIntentException) {
                        Toast.makeText(
                            activity,
                            "PendingIntent unable to execute request.",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        Toast.makeText(
                            activity,
                            "Something is wrong in your GPS",
                            Toast.LENGTH_SHORT
                        ).show()

                    }
                }
            }
        }
    }

    private fun createLocationCallBack() {
        if (locationCallback == null) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    locationManager.onLocationChanged(locationResult.lastLocation)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        enableGPS()
        fusedLocationClient!!.requestLocationUpdates(
            locationRequest!!,
            locationCallback!!,
            null
        )
        getLastKnownLocation()
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation() {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.activity)

        }
        fusedLocationClient!!.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                locationManager.getLastKnownLocation(location)
            }
        }
    }


    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient!!.removeLocationUpdates(it) }
    }
}