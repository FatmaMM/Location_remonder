package com.udacity.project4.utils

import android.location.Location

interface MLocationHelper {
    fun onLocationChanged(location: Location?)

    fun getLastKnownLocation(location: Location?)

}