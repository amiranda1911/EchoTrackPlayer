package br.dev.amiranda.echotrack

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class PlayerService : Service() {
    companion object {
        private var CHANNEL_ID = "ECHO_PULSE_CHANNEL"
        private var NOTIFICATION_ID = 1
        private var playerRunning = false
        private var playTrack = false

        private lateinit var notificationBuilder : NotificationCompat.Builder
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        createNotification()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotification()

        startForeground(NOTIFICATION_ID, notificationBuilder.build())


        notificationBuilder.setContentText("resrd")
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    private fun createNotification() {
        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Track")
            .setContentText("Artist")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
        /*
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define.
            if (ActivityCompat.checkSelfPermission(
                    this@PlayerService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notify(NOTIFICATION_ID, notificationBuilder.build())

        }
        */


    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        val channel = NotificationChannel(
            CHANNEL_ID, "nchannelname",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "descriptionText"
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d("PlayerService", "Notification Channel Created" )
    }
    
}