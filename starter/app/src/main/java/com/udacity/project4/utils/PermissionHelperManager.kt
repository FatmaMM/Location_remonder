package com.udacity.project4.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import com.udacity.project4.R

class PermissionHelperManager(
    val activity: Context,
    private val mPermissionHelper: PermissionHelper
) {

    companion object {
        const val LOCATION_PERMISSIONS_REQUEST_CODE: Int = 123
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }


    fun areAllPermissionsGranted(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                && hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun requestLocationPermission() {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (!shouldShowRequestRationale(Manifest.permission.ACCESS_FINE_LOCATION) && !shouldShowRequestRationale(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                permissionsRequest(PERMISSIONS, LOCATION_PERMISSIONS_REQUEST_CODE)
            }
        } else {
            mPermissionHelper.startLocationRequest()
        }
    }

    private fun permissionsRequest(permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity as Activity,
            permissions,
            requestCode
        )
    }


    private fun shouldShowRequestRationale(permission: String): Boolean {
        return shouldShowRequestPermissionRationale(
            activity as Activity,
            permission
        )
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSIONS_REQUEST_CODE) {
            mPermissionHelper.startLocationRequest()
        }
    }
}