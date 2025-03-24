package br.dev.amiranda.echotrack

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.max

class PCMPlayer(private val playingStream: List<ByteArray>, private val sampleRate: Int = 44100, private val numChannels: Int = 2) {

    private var isPlaying = false
    private var playerThread: Thread? = null
    private var audioAttributes: AudioAttributes? = null
    private var audioFormat: AudioFormat? = null
    private var audioTrack: AudioTrack? = null

    constructor(playingStream: List<ByteArray>) : this(playingStream, 44100, 2) {
        // Calculando o tamanho mínimo do buffer
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = max(minBufferSize, sampleRate * 2)

        // Configuração do áudio
        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(if (numChannels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO)
            .build()

        // Inicializa o AudioTrack
        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    fun start() {
        if (isPlaying) return

        isPlaying = true
        playerThread = Thread {
            playPCMFile()
        }
        playerThread?.start()
    }

    fun stop() {
        if (!isPlaying) return

        isPlaying = false
        playerThread?.join() // Aguarda a thread terminar
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    private fun playPCMFile() {
        // Verifica o tipo de configuração de canal
        val channelConfig = if (numChannels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO

        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT)
        val bufferSize = max(minBufferSize, sampleRate * 2)

        // Recria o AudioTrack com a configuração correta
        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack?.play()

        playingStream.forEach { buffer ->
            // Escreve o buffer no AudioTrack
            var bytesWritten = 0
            while (bytesWritten < buffer.size) {
                // O método write retorna a quantidade de bytes escritos
                val written = audioTrack?.write(buffer, bytesWritten, buffer.size - bytesWritten) ?: 0
                if (written > 0) {
                    bytesWritten += written
                }
            }
        }

        // Após a reprodução, paramos e liberamos o AudioTrack
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }
}