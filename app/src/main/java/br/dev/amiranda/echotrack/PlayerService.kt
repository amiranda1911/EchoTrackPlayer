package br.dev.amiranda.echotrack

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.widget.Toast
import androidx.core.app.NotificationCompat

class PlayerService : Service() {
    companion object {
        private var CHANNEL_ID = "ECHO_PULSE_CHANNEL"
        private var NOTIFICATION_ID = 1
        private var playerRunning = false

        private val pcmPlayer = PCMPlayer()

        @SuppressLint("StaticFieldLeak")
        private lateinit var notificationBuilder : NotificationCompat.Builder
    }

    private val messenger = Messenger(handler())

    private inner class handler : Handler(Looper.getMainLooper()) {


        override fun handleMessage(msg: Message) {
            val data = msg.data
            val action = data.getString("action").toString()
            val args = data.getString("args").toString()

            when(action){
                "play" -> playFile(args)
                "stop" -> {
                    pcmPlayer.stop()
                    playerRunning = false
                }
            }

        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Track")
            .setContentText("Artist")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)

        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        notificationBuilder.setContentText("EchoTrack Player")
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return messenger.binder
    }

    private fun playFile(filepath: String) {
        if(playerRunning){
            pcmPlayer.stop()
        }

        pcmPlayer.setAudioFile(filepath)
        playerRunning = true
        pcmPlayer.start()

    }
}