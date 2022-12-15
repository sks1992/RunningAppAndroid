package sk.sandeep.runningapp.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import sk.sandeep.runningapp.R
import sk.sandeep.runningapp.databinding.FragmentTrackingBinding
import sk.sandeep.runningapp.services.Polyline
import sk.sandeep.runningapp.services.TrackingService
import sk.sandeep.runningapp.util.Constants.ACTION_PAUSE_SERVICE
import sk.sandeep.runningapp.util.Constants.ACTION_START_OR_RESUME_SERVICE
import sk.sandeep.runningapp.util.Constants.ACTION_STOP_SERVICE
import sk.sandeep.runningapp.util.Constants.MAP_ZOOM
import sk.sandeep.runningapp.util.Constants.POLYLINE_COLOR
import sk.sandeep.runningapp.util.Constants.POLYLINE_WIDTH
import sk.sandeep.runningapp.util.TrackingUtility
import sk.sandeep.runningapp.view_model.MainViewModel

@AndroidEntryPoint
class TrackingFragment : Fragment(), MenuProvider {
    private val viewModel: MainViewModel by viewModels()
    private var map: GoogleMap? = null
    private lateinit var binding: FragmentTrackingBinding
    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private var curTimeInMillis = 0L

    private var menuHost: MenuHost? = null
    private var menu: Menu? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrackingBinding.inflate(inflater, container, false)

        menuHost = requireActivity()
        menuHost?.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnToggleRun.setOnClickListener {
            // sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
            toggleRun()
        }
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync {
            map = it
            addAllPolyLines()
        }
        subScribeToObservers()
    }

    private fun subScribeToObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner) {
            updateTracking(it)
        }

        TrackingService.pathPoints.observe(viewLifecycleOwner) {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        }

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner) {
            curTimeInMillis = it
            val formattedTime = TrackingUtility.getFormattedStopWatchTime(
                curTimeInMillis, true
            )

            binding.tvTimer.text = formattedTime
        }
    }

    private fun toggleRun() {
        if (isTracking) {
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun updateTracking(isTrackings: Boolean) {
        isTracking = isTrackings
        if (!isTracking) {
            binding.btnToggleRun.text = getString(R.string.start)
            binding.btnFinishRun.visibility = View.VISIBLE
        } else {
            binding.btnToggleRun.text = getString(R.string.stop)
            menu?.getItem(0)?.isVisible = true
            binding.btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCameraToUser() {
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun addAllPolyLines() {
        for (polyline in pathPoints) {
            val polylineOptions = PolylineOptions().color(POLYLINE_COLOR).width(
                POLYLINE_WIDTH
            ).addAll(polyline)
            map?.addPolyline(polylineOptions)
        }
    }

    //this function only add two last position not all positions
    private fun addLatestPolyline() {
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val secondLastLatLng = pathPoints.last()[pathPoints.last().size - 2]
            val lastLatLng = pathPoints.last().last()
            val polylineOptions = PolylineOptions().color(POLYLINE_COLOR).width(
                POLYLINE_WIDTH
            ).add(secondLastLatLng).add(lastLatLng)

            map?.addPolyline(polylineOptions)
        }
    }

    private fun sendCommandToService(action: String) = Intent(
        requireContext(), TrackingService::class.java
    ).also {
        it.action = action
        requireContext().startService(it)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.toolbar_tracking_menu, menu)
        this.menu = menu
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.miCancelTracking -> {
                showCancelTrackingDialog()
                true
            }
            else -> false
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)

        if (curTimeInMillis > 0L) {
            this.menu?.getItem(0)?.isVisible = true
        }
    }

    private fun showCancelTrackingDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.AlertDialogTheme)
            .setTitle("Cancel the Run?")
            .setMessage("Are you sure to cancel the current run and delete all its data?")
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Yes") { _, _ ->
                stopRun()
            }
            .setNegativeButton("No") { dialogInterface, _ ->
                dialogInterface.cancel()
            }
            .create()
        dialog.show()
    }

    private fun stopRun() {
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }
}