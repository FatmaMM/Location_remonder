package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver.Companion.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback,
    GoogleMap.OnMyLocationClickListener,
    GoogleMap.OnMyLocationButtonClickListener, MLocationHelper {

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var googleMap: GoogleMap
    private var marker: Marker? = null
    private val locationHelper: LocationHelperManger by inject {
        parametersOf(
            this@SelectLocationFragment.requireActivity(),
            this
        )
    }
    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    @SuppressLint("MissingPermission")
    private var activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            var allAreGranted = true
            for (b in result.values) {
                allAreGranted = allAreGranted && b
            }
            if (allAreGranted) {
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings.isMyLocationButtonEnabled = true
                googleMap.uiSettings.isZoomControlsEnabled = true
                googleMap.setOnMyLocationButtonClickListener(this)
                googleMap.setOnMyLocationClickListener(this)
                locationHelper.checkDeviceLocationSettingsAndStartGeofence()
            } else {
                showSnackBar()
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.google_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.saveBtn.setOnClickListener {
            if (marker == null) {
                Toast.makeText(
                    this@SelectLocationFragment.requireContext(),
                    getString(R.string.select_poi),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                _viewModel.navigationCommand.postValue(NavigationCommand.Back)
            }
        }
        return binding.root
    }

    private fun onPOIClicked() {
        googleMap.setOnPoiClickListener {
            marker?.remove()
            googleMap.clear()

            val locationSnippet = it.name
            _viewModel.updateClickedPoint(it.latLng, locationSnippet, it)
            marker = googleMap.addMarker(
                MarkerOptions().position(it.latLng).title(it.name).snippet(locationSnippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )

            addCircle(it.latLng)

            marker?.showInfoWindow()
        }
    }

    private fun onMapClicked() {
        googleMap.setOnMapClickListener {
            marker?.remove()
            googleMap.clear()

            val locationSnippet = "${it.latitude}, ${it.longitude}"
            _viewModel.updateClickedPoint(it, locationSnippet)

            marker = googleMap.addMarker(
                MarkerOptions().position(it).title(getString(R.string.dropped_pin))
                    .snippet(locationSnippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )

            addCircle(it)
            marker?.showInfoWindow()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
        activityResultLauncher.launch(PERMISSIONS)

        initMapUI()
        onPOIClicked()
        onMapClicked()
    }


    private fun addCircle(center: LatLng) {
        var circleOptions = CircleOptions().apply {
            center(center)
            radius(GEOFENCE_RADIUS_IN_METERS)
            strokeColor(Color.argb(255, 255, 99, 105))
            strokeWidth(2f)
            fillColor(Color.argb(60, 255, 99, 105))
        }

        googleMap.addCircle(circleOptions)
    }

    @SuppressLint("MissingPermission")
    private fun initMapUI() {
        _viewModel.locationAccepted.observe(viewLifecycleOwner) {
            val lat = _viewModel.latitude.value
            val long = _viewModel.longitude.value
            val locationSnippet = _viewModel.reminderSelectedLocationStr.value
            if (lat != null && long != null && locationSnippet != null) {
                val latLng = LatLng(lat, long)
                marker = googleMap.addMarker(
                    MarkerOptions().position(latLng).title(getString(R.string.dropped_pin))
                        .snippet(locationSnippet)
                )
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(latLng, 15.0f)
                )
            }
        }

        with(googleMap) {
            this.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.google_maps_style
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHelper.stopLocationUpdates()
    }

    override fun startLocationRequest() {
        locationHelper.startLocationUpdates()
    }

    override fun getLastKnownLocation(location: Location?) {
        _viewModel.locationAccepted.value = location
    }


    override fun showSnackBar() {
        Snackbar.make(
            binding.root,
            R.string.permission_denied_explanation,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                requireActivity().startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
    }


    @SuppressLint("MissingPermission")
    override fun onMyLocationClick(p0: Location) {
        googleMap.setPadding(0, 0, 0, 200)
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(p0.latitude, p0.longitude), 19.0f
            )
        )
    }

    override fun onMyLocationButtonClick(): Boolean {
        locationHelper.checkDeviceLocationSettingsAndStartGeofence()
        return false
    }

}
