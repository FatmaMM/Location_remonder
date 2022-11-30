package com.udacity.project4.utils

import android.location.Location

interface MLocationHelper {
    fun getLastKnownLocation(location: Location?)
    fun showSnackBar()
    fun startLocationRequest()
}