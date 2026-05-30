package net.orsal.demoscene

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import net.orsal.demoscene.effects.BumpEffect
import net.orsal.demoscene.effects.ChromeTorusEffect
import net.orsal.demoscene.effects.DotBallEffect
import net.orsal.demoscene.effects.FeedbackEffect
import net.orsal.demoscene.effects.FireEffect
import net.orsal.demoscene.effects.GlenzEffect
import net.orsal.demoscene.effects.InterferenceEffect
import net.orsal.demoscene.effects.KefrenEffect
import net.orsal.demoscene.effects.MandelbrotEffect
import net.orsal.demoscene.effects.MetaballsEffect
import net.orsal.demoscene.effects.ParticlesEffect
import net.orsal.demoscene.effects.PlasmaEffect
import net.orsal.demoscene.effects.PoulmouslipEffect
import net.orsal.demoscene.effects.RotozoomEffect
import net.orsal.demoscene.effects.ScrollerEffect
import net.orsal.demoscene.effects.ShadebobsEffect
import net.orsal.demoscene.effects.StarfieldEffect
import net.orsal.demoscene.effects.TunnelEffect
import net.orsal.demoscene.effects.TwisterEffect
import net.orsal.demoscene.effects.VoxelEffect
import net.orsal.demoscene.effects.WaterEffect
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Owns the demo timeline: it steps through the registered effects, auto-advances
 * when a part's duration elapses, and cross-fades each transition through black.
 * A tap requests an immediate jump to the next part.
 */
class DemoRenderer(
    private val context: Context,
    private val onVoiceActive: (Boolean) -> Unit,
) : GLSurfaceView.Renderer {

    private val effects: List<Effect> = listOf(
        PlasmaEffect(),
        RotozoomEffect(),
        TunnelEffect(),
        InterferenceEffect(),
        TwisterEffect(),
        ChromeTorusEffect(),
        MetaballsEffect(),
        FireEffect(),
        WaterEffect(),
        BumpEffect(),
        GlenzEffect(),
        DotBallEffect(),
        KefrenEffect(),
        ShadebobsEffect(),
        VoxelEffect(),
        StarfieldEffect(),
        ParticlesEffect(),
        FeedbackEffect(),
        MandelbrotEffect(),
        PoulmouslipEffect(),
        ScrollerEffect(),
    )

    private lateinit var effectContext: EffectContext

    private var current = 0
    private var localTime = 0f
    private var lastFrameNanos = 0L

    private val advanceRequested = AtomicBoolean(false)

    private companion object {
        const val FADE = 0.8f
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        effectContext = EffectContext(context, FullscreenQuad())
        effects.forEach { it.onSurfaceCreated(effectContext) }
        current = 0
        localTime = 0f
        lastFrameNanos = 0L
        notifyVoice()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        effects.forEach { it.onResize(width, height) }
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val delta = if (lastFrameNanos == 0L) 0f else (now - lastFrameNanos) / 1_000_000_000f
        lastFrameNanos = now
        localTime += delta

        val effect = effects[current]

        // Tap-only: the demo never auto-advances, it waits for a screen tap.
        if (advanceRequested.getAndSet(false)) {
            advance()
            return
        }

        val fade = (localTime / FADE).coerceIn(0f, 1f)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        effect.render(localTime, fade)
    }

    private fun advance() {
        current = (current + 1) % effects.size
        localTime = 0f
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        notifyVoice()
    }

    private fun notifyVoice() {
        onVoiceActive(effects[current] is PoulmouslipEffect)
    }

    fun requestAdvance() {
        advanceRequested.set(true)
    }
}
