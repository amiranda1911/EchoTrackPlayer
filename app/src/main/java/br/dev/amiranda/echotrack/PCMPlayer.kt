package br.dev.amiranda.echotrack

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.File
import java.io.FileInputStream
import kotlin.math.max

class PCMPlayer (sysAudioManager: AudioManager, private val filePath: String, private val sampleRate: Int = 44100, private val numChannels: Int = 2){
    private var isPlaying = false

    private var playerThread: Thread? = null

    private var audioManager: AudioManager? = null
    private var audioAttributes: AudioAttributes? = null
    private var audioFormat: AudioFormat? = null
    private var audioTrack: AudioTrack? = null

    constructor(sysAudioManager: AudioManager, filePath: String) : this(sysAudioManager,filePath, 44100, 2) {
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        val bufferSize = Math.max(minBufferSize, sampleRate * 2);

        audioManager = sysAudioManager
        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
            .build()

        audioTrack = AudioTrack(audioAttributes, audioFormat, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)
    }

    fun start() {
        if(isPlaying) return

        isPlaying = true
        playerThread = Thread {
            playPCMFile()
            playerThread?.start()
        }
    }

    fun stop() {
        if(!isPlaying) return

        isPlaying = false
        playerThread?.join()
        audioTrack?.stop()
        audioTrack?.release()

        audioTrack = null
    }

    private fun playPCMFile() {
        val channelConfig = if(numChannels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO

        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = max(minBufferSize, sampleRate * 2)

        audioTrack = AudioTrack(audioAttributes, audioFormat, bufferSize, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE)

        val file = File(filePath)
        val fileInputStream = FileInputStream(file)
        val buffer = ByteArray(bufferSize)

        audioTrack?.play()

        while (isPlaying && fileInputStream.read(buffer) != -1) {
            audioTrack?.write(buffer, 0, buffer.size)
        }

        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null

        fileInputStream.close()
    }
}