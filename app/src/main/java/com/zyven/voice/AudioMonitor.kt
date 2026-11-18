package com.zyven.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

/**
 * In-app microphone monitor only.
 * Android does not let a regular non-root app inject this stream into other apps.
 */
class AudioMonitor(
    @Volatile var gain: Float = 1.5f,
    @Volatile var bass: Float = 0.35f
) {
    @Volatile private var active = false
    private var worker: Thread? = null

    fun start() {
        if (active) return
        val sampleRate = 16000
        val minIn = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val minOut = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minIn <= 0 || minOut <= 0) return

        val bufferSize = max(max(minIn, minOut), 4096)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, sampleRate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            return
        }

        val player = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder().setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (player.state != AudioTrack.STATE_INITIALIZED) {
            recorder.release()
            player.release()
            return
        }

        active = true
        worker = thread(name = "ZyvenVoiceAudio") {
            val buffer = ShortArray(1024)
            var bassState = 0.0
            try {
                recorder.startRecording()
                player.play()
                while (active) {
                    val count = recorder.read(buffer, 0, buffer.size)
                    if (count > 0) {
                        val currentGain = gain.coerceIn(0.5f, 4f).toDouble()
                        val currentBass = bass.coerceIn(0f, 1f).toDouble()
                        for (i in 0 until count) {
                            val input = buffer[i].toDouble()
                            bassState += 0.12 * (input - bassState)
                            val lowEmphasis = input + (bassState - input * 0.12) * currentBass
                            val output = lowEmphasis * currentGain
                            buffer[i] = max(-32768.0, min(32767.0, output))
                                .toInt().toShort()
                        }
                        player.write(buffer, 0, count)
                    } else if (count < 0) {
                        break
                    }
                }
            } catch (_: Exception) {
                // Audio can be interrupted by system audio focus or permission changes.
            } finally {
                active = false
                try { recorder.stop() } catch (_: Exception) {}
                try { player.stop() } catch (_: Exception) {}
                recorder.release()
                player.release()
            }
        }
    }

    fun stop() {
        active = false
        worker?.join(700)
        worker = null
    }
}
