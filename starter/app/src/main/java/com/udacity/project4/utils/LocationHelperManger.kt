package com.udacity.project4.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_LOW_POWER, INTERVAL)
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
                    locationManager.getLastKnownLocation(locationResult.lastLocation)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedLocationClient!!.requestLocationUpdates(
            locationRequest!!,
            locationCallback!!,
            null
        )
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.activity)
        }
        fusedLocationClient!!.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                locationManager.getLastKnownLocation(location)
            } else {
                getCurrentLocation()
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun getCurrentLocation() {
        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.activity)
        }
        fusedLocationClient!!.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            object : CancellationToken() {
                override fun onCanceledRequested(p0: OnTokenCanceledListener) =
                    CancellationTokenSource().token

                override fun isCancellationRequested() = false
            })
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    locationManager.getLastKnownLocation(location)
                } else {
                    locationManager.startLocationRequest()
                }
            }
    }

    fun isLocationEnabled(): Boolean {
        val locationManager =
            activity.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient!!.removeLocationUpdates(it) }
    }

    fun checkDeviceLocationSettingsAndStartGeofence(v: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(activity)
        builder.setAlwaysShow(true)
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { ex ->
            if (ex is ResolvableApiException && v) {
                try {
                    ex.startResolutionForResult(
                        activity as Activity,
                        123
                    )
                } catch (ex: ApiException) {
                    Toast.makeText(
                        activity,
                        "PendingIntent unable to execute request.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                locationManager.showSnackBar()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            locationManager.startLocationRequest()
        }
    }

    fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
        val backgroundPermissionApproved =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }
}