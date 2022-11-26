package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)
        initData()
        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        geofenceClient = LocationServices.getGeofencingClient(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            if (!this::reminderDataItem.isInitialized) {
                reminderDataItem = ReminderDataItem(
                    title,
                    description,
                    location,
                    latitude,
                    longitude
                )
            }

            if (_viewModel.validateEnteredData(reminderDataItem)) {
                _viewModel.validateAndSaveReminder(reminderDataItem)
                listenToReminderGeofence(
                    reminderDataItem.latitude!!,
                    reminderDataItem.longitude!!,
                    reminderDataItem.id
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun listenToReminderGeofence(latitude: Double, longitude: Double, id: String) {
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

        geofenceClient.removeGeofences(geofencePendingIntent).run {
            addOnCompleteListener {
                geofenceClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                    addOnSuccessListener {
                        Log.d("Add_geofence", " geofence Added with reminder id $id successfully.")

                    }
                    addOnFailureListener {
                        _viewModel.showSnackBarInt.postValue(R.string.error_adding_geofence)
                        it.message?.let { message ->
                            Log.d("Add_geofence", message)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
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
}
