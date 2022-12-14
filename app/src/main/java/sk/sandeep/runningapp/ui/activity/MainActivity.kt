package sk.sandeep.runningapp.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import dagger.hilt.android.AndroidEntryPoint
import sk.sandeep.runningapp.R
import sk.sandeep.runningapp.databinding.ActivityMainBinding
import sk.sandeep.runningapp.util.Constants.ACTION_SHOW_TRACKING_FRAGMENT

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this, R.layout.activity_main
        )


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.findNavController()

        navController
            .addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.settingFragment, R.id.runFragment,
                    R.id.statisticsFragment ->
                        binding.bottomNavigationView.visibility = View.VISIBLE

                    else -> binding.bottomNavigationView.visibility = View.GONE
                }
            }
        navigateToTrackingFragmentIfNeeded(intent)
        /**
        For Adding toolbar functionality
         * */
        setSupportActionBar(binding.toolbar)

        /**
        For Adding BottomNavigation functionality
         * */
        binding.bottomNavigationView.setupWithNavController(navController)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_TRACKING_FRAGMENT) {
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
            val navController = navHostFragment.findNavController()
            navController
                .navigate(R.id.action_global_trackingFragment)
        }
    }
}