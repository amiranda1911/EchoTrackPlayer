package br.dev.amiranda.echotrack

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import kotlin.math.max

class PCMPlayer( private val sampleRate: Int = 44100, private val numChannels: Int = 2) {
    private var audioFile: String = ""
    private var isPlaying = false

    private var playerThread: Thread? = null
    private var decoderThread: Thread? = null

    private var audioAttributes: AudioAttributes? = null
    private var audioFormat: AudioFormat? = null
    private var audioTrack: AudioTrack? = null
    companion object{
        private var bufferSize = 0
        private var minBufferSize = 0
    }

    constructor() : this(44100, 2) {
        // Calculando o tamanho mínimo do buffer
        minBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
        bufferSize = max(minBufferSize, sampleRate * 2)

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
    }

    fun setAudioFile(audioFile: String) {
        this.audioFile = audioFile
    }

    fun start() {
        if (isPlaying) return

        isPlaying = true

        playerThread = Thread {
            playFile()
        }
        playerThread?.start()
    }

    fun stop() {
        if (!isPlaying) return

        isPlaying = false
        playerThread?.interrupt()

        playerThread?.join()

        audioTrack?.apply {
            stop()
            flush()
            release()
        }
        audioTrack = null

        playerThread = null
    }

    private fun playFile() {
        // Init AudioTrack
        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack?.play()

        extractPCMAudio(audioFile)

        // Após a reprodução, paramos e liberamos o AudioTrack
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }


    fun extractPCMAudio(inputFile: String) {
        val extractor = MediaExtractor()
        extractor.setDataSource(inputFile)

        var format: MediaFormat? = null
        var trackIndex = -1

        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith("audio/") == true) {
                trackIndex = i
                format = trackFormat
                break
            }
        }

        if (trackIndex < 0 || format == null) {
            extractor.release()
            throw IllegalArgumentException("Nenhuma trilha de áudio encontrada")
        }

        extractor.selectTrack(trackIndex)
        val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
        codec.configure(format, null, null, 0)
        codec.start()

        val inputBuffers = codec.inputBuffers
        val outputBuffers = codec.outputBuffers
        val bufferInfo = MediaCodec.BufferInfo()



        var isExtracting = true
        var isDecoding = true

        while (isDecoding) {
            if (Thread.currentThread().isInterrupted) {
                println("Thread foi interrompida. Parando a execução...")
                break // Para o bloco when ou o loop
            }

            if (isExtracting) {
                val inputIndex = codec.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = inputBuffers[inputIndex]
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)

                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isExtracting = false
                    } else {
                        codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            when {
                outputIndex >= 0 -> {
                    val outputBuffer = outputBuffers[outputIndex]
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                    // Adicionando o buffer PCM ao resultado
                    val pcmData = ByteArray(bufferInfo.size)
                    outputBuffer.get(pcmData)

                    audioTrack?.write(pcmData, 0, bufferInfo.size)
                    codec.releaseOutputBuffer(outputIndex, false)

                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        isDecoding = false
                    }
                }
                outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                    codec.outputBuffers
                }
                outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val newFormat = codec.outputFormat
                    val sampleRate = newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    val channelCount = newFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    println("Formato PCM: $sampleRate Hz, $channelCount canais")
                }
            }
        }

        codec.stop()
        codec.release()
        extractor.release()
    }
}