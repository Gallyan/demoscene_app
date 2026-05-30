package net.orsal.demoscene

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.util.Random
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.tanh

/**
 * A self-contained chiptune: no audio asset, the looping track is synthesized in
 * code (arpeggio + pulsing bass + lead melody + simple drums) and streamed
 * through an [AudioTrack] on its own thread. Authentic cracktro fuel.
 */
class ChiptunePlayer {

    @Volatile
    var muted = false
        private set

    /** When true, the spoken "poulmouslip" voice is mixed over the music. */
    @Volatile
    var voiceActive = false

    /** Current playback position in beats, wrapped over a bar (0..4). */
    @Volatile
    var beat = 0f
        private set

    @Volatile
    private var running = false
    private var thread: Thread? = null
    private var track: AudioTrack? = null

    fun start() {
        if (running) {
            return
        }
        running = true
        thread = Thread({ runAudioLoop() }, "ChiptunePlayer").apply { start() }
    }

    fun stop() {
        running = false
        thread?.join()
        thread = null
        track?.run {
            stop()
            release()
        }
        track = null
    }

    /** Returns the new muted state. */
    fun toggleMuted(): Boolean {
        muted = !muted
        return muted
    }

    private fun runAudioLoop() {
        val song = buildSong()
        val voice = VoiceSynth.build()

        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(max(minBuffer, CHUNK_FRAMES * 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track = audioTrack
        audioTrack.play()

        val samplesPerBeat = SAMPLE_RATE * 60.0 / BPM
        val barFrames = (samplesPerBeat * 4.0).toLong()

        val chunk = ShortArray(CHUNK_FRAMES)
        var pos = 0
        var voicePos = 0
        var wasVoice = false
        var framesPlayed = 0L
        while (running) {
            for (i in 0 until CHUNK_FRAMES) {
                var value = song[pos].toInt()
                pos = (pos + 1) % song.size

                if (voiceActive) {
                    // Restart the utterance whenever the voice is (re)enabled.
                    if (!wasVoice) {
                        voicePos = 0
                        wasVoice = true
                    }
                    // Mix the voice on top without ducking the music; the voice
                    // is tuned to the key so it sits in rather than fights it.
                    value += voice[voicePos] * 6 / 10
                    voicePos = (voicePos + 1) % voice.size
                } else {
                    wasVoice = false
                }

                if (muted) {
                    value = 0
                }
                chunk[i] = value.coerceIn(-32768, 32767).toShort()
            }
            audioTrack.write(chunk, 0, CHUNK_FRAMES)
            framesPlayed += CHUNK_FRAMES
            beat = ((framesPlayed % barFrames) / samplesPerBeat).toFloat()
        }
    }

    private fun buildSong(): ShortArray {
        val samplesPerStep = (SAMPLE_RATE * 60.0 / (BPM * 4.0)).toInt()
        val totalSteps = STEPS_PER_BAR * BARS
        val mix = DoubleArray(samplesPerStep * totalSteps)

        // Progression: Am - F - C - G.
        val arpChords = arrayOf(
            intArrayOf(57, 60, 64, 69),
            intArrayOf(53, 57, 60, 65),
            intArrayOf(60, 64, 67, 72),
            intArrayOf(55, 59, 62, 67),
        )
        val bassRoots = intArrayOf(45, 41, 48, 43)
        val lead = intArrayOf(
            76, -1, -1, -1, 74, -1, -1, -1, 72, -1, -1, -1, 76, -1, -1, -1,
            77, -1, -1, -1, 76, -1, -1, -1, 72, -1, -1, -1, 69, -1, -1, -1,
            72, -1, -1, -1, 76, -1, -1, -1, 79, -1, -1, -1, 76, -1, -1, -1,
            74, -1, -1, -1, 71, -1, -1, -1, 67, -1, -1, -1, 62, -1, 74, -1,
        )

        for (step in 0 until totalSteps) {
            val bar = (step / STEPS_PER_BAR) % BARS
            val start = step * samplesPerStep

            // Arpeggio: one chord note per step, an octave up for sparkle.
            val chord = arpChords[bar]
            renderTone(
                mix, start, samplesPerStep,
                midiToFreq(chord[step % chord.size] + 12),
                amp = 0.16, decay = 16.0, sustain = 0.0,
            )

            // Drums.
            val inBar = step % STEPS_PER_BAR
            if (inBar == 0 || inBar == 8) {
                renderKick(mix, start)
            }
            if (inBar == 4 || inBar == 12) {
                renderSnare(mix, start)
            }
            if (step % 2 == 0) {
                renderHat(mix, start)
            }
        }

        // Bass: root of the bar, retriggered every beat for a pulse.
        for (beat in 0 until totalSteps / 4) {
            val step = beat * 4
            val bar = (step / STEPS_PER_BAR) % BARS
            renderTone(
                mix, step * samplesPerStep, samplesPerStep * 4,
                midiToFreq(bassRoots[bar]),
                amp = 0.24, decay = 2.5, sustain = 0.45,
            )
        }

        // Lead melody: hold each note until the next one.
        for (step in 0 until totalSteps) {
            val note = lead[step]
            if (note < 0) {
                continue
            }
            var dur = 1
            while (step + dur < totalSteps && lead[step + dur] < 0) {
                dur++
            }
            renderTone(
                mix, step * samplesPerStep, samplesPerStep * dur,
                midiToFreq(note),
                amp = 0.17, decay = 1.5, sustain = 0.55, vibrato = true,
            )
        }

        val out = ShortArray(mix.size)
        for (i in mix.indices) {
            // Soft clip to keep the mix warm rather than harsh.
            out[i] = (tanh(mix[i] * 1.2) * 0.9 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun renderTone(
        buf: DoubleArray,
        start: Int,
        len: Int,
        freq: Double,
        amp: Double,
        decay: Double,
        sustain: Double,
        duty: Double = 0.5,
        vibrato: Boolean = false,
    ) {
        val durSeconds = len.toDouble() / SAMPLE_RATE
        var phase = 0.0
        for (i in 0 until len) {
            val idx = start + i
            if (idx >= buf.size) {
                break
            }
            val t = i.toDouble() / SAMPLE_RATE
            val f = if (vibrato) freq * (1.0 + 0.012 * sin(TWO_PI * 6.0 * t)) else freq
            phase += f / SAMPLE_RATE
            val wave = if (phase - floor(phase) < duty) 1.0 else -1.0
            buf[idx] += wave * amp * envelope(t, durSeconds, decay, sustain)
        }
    }

    private fun renderKick(buf: DoubleArray, start: Int) {
        val len = (0.18 * SAMPLE_RATE).toInt()
        var phase = 0.0
        for (i in 0 until len) {
            val idx = start + i
            if (idx >= buf.size) {
                break
            }
            val t = i.toDouble() / SAMPLE_RATE
            val f = 45.0 + 110.0 * exp(-t * 30.0)
            phase += f / SAMPLE_RATE
            buf[idx] += sin(TWO_PI * phase) * 0.6 * exp(-t * 14.0)
        }
    }

    private fun renderSnare(buf: DoubleArray, start: Int) {
        val len = (0.14 * SAMPLE_RATE).toInt()
        for (i in 0 until len) {
            val idx = start + i
            if (idx >= buf.size) {
                break
            }
            val t = i.toDouble() / SAMPLE_RATE
            val noise = noise.nextDouble() * 2.0 - 1.0
            val tone = sin(TWO_PI * 190.0 * t)
            buf[idx] += (noise * 0.32 + tone * 0.12) * exp(-t * 22.0)
        }
    }

    private fun renderHat(buf: DoubleArray, start: Int) {
        val len = (0.04 * SAMPLE_RATE).toInt()
        for (i in 0 until len) {
            val idx = start + i
            if (idx >= buf.size) {
                break
            }
            val t = i.toDouble() / SAMPLE_RATE
            buf[idx] += (noise.nextDouble() * 2.0 - 1.0) * 0.13 * exp(-t * 80.0)
        }
    }

    private fun envelope(t: Double, dur: Double, decay: Double, sustain: Double): Double {
        val attack = 0.004
        val release = 0.012
        val a = if (t < attack) t / attack else 1.0
        val r = if (t > dur - release) max(0.0, (dur - t) / release) else 1.0
        val d = sustain + (1.0 - sustain) * exp(-t * decay)
        return a * r * d
    }

    private fun midiToFreq(note: Int): Double = 440.0 * Math.pow(2.0, (note - 69) / 12.0)

    private val noise = Random(0x5EED)

    private companion object {
        const val SAMPLE_RATE = 44100
        const val BPM = 125.0
        const val STEPS_PER_BAR = 16
        const val BARS = 4
        const val CHUNK_FRAMES = 2048
        const val TWO_PI = 2.0 * Math.PI
    }
}
