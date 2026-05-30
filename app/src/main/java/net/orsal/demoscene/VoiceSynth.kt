package net.orsal.demoscene

import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * A tiny formant speech synth that "sings" the word *poulmouslip* in a robotic,
 * talkbox / vocoder style (à la Daft Punk): a buzzy pitched carrier shaped by
 * resonant formant filters for the vowels, noise bursts for the consonants. The
 * whole utterance plus a trailing gap is rendered once into a loopable buffer.
 */
object VoiceSynth {

    private const val SAMPLE_RATE = 44100

    /** One phoneme: duration, whether it is voiced, its three formants, level. */
    private class Phoneme(
        val durMs: Int,
        val voiced: Boolean,
        val f1: Double,
        val f2: Double,
        val f3: Double,
        val level: Double,
    )

    // p-ou-l-m-ou-s-l-i-p, roughly.
    private val word = listOf(
        Phoneme(70, false, 600.0, 1200.0, 2200.0, 0.0),   // p (mostly silence + burst)
        Phoneme(150, true, 320.0, 800.0, 2300.0, 1.0),    // ou
        Phoneme(85, true, 360.0, 1300.0, 2600.0, 0.9),    // l
        Phoneme(110, true, 280.0, 1000.0, 2200.0, 0.8),   // m (nasal)
        Phoneme(150, true, 320.0, 800.0, 2300.0, 1.0),    // ou
        Phoneme(150, false, 4500.0, 6500.0, 8000.0, 0.7), // s (fricative)
        Phoneme(85, true, 360.0, 1300.0, 2600.0, 0.9),    // l
        Phoneme(200, true, 270.0, 2300.0, 3000.0, 1.0),   // i
        Phoneme(90, false, 600.0, 1200.0, 2200.0, 0.0),   // p
    )

    private const val TRAILING_GAP_MS = 900

    fun build(): ShortArray {
        var totalMs = TRAILING_GAP_MS
        for (p in word) {
            totalMs += p.durMs
        }
        val total = SAMPLE_RATE * totalMs / 1000
        val buf = DoubleArray(total)

        val noise = Random(0xC0FFEE)
        var carrierPhase = 0.0

        // Three formant resonators (biquad band-pass), state carried across phonemes.
        val state = Array(3) { DoubleArray(4) } // x1, x2, y1, y2

        var writePos = 0
        var globalSample = 0
        for (ph in word) {
            val len = SAMPLE_RATE * ph.durMs / 1000
            for (i in 0 until len) {
                val tInPh = i.toDouble() / len
                // Short attack/release to avoid clicks at phoneme edges.
                val env = envelope(tInPh)

                val source: Double
                if (ph.voiced) {
                    // Buzzy sawtooth carrier with a touch of vibrato.
                    val freq = 132.0 * (1.0 + 0.01 * sin(2.0 * PI * 5.0 * globalSample / SAMPLE_RATE))
                    carrierPhase += freq / SAMPLE_RATE
                    source = 2.0 * (carrierPhase - floor(carrierPhase + 0.5))
                } else {
                    // Unvoiced: noise, but the plosives (level 0) stay silent until
                    // a short burst near their end.
                    val burst = if (ph.level <= 0.001) {
                        if (tInPh > 0.7) (noise.nextDouble() * 2.0 - 1.0) * 0.8 else 0.0
                    } else {
                        noise.nextDouble() * 2.0 - 1.0
                    }
                    source = burst
                }

                val shaped =
                    0.6 * biquad(state[0], source, ph.f1) +
                        0.3 * biquad(state[1], source, ph.f2) +
                        0.15 * biquad(state[2], source, ph.f3)

                val level = if (ph.level <= 0.001) 0.9 else ph.level
                if (writePos < total) {
                    buf[writePos] = shaped * env * level
                }
                writePos++
                globalSample++
            }
        }

        // Normalize to a strong, clearly audible level (the talkbox formants are
        // otherwise quiet relative to the music).
        var peak = 0.0
        for (s in buf) {
            val a = if (s < 0.0) -s else s
            if (a > peak) {
                peak = a
            }
        }
        val gain = if (peak > 0.0001) 0.95 / peak else 0.0

        val out = ShortArray(total)
        for (i in buf.indices) {
            out[i] = (clamp(buf[i] * gain) * Short.MAX_VALUE).toInt().toShort()
        }
        // Trailing samples stay zero (the gap before the word repeats).
        return out
    }

    private fun biquad(s: DoubleArray, x: Double, freq: Double): Double {
        val w0 = 2.0 * PI * freq / SAMPLE_RATE
        val q = 9.0
        val alpha = sin(w0) / (2.0 * q)
        val cosW = cos(w0)
        val a0 = 1.0 + alpha
        val b0 = alpha / a0
        val b2 = -alpha / a0
        val a1 = -2.0 * cosW / a0
        val a2 = (1.0 - alpha) / a0
        val y = b0 * x + b2 * s[1] - a1 * s[2] - a2 * s[3]
        s[1] = s[0]
        s[0] = x
        s[3] = s[2]
        s[2] = y
        return y
    }

    private fun envelope(t: Double): Double {
        val edge = 0.12
        if (t < edge) {
            return t / edge
        }
        if (t > 1.0 - edge) {
            return (1.0 - t) / edge
        }
        return 1.0
    }

    private fun clamp(v: Double): Double {
        if (v > 1.0) {
            return 1.0
        }
        if (v < -1.0) {
            return -1.0
        }
        return v
    }
}
