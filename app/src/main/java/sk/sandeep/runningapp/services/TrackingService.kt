package sk.sandeep.runningapp.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import sk.sandeep.runningapp.*
import sk.sandeep.runningapp.R
import sk.sandeep.runningapp.util.Constants.ACTION_PAUSE_SERVICE
import sk.sandeep.runningapp.util.Constants.ACTION_START_OR_RESUME_SERVICE
import sk.sandeep.runningapp.util.Constants.ACTION_STOP_SERVICE
import sk.sandeep.runningapp.util.Constants.FASTEST_LOCATION_INTERVAL
import sk.sandeep.runningapp.util.Constants.LOCATION_UPDATE_INTERVAL
import sk.sandeep.runningapp.util.Constants.NOTIFICATION_CHANNEL_ID
import sk.sandeep.runningapp.util.Constants.NOTIFICATION_CHANNEL_NAME
import sk.sandeep.runningapp.util.Constants.NOTIFICATION_ID
import sk.sandeep.runningapp.util.Constants.TIMER_UPDATE_INTERVAL
import sk.sandeep.runningapp.util.Constants.flags
import sk.sandeep.runningapp.util.TrackingUtility
import timber.log.Timber
import javax.inject.Inject

typealias Polyline = MutableList<LatLng>
typealias Polylines = MutableList<Polyline>

//service() and IntentService() but if we want to use lifecycle owner to
// observe livedata so we use LifecycleService
// to communicate between  activity/fragment to service we use intent
//if we want to pass data between service to fragment we use Singleton Pattern
// or make this service as bound service

@AndroidEntryPoint
class TrackingService : LifecycleService() {
    var isFirstRun = true

    @Inject
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val timeRunInSeconds = MutableLiveData<Long>()

    @Inject
    lateinit var baseNotificationBuilder: NotificationCompat.Builder

    lateinit var curNotificationBuilder: NotificationCompat.Builder

    companion object {
        val timeRunInMillis = MutableLiveData<Long>()
        val isTracking = MutableLiveData<Boolean>()
        val pathPoints = MutableLiveData<Polylines>()
    }


    private fun postInitialValues() {
        isTracking.postValue(false)
        pathPoints.postValue(mutableListOf())
        timeRunInSeconds.postValue(0L)
        timeRunInMillis.postValue(0L)
    }

    override fun onCreate() {
        super.onCreate()
        curNotificationBuilder = baseNotificationBuilder
        postInitialValues()
        fusedLocationProviderClient = getFusedLocationProviderClient(this)

        isTracking.observe(this) {
            updateLocationTracking(it)
            updateNotificationTrackingState(it)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false
                        Timber.d("1. service start")
                    } else {
                        Timber.d("Resuming service")
                        startTimer()
                    }
                    //Timber.d("Started or resumed service")
                }
                ACTION_PAUSE_SERVICE -> {
                    Timber.d("Paused service")
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {
                    Timber.d("Stop Service")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private var isTimerEnabled = false
    private var lapTime = 0L
    private var timeRun = 0L
    private var timeStarted = 0L
    private var lastSecondTimesStamp = 0L


    private fun startTimer() {
        addEmptyPolyline()
        isTracking.postValue(true)
        timeStarted = System.currentTimeMillis()
        isTimerEnabled = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isTracking.value!!) {
                //the differance between now and timeStarted
                lapTime = System.currentTimeMillis() - timeStarted
                //post the new lap time
                timeRunInMillis.postValue(timeRun + lapTime)
                if (timeRunInMillis.value!! >= lastSecondTimesStamp + 2000L) {
                    timeRunInSeconds.postValue(timeRunInSeconds.value!! + 2)
                    lastSecondTimesStamp += 2000L
                }
                delay(TIMER_UPDATE_INTERVAL)
            }
            timeRun += lapTime
        }
    }

    private fun pauseService() {
        isTracking.postValue(false)
        isTimerEnabled = false
    }

    private fun updateNotificationTrackingState(isTracking: Boolean) {
        val notificationActionText = if (isTracking) "Pause" else "Resume"
        val pendingIntent = if (isTracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(
                this, 1, pauseIntent,
                flags
            )
        } else {
            val resumeIntent = Intent(this, TrackingService::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, flags)
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        curNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(curNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }
        curNotificationBuilder = baseNotificationBuilder
            .addAction(
                R.drawable.ic_pause_black_24dp,
                notificationActionText,
                pendingIntent
            )
        notificationManager.notify(NOTIFICATION_ID, curNotificationBuilder.build())
    }


    @SuppressLint("MissingPermission")
    private fun updateLocationTracking(isTracking: Boolean) {
        if (isTracking) {
            if (TrackingUtility.hasLocationPermissions(this)) {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    LOCATION_UPDATE_INTERVAL
                )
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(LOCATION_UPDATE_INTERVAL)
                    .setMaxUpdateDelayMillis(FASTEST_LOCATION_INTERVAL)
                    .build()

                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallBack,
                    Looper.getMainLooper()
                )
            } else {
                fusedLocationProviderClient.removeLocationUpdates(
                    locationCallBack
                )
            }
        }
    }

    private val locationCallBack = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            if (isTracking.value!!) {
                p0.locations.let {
                    for (location in it) {
                        addPathPoint(location)
                        Timber.d("New Location: ${location.latitude}, ${location.longitude}")
                    }
                }
            }
        }
    }

    private fun addPathPoint(location: Location?) {
        location?.let {
            val position = LatLng(location.latitude, location.longitude)
            pathPoints.value?.apply {
                last().add(position)
                pathPoints.postValue(this)
            }
        }
    }

    private fun addEmptyPolyline() = pathPoints.value?.apply {
        add(mutableListOf())
        pathPoints.postValue(this)
    } ?: pathPoints.postValue(mutableListOf(mutableListOf()))

    private fun startForegroundService() {
        startTimer()
        isTracking.postValue(true)
        val notificationManger =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManger)
        }

        Timber.d("2. service start")
        startForeground(NOTIFICATION_ID, baseNotificationBuilder.build())

        timeRunInSeconds.observe(this) {
            val notification =
                curNotificationBuilder.setContentText(
                    TrackingUtility.getFormattedStopWatchTime(it * 1000L)
                )
            notificationManger.notify(NOTIFICATION_ID, notification.build())
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}