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
import net.orsal.demoscene.effects.SpectrumEffect
import net.orsal.demoscene.effects.StarfieldEffect
import net.orsal.demoscene.effects.TunnelEffect
import net.orsal.demoscene.effects.TwisterEffect
import net.orsal.demoscene.effects.VoxelEffect
import net.orsal.demoscene.effects.WaterEffect
import java.util.concurrent.atomic.AtomicInteger
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Owns the demo timeline: it steps through the registered effects, auto-advances
 * when a part's duration elapses, and cross-fades each transition through black.
 * A tap requests an immediate jump to the next part.
 */
class DemoRenderer(
    private val context: Context,
    private val audio: AudioSource,
    private val onEffectChanged: (name: String, voice: Boolean) -> Unit,
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
        SpectrumEffect(),
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

    private val pendingStep = AtomicInteger(0)

    private companion object {
        const val FADE = 0.8f
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        effectContext = EffectContext(context, FullscreenQuad(), audio)
        effects.forEach { it.onSurfaceCreated(effectContext) }
        current = 0
        localTime = 0f
        lastFrameNanos = 0L
        notifyEffect()
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

        // Tap-only: tap left for the previous part, right for the next.
        val step = pendingStep.getAndSet(0)
        if (step != 0) {
            advance(step)
            return
        }

        val fade = (localTime / FADE).coerceIn(0f, 1f)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        effect.render(localTime, fade)
    }

    private fun advance(step: Int) {
        val size = effects.size
        current = ((current + step) % size + size) % size
        localTime = 0f
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        notifyEffect()
    }

    private fun notifyEffect() {
        val effect = effects[current]
        onEffectChanged(effect.name, effect is PoulmouslipEffect)
    }

    /** dir = +1 for the next part, -1 for the previous one. */
    fun requestStep(dir: Int) {
        pendingStep.addAndGet(dir)
    }
}
