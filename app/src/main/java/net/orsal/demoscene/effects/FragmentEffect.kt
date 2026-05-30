package net.orsal.demoscene.effects

import android.content.Context
import android.opengl.GLES20
import net.orsal.demoscene.Effect
import net.orsal.demoscene.EffectContext
import net.orsal.demoscene.FullscreenQuad
import net.orsal.demoscene.GLUtil

/**
 * Base class for any effect that is just a fullscreen fragment shader fed the
 * standard `uTime`, `uResolution` and `uFade` uniforms. Subclasses only provide
 * the fragment source; effects that need extra uniforms or resources (textures,
 * etc.) override the hooks.
 */
abstract class FragmentEffect(
    override val name: String,
    override val duration: Float,
) : Effect {

    private var program = 0
    private var aPos = 0
    private var uTime = 0
    private var uResolution = 0
    private var uFade = 0

    protected var widthPx = 1f
        private set
    protected var heightPx = 1f
        private set

    protected lateinit var androidContext: Context
        private set

    private lateinit var quad: FullscreenQuad

    /** The fragment shader for this effect. */
    protected abstract fun fragmentSource(): String

    /** Fetch extra uniform locations or load resources once the program links. */
    protected open fun onProgramReady(program: Int) {}

    /** Set extra uniforms / bind textures right before the quad is drawn. */
    protected open fun onRender(program: Int, time: Float) {}

    /** Release any extra resources the subclass allocated. */
    protected open fun onDispose() {}

    final override fun onSurfaceCreated(context: EffectContext) {
        androidContext = context.androidContext
        quad = context.quad
        program = GLUtil.buildProgram(VERTEX, fragmentSource())
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        uTime = GLES20.glGetUniformLocation(program, "uTime")
        uResolution = GLES20.glGetUniformLocation(program, "uResolution")
        uFade = GLES20.glGetUniformLocation(program, "uFade")
        onProgramReady(program)
    }

    override fun onResize(width: Int, height: Int) {
        widthPx = width.toFloat()
        heightPx = height.toFloat()
    }

    final override fun render(time: Float, fade: Float) {
        GLES20.glUseProgram(program)
        GLES20.glUniform1f(uTime, time)
        GLES20.glUniform2f(uResolution, widthPx, heightPx)
        GLES20.glUniform1f(uFade, fade)
        onRender(program, time)
        quad.draw(aPos)
    }

    final override fun dispose() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
        onDispose()
    }

    companion object {
        const val VERTEX = """
            attribute vec2 aPos;
            varying vec2 vPos;
            void main() {
                vPos = aPos;
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
        """
    }
}
