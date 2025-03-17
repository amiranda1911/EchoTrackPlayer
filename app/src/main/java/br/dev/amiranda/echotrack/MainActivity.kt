package br.dev.amiranda.echotrack

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.ComponentActivity


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(this, PlayerService::class.java))
        val fileList: ListView = findViewById<ListView>(R.id.track_list_view)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, searchAudioFiles(this))
        fileList.adapter = adapter
    }
}


fun searchAudioFiles(context: Context): List<String> {
    val auidioFilesList = mutableListOf<String>()

    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Video.Media.TITLE,
        MediaStore.Video.Media.ALBUM,
        MediaStore.Video.Media.ARTIST,
        MediaStore.Video.Media.YEAR
    )

    val cursor = context.contentResolver.query(uri, projection, null,null, null)

    cursor?.use {
        //val columnIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        while(it.moveToNext()) {
            val filePath = it.getString(0) + " - " + it.getString(1) + " - " + it.getString(2) + " - " + it.getString(3)
            auidioFilesList.add(filePath)
        }
    }
    return auidioFilesList
}