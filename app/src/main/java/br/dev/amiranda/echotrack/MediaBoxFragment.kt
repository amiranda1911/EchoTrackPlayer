package br.dev.amiranda.echotrack

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView


class MediaBoxFragment : Fragment() {
    private var fileList: ListView? = null

    private var playerMessenger: Messenger? = null
    private var bound = false

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
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inicia o serviço se não foi iniciado
        val intent = Intent(requireContext(), PlayerService::class.java)
        requireContext().startService(intent) // Inicia o serviço
        requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE) // Conecta ao serviço

        val view = inflater.inflate(R.layout.fragment_media_box, container, false)
        fileList = view.findViewById<ListView>(R.id.track_list_view)

        // Music list
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, searchAudioFiles(requireContext()))
        fileList?.adapter = adapter
        fileList?.setOnItemClickListener { parent, view, position, id ->
            val selectedFile = parent.getItemAtPosition(position) as String
            playFile(selectedFile)
        }

        // Inflate the layout for this fragment
        return view
    }


    fun playFile(actfile: String) {
        val msg = Message.obtain()
        val bundle = Bundle()
        bundle.putString("action", "play")
        bundle.putString("args", actfile)
        msg.data = bundle
        playerMessenger?.send(msg)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (bound) {
            requireContext().unbindService(serviceConnection)
            bound = false
        }
    }
}


