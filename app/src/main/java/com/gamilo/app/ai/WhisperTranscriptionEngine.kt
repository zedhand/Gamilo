package com.gamilo.app.ai

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import com.gamilo.app.ai.whisper.WhisperVocab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.min
import kotlin.math.sqrt

/**
 * On-device speech-to-text via a bundled Whisper (tiny.en) model run through TensorFlow Lite.
 * The mel-spectrogram/inference pipeline is ported from vilassn/whisper_android's pure-Java
 * WhisperEngineJava/WhisperUtil (MIT license) — chosen over a native whisper.cpp JNI build
 * because it only needs the stock `org.tensorflow:tensorflow-lite` runtime, no NDK/CMake
 * toolchain to vendor and maintain. Model + vocab/filter files are bundled as app assets
 * (~42MB total, see app/src/main/assets/models/) so transcription never touches the network.
 */
class WhisperTranscriptionEngine(private val context: Context) : VoiceTranscriptionEngine {

    private val interpreter: Interpreter by lazy { loadInterpreter() }
    private val vocab: WhisperVocab by lazy { WhisperVocab.loadFromAssets(context, MODEL_ASSET_VOCAB) }

    override suspend fun transcribeOnce(): TranscriptionResult = withContext(Dispatchers.Default) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return@withContext TranscriptionResult.Failure("Microphone permission is required.")
        }

        val samples = try {
            recordUntilSilence()
        } catch (e: SecurityException) {
            return@withContext TranscriptionResult.Failure("Microphone permission is required.")
        }
        if (samples.isEmpty()) {
            return@withContext TranscriptionResult.Failure("No speech detected.")
        }

        val text = try {
            transcribe(samples)
        } catch (e: Exception) {
            return@withContext TranscriptionResult.Failure("Transcription failed: ${e.message}")
        }

        if (text.isBlank()) TranscriptionResult.Failure("Could not understand the audio — try again.")
        else TranscriptionResult.Success(text.trim())
    }

    /**
     * Captures 16kHz mono PCM until trailing silence is detected after real speech. Stops as
     * soon as the user stops talking (at least [MIN_SPEECH_MS] of detected speech, followed by
     * [TRAILING_SILENCE_MS] of quiet) rather than waiting out a fixed total recording length —
     * a fixed-timer version would make a short utterance sit through several extra seconds of
     * dead air before it would even consider stopping. If nothing is said at all within
     * [NO_SPEECH_TIMEOUT_MS], gives up early instead of running the full 30s hard cap (the
     * model's fixed input window).
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun recordUntilSilence(): FloatArray {
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)
        if (minBufferSize <= 0) return FloatArray(0)

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, SAMPLE_RATE, channelConfig, audioFormat, minBufferSize * 2,
        )
        val maxSamples = SAMPLE_RATE * WhisperVocab.CHUNK_SIZE_SECONDS
        val samples = ShortArray(maxSamples)
        val readBuffer = ShortArray(minBufferSize)
        var written = 0
        var hasSpokenMs = 0L
        var silentStreakMs = 0L

        try {
            audioRecord.startRecording()
            while (written < maxSamples) {
                val read = audioRecord.read(readBuffer, 0, readBuffer.size)
                if (read <= 0) break

                val copyCount = min(read, maxSamples - written)
                System.arraycopy(readBuffer, 0, samples, written, copyCount)
                written += copyCount

                val chunkMs = (read * 1000L) / SAMPLE_RATE
                if (rms(readBuffer, read) >= SILENCE_RMS_THRESHOLD) {
                    hasSpokenMs += chunkMs
                    silentStreakMs = 0L
                } else {
                    silentStreakMs += chunkMs
                }

                val stoppedTalking = hasSpokenMs >= MIN_SPEECH_MS && silentStreakMs >= TRAILING_SILENCE_MS
                val gaveUpWaiting = hasSpokenMs == 0L && silentStreakMs >= NO_SPEECH_TIMEOUT_MS
                if (stoppedTalking || gaveUpWaiting) break
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }

        if (hasSpokenMs == 0L) return FloatArray(0)
        return FloatArray(written) { samples[it] / 32768.0f }
    }

    private fun rms(buffer: ShortArray, length: Int): Double {
        var sumSquares = 0.0
        for (i in 0 until length) {
            val v = buffer[i].toDouble()
            sumSquares += v * v
        }
        return sqrt(sumSquares / length)
    }

    private fun transcribe(samples: FloatArray): String {
        val fixedInputSize = SAMPLE_RATE * WhisperVocab.CHUNK_SIZE_SECONDS
        val input = FloatArray(fixedInputSize)
        System.arraycopy(samples, 0, input, 0, min(samples.size, fixedInputSize))

        val melSpectrogram = vocab.melSpectrogram(input, Runtime.getRuntime().availableProcessors())
        return runInference(melSpectrogram)
    }

    private fun runInference(melSpectrogram: FloatArray): String {
        val outputTensor = interpreter.getOutputTensor(0)

        val inputBuffer = ByteBuffer.allocateDirect(melSpectrogram.size * Float.SIZE_BYTES).order(ByteOrder.nativeOrder())
        melSpectrogram.forEach { inputBuffer.putFloat(it) }
        inputBuffer.rewind()

        val outputBuffer = ByteBuffer.allocateDirect(outputTensor.numBytes()).order(ByteOrder.nativeOrder())

        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        // The exported graph's output tensor is int32 token ids despite what its declared
        // TFLite dtype says — same quirk the reference implementation (vilassn/whisper_android)
        // works around, since both int32 and float32 are 4 bytes wide so the byte-count math is
        // unaffected either way.
        val tokenCount = outputTensor.numBytes() / Int.SIZE_BYTES
        val result = StringBuilder()
        for (i in 0 until tokenCount) {
            val token = outputBuffer.getInt()
            if (token == vocab.tokenEOT) break
            if (token < vocab.tokenEOT) {
                result.append(vocab.wordForToken(token))
            }
        }
        return result.toString()
    }

    private fun loadInterpreter(): Interpreter {
        val assetFd = context.assets.openFd(MODEL_ASSET_PATH)
        val model = FileInputStream(assetFd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, assetFd.startOffset, assetFd.declaredLength,
        )
        val options = Interpreter.Options().apply {
            setNumThreads(Runtime.getRuntime().availableProcessors())
        }
        return Interpreter(model, options)
    }

    companion object {
        private const val MODEL_ASSET_PATH = "models/whisper-tiny.en.tflite"
        private const val MODEL_ASSET_VOCAB = "models/filters_vocab_en.bin"
        private const val SAMPLE_RATE = 16000
        private const val MIN_SPEECH_MS = 300L
        private const val TRAILING_SILENCE_MS = 3000L
        private const val NO_SPEECH_TIMEOUT_MS = 8000L
        private const val SILENCE_RMS_THRESHOLD = 400.0
    }
}
