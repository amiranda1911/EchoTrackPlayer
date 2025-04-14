package br.dev.amiranda.echotrack

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import br.dev.amiranda.echotrack.ui.main.MediaBoxFragment

class MainActivity : FragmentActivity() {
    private lateinit var receiver: BroadcastReceiver
    private var playerMessenger: Messenger? = null
    private var bound = false

    private lateinit var miniPlayerView: ConstraintLayout
    private lateinit var playProgressView: ProgressBar
    private lateinit var thumbnailView: ImageView
    private lateinit var titleView: TextView
    private lateinit var artistView: TextView

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            playerMessenger = Messenger(service)
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerMessenger = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        miniPlayerView = findViewById(R.id.miniPlayerView)
        playProgressView = findViewById(R.id.playProgressView)
        thumbnailView = findViewById(R.id.thumbnailView)
        titleView = findViewById(R.id.titleView)
        artistView = findViewById(R.id.artistiView)

        registerAllReceivers()

        val intent = Intent(this, PlayerService::class.java)
        startService(intent) // Inicia o serviço
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE) // Conecta ao serviço



        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                )

        if (savedInstanceState == null) { // Evita recriar o Fragment ao girar a tela
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerView, MediaBoxFragment())
                .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) {
            unbindService(serviceConnection)
            bound = false
        }
        unregisterReceiver(receiver)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerAllReceivers(){
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.e("MeuReceiver", "Broadcast recebido!")
                Log.e("MeuReceiver", "Action: ${intent?.action}")
                if (intent?.action.equals("br.dev.amiranda.EchoTrack.PlayerStart")) {
                    val title = intent?.getStringExtra("title")
                    val artist = intent?.getStringExtra("artist")
                    val album = intent?.getStringExtra("album")
                    val thumb = intent?.getStringExtra("thumb")

                    var bitmap: Bitmap? = null
                    if (thumb != null) {
                        val uri = Uri.parse(thumb)
                        try {
                            val inputStream = getContentResolver().openInputStream(uri)
                            bitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream?.close();
                            // Usa o bitmap como quiser
                        } catch (e : Exception) {
                            e.printStackTrace();
                        }
                    }

                    thumbnailView.setImageBitmap(bitmap)
                    titleView.text = title
                    artistView.text = artist

                    Toast.makeText(this@MainActivity, "Tocando: $thumb", Toast.LENGTH_SHORT).show()
                }
            }
        }
        val filter = IntentFilter("br.dev.amiranda.EchoTrack.PlayerStart")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)

        } else {
            @Suppress("DEPRECATION")
            registerReceiver(receiver, filter)
        }
    }

    fun sendToService(action: String, args: String = "") {
        val msg = Message.obtain(null, 0)
        val bundle = Bundle()
        bundle.putString("action", action)
        bundle.putString("args", args)
        msg.data = bundle
        playerMessenger?.send(msg)
    }

    fun playFile(actfile: String?) {
        val msg = Message.obtain()
        val bundle = Bundle()
        bundle.putString("action", "play")
        bundle.putString("args", actfile)
        msg.data = bundle
        playerMessenger?.send(msg)
    }

    fun searchAudioFiles(): List<String> {
        val auidioFilesList = mutableListOf<String>()

        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA
        )

        val cursor = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            while (it.moveToNext()) {
                val filePath =
                    it.getString(4)
                auidioFilesList.add(filePath)
            }
        }
        return auidioFilesList
    }
}

