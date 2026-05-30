package net.orsal.demoscene.effects

import android.opengl.GLES20
import net.orsal.demoscene.Effect
import net.orsal.demoscene.EffectContext
import net.orsal.demoscene.FullscreenQuad
import net.orsal.demoscene.GLUtil

/**
 * The classic sine-based plasma: a handful of overlapping sine fields summed
 * together and pushed through a cycling palette.
 */
class PlasmaEffect : Effect {

    override val name = "Plasma"
    override val duration = 14f

    private var program = 0
    private var aPos = 0
    private var uTime = 0
    private var uResolution = 0
    private var uFade = 0

    private var width = 1f
    private var height = 1f

    private lateinit var quad: FullscreenQuad

    override fun onSurfaceCreated(context: EffectContext) {
        quad = context.quad
        program = GLUtil.buildProgram(VERTEX, FRAGMENT)
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        uTime = GLES20.glGetUniformLocation(program, "uTime")
        uResolution = GLES20.glGetUniformLocation(program, "uResolution")
        uFade = GLES20.glGetUniformLocation(program, "uFade")
    }

    override fun onResize(width: Int, height: Int) {
        this.width = width.toFloat()
        this.height = height.toFloat()
    }

    override fun render(time: Float, fade: Float) {
        GLES20.glUseProgram(program)
        GLES20.glUniform1f(uTime, time)
        GLES20.glUniform2f(uResolution, width, height)
        GLES20.glUniform1f(uFade, fade)
        quad.draw(aPos)
    }

    override fun dispose() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }

    companion object {
        private const val VERTEX = """
            attribute vec2 aPos;
            varying vec2 vPos;
            void main() {
                vPos = aPos;
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
        """

        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;

            void main() {
                vec2 p = vPos;
                p.x *= uResolution.x / uResolution.y;

                float t = uTime;
                float v = 0.0;
                v += sin(p.x * 5.0 + t);
                v += sin((p.y * 5.0 + t) * 0.5);
                v += sin((p.x * 4.0 + p.y * 4.0 + t) * 0.5);

                float cx = p.x + 0.6 * sin(t * 0.5);
                float cy = p.y + 0.6 * cos(t * 0.33);
                v += sin(sqrt(60.0 * (cx * cx + cy * cy) + 1.0) + t);

                v *= 0.5;
                float pi = 3.14159265;
                vec3 col = vec3(
                    sin(v * pi),
                    sin(v * pi + 2.094),
                    sin(v * pi + 4.188)
                );
                col = col * 0.5 + 0.5;

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
