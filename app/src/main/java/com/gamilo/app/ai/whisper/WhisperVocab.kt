package com.gamilo.app.ai.whisper

import android.content.Context
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin

/**
 * Mel-spectrogram computation and token decoding for the bundled whisper-tiny.en TFLite model.
 * Ported from vilassn/whisper_android's WhisperUtil.java (MIT license,
 * github.com/vilassn/whisper_android) — the bundled filters_vocab_en.bin bakes in the exact mel
 * filterbank and token table that model was exported with, so this has to match that reference
 * implementation's math precisely rather than reuse a generic FFT/mel library.
 */
class WhisperVocab private constructor(
    private val tokenToWord: Map<Int, String>,
    private val melFilters: FloatArray,
    private val nMel: Int,
    private val nFftBins: Int,
    val tokenEOT: Int,
) {
    fun wordForToken(token: Int): String = tokenToWord[token] ?: ""

    /** samples must be exactly SAMPLE_RATE * CHUNK_SIZE_SECONDS long (the model's fixed input window). */
    fun melSpectrogram(samples: FloatArray, threadCount: Int): FloatArray {
        val fftSize = N_FFT
        val fftStep = HOP_LENGTH
        val melLen = samples.size / fftStep
        val mel = FloatArray(nMel * melLen)

        val hann = FloatArray(fftSize) { i -> (0.5 * (1.0 - cos(2.0 * Math.PI * i / fftSize))).toFloat() }

        val threads = (0 until threadCount.coerceAtLeast(1)).map { threadIndex ->
            Thread {
                val fftIn = FloatArray(fftSize)
                val fftOut = FloatArray(fftSize * 2)
                var i = threadIndex
                while (i < melLen) {
                    val offset = i * fftStep
                    for (j in 0 until fftSize) {
                        fftIn[j] = if (offset + j < samples.size) hann[j] * samples[offset + j] else 0f
                    }

                    fft(fftIn, fftOut)
                    for (j in 0 until fftSize) {
                        fftOut[j] = fftOut[2 * j] * fftOut[2 * j] + fftOut[2 * j + 1] * fftOut[2 * j + 1]
                    }
                    for (j in 1 until fftSize / 2) {
                        fftOut[j] += fftOut[fftSize - j]
                    }

                    for (j in 0 until nMel) {
                        var sum = 0.0
                        for (k in 0 until nFftBins) {
                            sum += (fftOut[k] * melFilters[j * nFftBins + k]).toDouble()
                        }
                        if (sum < 1e-10) sum = 1e-10
                        mel[j * melLen + i] = log10(sum).toFloat()
                    }
                    i += threadCount
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        var maxVal = -1e20
        for (v in mel) if (v > maxVal) maxVal = v.toDouble()
        val floor = maxVal - 8.0
        for (i in mel.indices) {
            val clamped = if (mel[i] < floor) floor else mel[i].toDouble()
            mel[i] = ((clamped + 4.0) / 4.0).toFloat()
        }
        return mel
    }

    private fun fft(input: FloatArray, output: FloatArray) {
        val n = input.size
        if (n == 1) {
            output[0] = input[0]
            output[1] = 0f
            return
        }
        if (n % 2 == 1) {
            dft(input, output)
            return
        }

        val even = FloatArray(n / 2)
        val odd = FloatArray(n / 2)
        for (i in 0 until n) {
            if (i % 2 == 0) even[i / 2] = input[i] else odd[i / 2] = input[i]
        }

        val evenFft = FloatArray(n)
        val oddFft = FloatArray(n)
        fft(even, evenFft)
        fft(odd, oddFft)

        for (k in 0 until n / 2) {
            val theta = (2.0 * Math.PI * k / n).toFloat()
            val re = cos(theta.toDouble()).toFloat()
            val im = -sin(theta.toDouble()).toFloat()
            val reOdd = oddFft[2 * k]
            val imOdd = oddFft[2 * k + 1]
            output[2 * k] = evenFft[2 * k] + re * reOdd - im * imOdd
            output[2 * k + 1] = evenFft[2 * k + 1] + re * imOdd + im * reOdd
            output[2 * (k + n / 2)] = evenFft[2 * k] - re * reOdd + im * imOdd
            output[2 * (k + n / 2) + 1] = evenFft[2 * k + 1] - re * imOdd - im * reOdd
        }
    }

    private fun dft(input: FloatArray, output: FloatArray) {
        val n = input.size
        for (k in 0 until n) {
            var re = 0f
            var im = 0f
            for (i in 0 until n) {
                val angle = (2.0 * Math.PI * k * i / n).toFloat()
                re += input[i] * cos(angle.toDouble()).toFloat()
                im -= input[i] * sin(angle.toDouble()).toFloat()
            }
            output[k * 2] = re
            output[k * 2 + 1] = im
        }
    }

    companion object {
        const val CHUNK_SIZE_SECONDS = 30
        private const val N_FFT = 400
        private const val HOP_LENGTH = 160
        private const val VOCAB_MAGIC = 0x5553454e

        // Fixed special-token ids for whisper's English-only tokenizer (unchanged by this
        // model's export step, same values whisper.cpp itself uses).
        private const val TOKEN_EOT = 50256
        private const val TOKEN_SOT = 50257
        private const val TOKEN_PREV = 50360
        private const val TOKEN_NOT = 50362
        private const val TOKEN_BEG = 50363
        private const val N_VOCAB_ENGLISH = 51864

        fun loadFromAssets(context: Context, assetPath: String): WhisperVocab =
            context.assets.open(assetPath).use { parse(it) }

        private fun parse(stream: InputStream): WhisperVocab {
            val buffer = ByteBuffer.wrap(stream.readBytes()).order(ByteOrder.LITTLE_ENDIAN)

            val magic = buffer.int
            require(magic == VOCAB_MAGIC) { "Invalid vocab/filter file (bad magic: $magic)" }

            val nMel = buffer.int
            val nFftBins = buffer.int
            val filterData = FloatArray(nMel * nFftBins)
            for (i in filterData.indices) filterData[i] = buffer.float

            val nVocab = buffer.int
            val tokenToWord = HashMap<Int, String>(N_VOCAB_ENGLISH)
            for (i in 0 until nVocab) {
                val len = buffer.int
                val wordBytes = ByteArray(len)
                buffer.get(wordBytes)
                tokenToWord[i] = String(wordBytes, Charsets.UTF_8)
            }

            for (i in nVocab until N_VOCAB_ENGLISH) {
                tokenToWord[i] = when {
                    i > TOKEN_BEG -> "[_TT_${i - TOKEN_BEG}]"
                    i == TOKEN_EOT -> "[_EOT_]"
                    i == TOKEN_SOT -> "[_SOT_]"
                    i == TOKEN_PREV -> "[_PREV_]"
                    i == TOKEN_NOT -> "[_NOT_]"
                    i == TOKEN_BEG -> "[_BEG_]"
                    else -> "[_extra_token_$i]"
                }
            }

            return WhisperVocab(tokenToWord, filterData, nMel, nFftBins, TOKEN_EOT)
        }
    }
}
