package br.dev.amiranda.echotrack.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import br.dev.amiranda.echotrack.MainActivity
import br.dev.amiranda.echotrack.R


class MediaBoxFragment : Fragment() {
    private var fileList: ListView? = null
    private var files : List<String> = List<String>(0) {"Teste"}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_media_box, container, false)
        fileList = view.findViewById<ListView>(R.id.track_list_view)

        files = (activity as? MainActivity)?.searchAudioFiles() ?: listOf()

        // Music list
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            files
        )
        fileList?.adapter = adapter
        fileList?.setOnItemClickListener { parent, view, position, id ->
            val selectedFile = parent.getItemAtPosition(position) as String
            (activity as? MainActivity)?.playFile(selectedFile)
        }

        // Inflate the layout for this fragment
        return view
    }
}