package com.evenfall.app.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer

@OptIn(UnstableApi::class)
class MonoDownmixAudioProcessor : BaseAudioProcessor() {
    var isEnabled: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                flush()
            }
        }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (!isEnabled || inputAudioFormat.channelCount <= 1 || inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return AudioProcessor.AudioFormat(inputAudioFormat.sampleRate, 1, C.ENCODING_PCM_16BIT)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val inputChannelCount = inputAudioFormat.channelCount
        val bytesPerFrame = inputChannelCount * 2
        val frameCount = remaining / bytesPerFrame
        val outputBuffer = replaceOutputBuffer(frameCount * 2)

        for (i in 0 until frameCount) {
            var sum = 0
            for (ch in 0 until inputChannelCount) {
                sum += inputBuffer.short
            }
            val avg = (sum / inputChannelCount).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            outputBuffer.putShort(avg)
        }

        outputBuffer.flip()
    }
}
