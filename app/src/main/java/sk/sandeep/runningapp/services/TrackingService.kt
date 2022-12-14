package sk.sandeep.runningapp.services

import android.content.Intent
import androidx.lifecycle.LifecycleService
import sk.sandeep.runningapp.util.Constants.ACTION_PAUSE_SERVICE
import sk.sandeep.runningapp.util.Constants.ACTION_START_OR_RESUME_SERVICE
import sk.sandeep.runningapp.util.Constants.ACTION_STOP_SERVICE
import timber.log.Timber

//service() and IntentService() but if we want to use lifecycle owner to
// observe livedata so we use LifecycleService
// to communicate between  activity/fragment to service we use intent
//if we want to pass data between service to fragment we use Singleton Pattern
// or make this service as bound service
class TrackingService : LifecycleService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    Timber.d("Started or resumed service")
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused service")
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("Stop Service")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
}