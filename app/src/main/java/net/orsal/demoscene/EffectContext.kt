package net.orsal.demoscene

import android.content.Context

/**
 * Shared resources handed to every effect at creation time, so effects don't
 * each rebuild the same quad or carry their own Android context reference.
 */
class EffectContext(
    val androidContext: Context,
    val quad: FullscreenQuad,
)
