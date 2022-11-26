package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver.Companion.GEOFENCE_RADIUS_IN_METERS
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.*
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback, MLocationHelper,
    PermissionHelper {

    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var googleMap: GoogleMap
    private var marker: Marker? = null

    lateinit var snackBar: Snackbar

    private val locationHelper: LocationHelperManger by inject { parametersOf(this) }
    private val permissionManager: PermissionHelperManager by inject {
        parametersOf(
            this@SelectLocationFragment.requireActivity(), this
        )
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

    override fun onResume() {
        super.onResume()
        permissionManager.requestLocationPermission()
        locationHelper.startLocationUpdates()
    }

    private fun onLocationSelected() {
        onPOIClicked()
        onMapClicked()
    }

    private fun onPOIClicked() {
        googleMap.setOnPoiClickListener {
            marker?.remove()
            googleMap.clear()

            val locationSnippet = it.name
            _viewModel.updateClickedPoint(it.latLng, locationSnippet, it)

            if (permissionManager.areAllPermissionsGranted()) {
                googleMap.uiSettings.isMyLocationButtonEnabled = true
                googleMap.uiSettings.isZoomControlsEnabled = true
            }

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
        if (permissionManager.areAllPermissionsGranted()) {
            googleMap.uiSettings.isZoomControlsEnabled = true
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
        }
        initMapUI()
        onLocationSelected()
        googleMap.setOnMyLocationButtonClickListener {
            locationHelper.getLastKnownLocation()
            true
        }
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

    private fun initMapUI() {
        val lat = _viewModel.latitude.value
        val long = _viewModel.longitude.value
        val locationSnippet = _viewModel.reminderSelectedLocationStr.value

        with(googleMap) {
            this.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.google_maps_style
                )
            )
        }

        if (lat != null && long != null && locationSnippet != null) {
            val latLng = LatLng(lat, long)
            marker = googleMap.addMarker(
                MarkerOptions().position(latLng).title(getString(R.string.dropped_pin))
                    .snippet(locationSnippet)
            )
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, 15.0f)
            )
        } else if (permissionManager.areAllPermissionsGranted()) {
            locationHelper.startLocationUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHelper.stopLocationUpdates()
    }

    override fun locationPermissionAccepted() {
        locationHelper.startLocationUpdates()
    }

    override fun locationPermissionDenied() {
        hasGPSSignal(false)
    }

    override fun hasGPSSignal(isGPSEnabled: Boolean) {
        if (!this::snackBar.isInitialized) {
            snackBar = Snackbar.make(
                binding.root, getString(R.string.gps_request_message), Snackbar.LENGTH_INDEFINITE
            )
            snackBar.setAction(getString(R.string.yes)) {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                snackBar.dismiss()
            }
            snackBar.setActionTextColor(
                ContextCompat.getColor(
                    this.requireContext(),
                    R.color.colorAccent
                )
            )
        }

        if (!isGPSEnabled) snackBar.show()
        else snackBar.dismiss()
    }

    override fun onLocationChanged(location: Location?) {
        location?.let {
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15.0f)
            )
        }
    }

    override fun getLastKnownLocation(location: Location?) {
        location?.let {
            googleMap.setPadding(0, 0, 0, 200)
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(it.latitude, it.longitude), 15.0f
                )
            )
        }
    }

}
