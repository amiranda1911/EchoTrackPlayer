package br.dev.amiranda.echotrack

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat

class MediaUtils {
    fun extractPCMAudio(inputFile: String): List<ByteArray> {
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

            val pcmDataList = mutableListOf<ByteArray>()

            var isExtracting = true
            var isDecoding = true

            while (isDecoding) {
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
                        pcmDataList.add(pcmData)

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

            return pcmDataList
    }
}