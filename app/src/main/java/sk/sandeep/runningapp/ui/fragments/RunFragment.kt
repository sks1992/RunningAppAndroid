package sk.sandeep.runningapp.ui.fragments

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import sk.sandeep.runningapp.R
import sk.sandeep.runningapp.databinding.FragmentRunBinding
import sk.sandeep.runningapp.util.Constants.REQUEST_CODE_LOCATION_PERMISSION
import sk.sandeep.runningapp.util.TrackingUtility
import sk.sandeep.runningapp.view_model.MainViewModel

@AndroidEntryPoint
class RunFragment : Fragment(), EasyPermissions.PermissionCallbacks {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRunBinding.inflate(inflater, container, false)
        context ?: return binding.root
        requestPermission()
        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_runFragment_to_trackingFragment)
        }

        return binding.root
    }


    private fun requestPermission() {
        if (TrackingUtility.hasLocationPermissions(requireContext())) {
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "You Need To Accept Location Permissions To  Use This App.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )
        }else {
            EasyPermissions.requestPermissions(
                this,
                "You Need To Accept Location Permissions To  Use This App.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            )
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms)){
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermission()
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

}