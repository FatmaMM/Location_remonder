package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.navArgs
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.LocationHelperManger
import com.udacity.project4.utils.MLocationHelper
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class SaveReminderFragment : BaseFragment(), MLocationHelper {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var reminderDataItem: ReminderDataItem
    private lateinit var geofenceClient: GeofencingClient
    private val args by navArgs<SaveReminderFragmentArgs>()
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = GeofenceBroadcastReceiver.GEOFENCE_EVENT
        PendingIntent.getBroadcast(
            requireContext(),
            1,
            intent,
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                else -> PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }
    private val locationHelper: LocationHelperManger by inject {
        parametersOf(
            requireActivity(),
            this
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)
        setDisplayHomeAsUpEnabled(true)
        initData()

        binding.viewModel = _viewModel
        geofenceClient = LocationServices.getGeofencingClient(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            this.reminderDataItem = ReminderDataItem(
                title,
                description,
                location,
                latitude,
                longitude
            )

            if (_viewModel.validateEnteredData(reminderDataItem)) {
                checkPermissionsAndStartGeofencing()
            }
        }
        _viewModel.showSnackBar.observe(viewLifecycleOwner) {
            Snackbar.make(binding.root, it.toString(), Snackbar.LENGTH_LONG).show()
        }
    }

    private val registration =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == RESULT_OK)
                if (_viewModel.validateEnteredData(reminderDataItem)) {
                    _viewModel.validateAndSaveReminder(reminderDataItem)
                    listenToReminderGeofence(
                        reminderDataItem.latitude!!,
                        reminderDataItem.longitude!!,
                        reminderDataItem.id
                    )
                }
        }

    private fun checkDeviceLocationSettingsAndStartGeofence(v: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(this.requireActivity())
        builder.setAlwaysShow(true)
        val locationSettingsResponseTask = settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { ex ->
            if (ex is ResolvableApiException && v) {
                try {
                    val request: IntentSenderRequest = IntentSenderRequest.Builder(
                        ex.resolution.intentSender
                    ).setFillInIntent(Intent())
                        .build()
                    registration.launch(request)
                    Toast.makeText(
                        this.requireContext(),
                        getString(R.string.location_required_error),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (ex: ApiException) {
                    Log.e(
                        "activity",
                        "PendingIntent unable to execute request.",
                    )
                }
            } else {
                showSnackBar()
            }
        }

        locationSettingsResponseTask.addOnCompleteListener {
            startLocationRequest()
        }
    }


    @SuppressLint("MissingPermission")
    private fun listenToReminderGeofence(latitude: Double, longitude: Double, id: String) {
        if (!locationHelper.foregroundAndBackgroundLocationPermissionApproved())
            return

        val geofence = Geofence.Builder().setRequestId(id).setCircularRegion(
            latitude,
            longitude,
            GeofenceBroadcastReceiver.GEOFENCE_RADIUS_IN_METERS.toFloat()
        )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofenceClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                Log.d("Add_geofence", " geofence Added with reminder id $id successfully.")
            }
            addOnFailureListener {
                _viewModel.showSnackBarInt.postValue(R.string.error_adding_geofence)
            }
        }
    }
}
}

override fun onDestroy() {
    super.onDestroy()
    _viewModel.onClear()
}


private fun initData() {
    args.reminderData?.let {
        reminderDataItem = it
        _viewModel.apply {
            reminderTitle.postValue(it.title)
            reminderDescription.postValue(it.description)
            reminderSelectedLocationStr.postValue(it.location)
            latitude.postValue(it.latitude)
            longitude.postValue(it.longitude)
        }
    }
}


private var permissionsArray =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    } else arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION
    )

private var activityResultLauncher: ActivityResultLauncher<Array<String>> =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        var allAreGranted = true
        for (b in result.values) {
            allAreGranted = allAreGranted && b
        }
        if (allAreGranted) {
            checkDeviceLocationSettingsAndStartGeofence()
        } else {
            showSnackBar()
        }
    }


private fun checkPermissionsAndStartGeofencing() {
    if (locationHelper.foregroundAndBackgroundLocationPermissionApproved()) {
        checkDeviceLocationSettingsAndStartGeofence()
    } else {
        activityResultLauncher.launch(permissionsArray)
    }
}

companion object {
    private const val TAG = "SaveReminderFragment"
}

@SuppressLint("MissingPermission")
override fun getLastKnownLocation(location: Location?) {
}

override fun showSnackBar() {
    Snackbar.make(
        binding.root,
        R.string.location_required_error,
        Snackbar.LENGTH_INDEFINITE
    )
        .setAction(R.string.settings) {
            startActivity(Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }.show()
}

override fun startLocationRequest() {
    if (locationHelper.isLocationEnabled())
        if (_viewModel.validateEnteredData(reminderDataItem)) {
            _viewModel.validateAndSaveReminder(reminderDataItem)
            listenToReminderGeofence(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                reminderDataItem.id
            )
        } else {
            checkDeviceLocationSettingsAndStartGeofence()
        }
}

override fun onResume() {
    super.onResume()
    if (this@SaveReminderFragment.context == null) {
        recreateFragment()
    }
}

private fun recreateFragment() {
    parentFragmentManager.beginTransaction().detach(this).attach(this).commit()
}
}

