package sk.sandeep.runningapp.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.GoogleMap
import dagger.hilt.android.AndroidEntryPoint
import sk.sandeep.runningapp.R
import sk.sandeep.runningapp.databinding.FragmentTrackingBinding
import sk.sandeep.runningapp.services.TrackingService
import sk.sandeep.runningapp.util.Constants.ACTION_START_OR_RESUME_SERVICE
import sk.sandeep.runningapp.view_model.MainViewModel

@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {
    private val viewModel: MainViewModel by viewModels()
    private var map: GoogleMap? = null
    private lateinit var binding: FragmentTrackingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTrackingBinding.inflate(inflater, container, false)
        context ?: return binding.root


        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync {
            map = it
        }

        binding.btnToggleRun.setOnClickListener{
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
        return binding.root
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
}
