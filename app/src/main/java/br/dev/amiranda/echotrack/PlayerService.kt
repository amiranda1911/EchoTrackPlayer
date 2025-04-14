package br.dev.amiranda.echotrack

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class PlayerService : Service() {
    companion object {
        private var CHANNEL_ID = "ECHO_PULSE_CHANNEL"
        private var NOTIFICATION_ID = 1
        private var playerRunning = false

        private val pcmPlayer = PCMPlayer()

        @SuppressLint("StaticFieldLeak")
        private lateinit var notificationBuilder : NotificationCompat.Builder
        private lateinit var notificationManager : NotificationManager
    }

    private val messenger = Messenger(handler())

    private inner class handler : Handler(Looper.getMainLooper()) {

        override fun handleMessage(msg: Message) {
            val data = msg.data
            val action = data.getString("action").toString()
            val args = data.getString("args").toString()

            when(action) {
                "play" -> playFile(args)
                "stop" -> pcmPlayer.stop()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val mChannel = NotificationChannel(CHANNEL_ID, "Player Notifications", NotificationManager.IMPORTANCE_HIGH)
        mChannel.description = "nPlayerChannel"

        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)

        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Track")
            .setContentText("Artist")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)

        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return messenger.binder
    }

    private fun playFile(filepath: String) {
        if(playerRunning){
            pcmPlayer.stop()
        }
        metadataFileNotification(filepath)
        pcmPlayer.setAudioFile(filepath)
        playerRunning = true
        pcmPlayer.start()
    }

    private fun metadataFileNotification(file: String) {
        val retriever = MediaMetadataRetriever()
        val metadata = mutableMapOf<String, String>()
        try{
            retriever.setDataSource(file)

            val thumb = File(cacheDir, "thumb_temp.jpg")
            val imagemBytes = retriever.getEmbeddedPicture();
            var thumbnail : Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
            if (imagemBytes != null) {
                thumbnail = BitmapFactory.decodeByteArray(imagemBytes, 0, imagemBytes.size);
            }

            val fos = FileOutputStream(thumb);
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            fos.close();

            val uri = FileProvider.getUriForFile(this, "br.dev.amiranda.fileprovider", thumb);

            metadata["title"] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Uknow"
            metadata["artist"] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Uknow"
            metadata["album"] = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Uknow"


            notificationBuilder.setContentTitle(metadata["title"]).setSilent(true)
            notificationBuilder.setContentText(metadata["artist"]).setSilent(true)

            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

            val playerStartIntentBroadcast = Intent("br.dev.amiranda.EchoTrack.PlayerStart")
            playerStartIntentBroadcast.putExtra("thumb", uri.toString())
            playerStartIntentBroadcast.putExtra("title", metadata["title"])
            playerStartIntentBroadcast.putExtra("artist", metadata["artist"])
            playerStartIntentBroadcast.putExtra("album", metadata["album"])
            sendBroadcast(playerStartIntentBroadcast)

        }catch (e : Exception){
            e.printStackTrace()
        }finally {
            retriever.release()
        }
    }
}