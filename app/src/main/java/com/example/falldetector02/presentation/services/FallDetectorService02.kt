package com.example.falldetector02.presentation.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.falldetector02.R
import com.example.falldetector02.di.ServiceModule
import com.example.falldetector02.presentation.util.other.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
@SuppressLint("LongLogTag")
class FallDetectorService02 : LifecycleService() {


    /**
    onCreate() is called when the Service object is instantiated (ie: when the service is created).
    You should do things in this method that you need to do only once (ie: initialize some variables, etc.).
    onCreate() will only ever be called once per instantiated object.
    You only need to implement onCreate() if you actually want/need to initialize something only once.
    onStartCommand() is called every time a client starts the service using startService(Intent intent).
    This means that onStartCommand() can get called multiple times. You should do the things in this method that are needed each time a client requests something from your service.
    This depends a lot on what your service does and how it communicates with the clients (and vice-versa).
    If you don't implement onStartCommand() then you won't be able to get any information from the Intent that the client passes to onStartCommand() and your service might not be able to do any useful work.
     * */


    val TAG = "AppDebug FallDetectorService02"


    companion object {
        val isDetecting = MutableLiveData<Boolean>(false)
    }

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null


    @Inject
    lateinit var fallingSensorManager: FallingSensorManager

    var serviceKilled = false


    @Inject
    @ServiceModule.Impl2
    lateinit var topNotificationBuilder: NotificationCompat.Builder

    @Inject
    @ServiceModule.Impl1
    lateinit var detectionNotificationBuilder: NotificationCompat.Builder

    //lateinit var topNotificationBuilder: NotificationCompat.Builder

    // lateinit var detectionNotificationBuilder: NotificationCompat.Builder


    private val sensorListener: SensorEventListener? = object : SensorEventListener {
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }

        @SuppressLint("LongLogTag")
        override fun onSensorChanged(event: SensorEvent?) {

            if (fallingSensorManager?.isFallHappened(event) == true) {

                Log.i(TAG, "onSensorChanged fall detected: ")
                Log.i(TAG, "onSensorChanged timestamp ${event!!.timestamp}: ")
                val duration: Long =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                        System.currentTimeMillis() - (System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000)
                    } else {
                        System.currentTimeMillis() - (System.currentTimeMillis() + (event.timestamp - System.nanoTime()) / 1000000)
                    }
                Log.i(TAG, "onSensorChanged duration ${duration}: ")

                //  TODO(inserting to db happen here)

                notifyDetection(duration = duration)

            }

        }

        private fun notifyDetection(duration: Long) {

            val id = (System.currentTimeMillis() / 1000).toDouble().roundToInt()
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel(
                    notificationManager,
                    "${NOTIFICATION_CHANNEL_ID}.new_detection"
                )
            }
            detectionNotificationBuilder
                .setContentTitle("Fall detected")
                .setContentText("fall detection duration: $duration ms")
                .setAutoCancel(true)
                .setOngoing(false)
            notificationManager.notify(
                id,
                detectionNotificationBuilder.build()
            )
        }

        // TODO(inserting to db happen here)

    }



    /**onCreate:
     * The system invokes this method to perform one-time setup procedures when the service is initially created (before it calls either onStartCommand() or onBind()).
     * If the service is already running, this method is not called.
     * */

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: ")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        topNotificationBuilder = topNotificationBuilder
        detectionNotificationBuilder = detectionNotificationBuilder

        postInitialValues()

        isDetecting.observe(this, {
            updateNotificationDetectorState(it)
        })

    }


    /**  onStartCommand:
    The system invokes this method by calling startService() when another component (such as an activity) requests that the service be started.
    When this method executes, the service is started and can run in the background indefinitely.
    If you implement this, it is your responsibility to stop the service when its work is complete by calling stopSelf() or stopService().
    If you only want to provide binding, you don't need to implement this method.

     * */

    @SuppressLint("LongLogTag")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: ")
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    Log.i(TAG, "ACTION_START_OR_RESUME_SERVICE: ")
                    startMyForegroundService()
                }
                ACTION_PAUSE_SERVICE -> {
                    Log.i(TAG, "ACTION_PAUSE_SERVICE: ")
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {
                    Log.i(TAG, "ACTION_STOP_SERVICE: ")
                    killService()
                }
                else -> {
                    Log.i(TAG, "else happened: ")
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateNotificationDetectorState(isDetecting: Boolean) {

        val notificationActionText: String


        if (isDetecting) {
            notificationActionText = "Pause"
            topNotificationBuilder
                .setContentText("Fall Detector is running...")
                .setAutoCancel(false)
                .setOngoing(true)
        } else {
            notificationActionText = "Resume"
            topNotificationBuilder
                .setContentText("Fall Detector has been stopped")
                .setAutoCancel(true)
                .setOngoing(false)
        }


        val pendingIntent = if (isDetecting) {
            val pauseIntent = Intent(this, FallDetectorService02::class.java).apply {
                action = ACTION_PAUSE_SERVICE
            }
            PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            val resumeIntent = Intent(this, FallDetectorService02::class.java).apply {
                action = ACTION_START_OR_RESUME_SERVICE
            }
            PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        topNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(topNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }

        if (!serviceKilled) {
            topNotificationBuilder = topNotificationBuilder
                .addAction(R.drawable.ic_pause_black_24dp, notificationActionText, pendingIntent)
            notificationManager.notify(NOTIFICATION_ID, topNotificationBuilder.build())
        }
    }

    private fun postInitialValues() {
        isDetecting.postValue(false)
    }


    private fun startMyForegroundService() {
        isDetecting.postValue(true)

        //fallingSensorManager = FallingSensorManager()
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager.registerListener(
            sensorListener,
            sensor,
            SensorManager.SENSOR_DELAY_UI
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager, NOTIFICATION_CHANNEL_ID)
        }

    }


    private fun pauseService() {
        isDetecting.postValue(false)
        sensorManager.unregisterListener(sensorListener, sensor)
    }


    private fun killService() {
        serviceKilled = true
        sensorManager.unregisterListener(sensorListener, sensor)
        pauseService()
        postInitialValues()
        stopForeground(true)
        stopSelf()
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        notificationManager: NotificationManager, notificationChannelId: String
    ) {
        val channel = NotificationChannel(
            notificationChannelId,
            "${notificationChannelId}.name",//NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }


    /** onDestroy:
     * The system invokes this method when the service is no longer used and is being destroyed.
     * Your service should implement this to clean up any resources such as threads, registered listeners, or receivers.
     * This is the last call that the service receives.
     * */

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: ")
        super.onDestroy()
    }



    /** onBind
    The system invokes this method by calling bindService() when another component wants to bind with the service (such as to perform RPC).
    In your implementation of this method, you must provide an interface that clients use to communicate with the service by returning an IBinder.
    You must always implement this method; however, if you don't want to allow binding, you should return null.
     *
     * */

    override fun onBind(intent: Intent): IBinder? {
        Log.i(TAG, "onBind: ")
        return super.onBind(intent)
    }

    /** getLifecycle:
     * Returns the Lifecycle(Defines an object that has an Android Lifecycle) of the provider.
     * */
    override fun getLifecycle(): Lifecycle {
        Log.i(TAG, "getLifecycle: ")
        return super.getLifecycle()
    }
}