package br.dev.amiranda.echotrack

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.FileOutputStream

class MediaUtils {
    public fun extractPCMAudio(input: String, output: String) {
        val extractor = MediaExtractor()
        extractor.setDataSource(input)

        // Found Audio Track
        var audioTrackIndex = -1
        var format: MediaFormat? = null
        for(i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: ""
            if(mime.startsWith("audio/")){
                audioTrackIndex = i
                format = trackFormat
                extractor.selectTrack(i)
                break
            }
        }

        if(audioTrackIndex == -1 || format == null)
            throw RuntimeException("Äudio File not found!")

        // Create audio decoder
        val mimeType = format.getString(MediaFormat.KEY_MIME)!!
        val decoder = MediaCodec.createDecoderByType(mimeType)
        decoder.configure(format, null, null, 0)
        decoder.start()

        val bufferInfo = MediaCodec.BufferInfo()
        val pcmFile = File(output)
        val outputStream = FileOutputStream(pcmFile)
        var isEOS = false

        while(!isEOS){
            // Send data from extractor to decoder
            val  inputIndex = decoder.dequeueInputBuffer(10000)
            if(inputIndex >= 0) {
                val inputBuffer = decoder.getInputBuffer(inputIndex)!!
                val sampleSize = extractor.readSampleData(inputBuffer, 0)
                if(sampleSize < 0) {
                    decoder.queueInputBuffer(inputIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    isEOS = true
                } else {
                    decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }
        }

        // Get PCM from decoder
        val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
        if(outputIndex >= 0) {
            val outputBuffer = decoder.getOutputBuffer(outputIndex)!!
            val chunkPCM = ByteArray(bufferInfo.size)
            outputBuffer.get(chunkPCM)
            outputBuffer.clear()

            // Write pcm in file
            outputStream.write(chunkPCM)
            decoder.releaseOutputBuffer(outputIndex, false)
        }

        // Verify and finalize
        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
            isEOS = true
        }

        // Clear resources
        outputStream.close()
        decoder.stop()
        decoder.release()
        extractor.release()
    }
}