package net.orsal.demoscene

/** Number of frequency bands exposed to audio-reactive effects. */
const val AUDIO_BANDS = 16

/** Live audio analysis the effects can react to. */
interface AudioSource {

    /** Current playback position in beats, wrapped over a bar (0..4). */
    val beat: Float

    /** Per-band magnitudes (0..1), low to high frequency, length [AUDIO_BANDS]. */
    val bands: FloatArray
}
