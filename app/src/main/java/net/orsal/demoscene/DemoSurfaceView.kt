package net.orsal.demoscene

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

/**
 * GL surface configured for ES 2.0 that forwards taps to the renderer: tapping
 * the left half goes to the previous part, the right half to the next.
 */
class DemoSurfaceView(
    context: Context,
    audio: AudioSource,
    onEffectChanged: (name: String, voice: Boolean) -> Unit,
) : GLSurfaceView(context) {

    private val demoRenderer = DemoRenderer(context, audio, onEffectChanged)

    init {
        setEGLContextClientVersion(2)
        setRenderer(demoRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            performClick()
            demoRenderer.requestStep(if (event.x < width / 2f) -1 else 1)
            return true
        }
        return super.onTouchEvent(event)
    }
}
