package net.orsal.demoscene

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

/**
 * GL surface configured for ES 2.0 that forwards taps to the renderer so the
 * viewer can skip to the next part of the demo.
 */
class DemoSurfaceView(
    context: Context,
    onEffectChanged: (name: String, voice: Boolean) -> Unit,
) : GLSurfaceView(context) {

    private val demoRenderer = DemoRenderer(context, onEffectChanged)

    init {
        setEGLContextClientVersion(2)
        setRenderer(demoRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    override fun performClick(): Boolean {
        super.performClick()
        demoRenderer.requestAdvance()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            performClick()
            return true
        }
        return super.onTouchEvent(event)
    }
}
