package net.orsal.demoscene

import android.content.Context

/**
 * Shared resources handed to every effect at creation time, so effects don't
 * each rebuild the same quad or carry their own Android context reference.
 *
 * @param beat returns the current musical position in beats (wrapped over a bar),
 *   so audio-reactive effects can pump in time with the chiptune.
 */
class EffectContext(
    val androidContext: Context,
    val quad: FullscreenQuad,
    val beat: () -> Float,
)
