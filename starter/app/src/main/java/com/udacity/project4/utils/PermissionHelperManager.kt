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

class PermissionHelperManager(val activity: Context, private val mPermissionHelper: PermissionHelper) {

    companion object {
        const val LOCATION_PERMISSIONS_REQUEST_CODE: Int = 123
        val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    var builder: AlertDialog.Builder? = null


    fun areAllPermissionsGranted(): Boolean {
        return hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                && hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                && hasPermission(Manifest.permission.CAMERA)
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }


    fun requestLocationPermission() {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)  && !hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            if (shouldShowRequestRationale(Manifest.permission.ACCESS_FINE_LOCATION) && shouldShowRequestRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                buildAlertMessageRequestGps()
                return
            } else {
                permissionsRequest(PERMISSIONS, LOCATION_PERMISSIONS_REQUEST_CODE)
            }
        } else {
            mPermissionHelper.locationPermissionAccepted()
        }
    }

    private fun buildAlertMessageRequestGps() {
        builder = AlertDialog.Builder(activity)
            .setMessage(activity.getString(R.string.location_required_error))
            .setCancelable(false)
            .setPositiveButton(
                activity.getString(R.string.yes)
            ) { p0, p1 ->
                p0?.cancel()
                permissionsRequest(PERMISSIONS, LOCATION_PERMISSIONS_REQUEST_CODE)
            }
        val alertDialog = builder?.create()
        alertDialog?.show()
    }

    private fun permissionsRequest(permissions:Array<String>, requestCode: Int) {
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

}