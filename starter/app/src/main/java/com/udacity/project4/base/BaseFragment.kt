package com.udacity.project4.base

import android.location.Location
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.utils.LocationHelperManger
import com.udacity.project4.utils.MLocationHelper
import com.udacity.project4.utils.PermissionHelper
import com.udacity.project4.utils.PermissionHelperManager
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

/**
 * Base Fragment to observe on the common LiveData objects
 */
abstract class BaseFragment : Fragment(), MLocationHelper, PermissionHelper {
    /**
     * Every fragment has to have an instance of a view model that extends from the BaseViewModel
     */
    abstract val _viewModel: BaseViewModel
     val locationHelper: LocationHelperManger by inject {
        parametersOf(
            this@BaseFragment.requireActivity(),
            this
        )
    }
     val permissionManager: PermissionHelperManager by inject {
        parametersOf(
            this@BaseFragment.requireActivity(), this
        )
    }

    override fun onStart() {
        super.onStart()
        _viewModel.showErrorMessage.observe(this, Observer {
            Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
        })
        _viewModel.showToast.observe(this, Observer {
            Toast.makeText(activity, it, Toast.LENGTH_LONG).show()
        })
        _viewModel.showSnackBar.observe(this, Observer {
            Snackbar.make(this.requireView(), it, Snackbar.LENGTH_LONG).show()
        })
        _viewModel.showSnackBarInt.observe(this, Observer {
            Snackbar.make(this.requireView(), getString(it), Snackbar.LENGTH_LONG).show()
        })

        _viewModel.navigationCommand.observe(this, Observer { command ->
            when (command) {
                is NavigationCommand.To -> findNavController().navigate(command.directions)
                is NavigationCommand.Back -> findNavController().popBackStack()
                is NavigationCommand.BackTo -> findNavController().popBackStack(
                    command.destinationId,
                    false
                )
            }
        })
    }
    override fun onLocationChanged(location: Location?) {

    }

    override fun getLastKnownLocation(location: Location?) {
    }

    override fun startLocationRequest() {
    }
}