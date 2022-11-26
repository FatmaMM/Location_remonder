package com.udacity.project4.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
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
        var mLocationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        locationManager.hasGPSSignal(isGPSEnabled)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(FAST_INTERVAL)
            .setMaxUpdateDelayMillis(INTERVAL)
            .build()
    }


    private fun createLocationCallBack() {
        if (locationCallback == null) {
            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    for (location in locationResult.locations) {
                        if (location != null) {
                            locationManager.onLocationChanged(location)
                            locationManager.hasGPSSignal(true)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedLocationClient!!.requestLocationUpdates(locationRequest!!, locationCallback!!, null)
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
                locationManager.hasGPSSignal(true)
            }
        }
    }


    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient!!.removeLocationUpdates(it) }
    }
}