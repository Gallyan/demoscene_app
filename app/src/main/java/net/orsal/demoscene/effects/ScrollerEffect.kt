package net.orsal.demoscene.effects

import android.opengl.GLES20
import net.orsal.demoscene.R

/**
 * A sine-wave scroller over glowing copper bars, the way an old logon screen or
 * cracktro would greet you. The message is baked once into a texture, then the
 * shader scrolls and waves it across the lower part of the screen.
 */
class ScrollerEffect : FragmentEffect("Scroller", 18f) {

    private var uTex = 0
    private var uTexAspect = 0

    private var textureId = 0
    private var texAspect = 8f

    override fun fragmentSource() = FRAGMENT

    override fun onProgramReady(program: Int) {
        uTex = GLES20.glGetUniformLocation(program, "uTex")
        uTexAspect = GLES20.glGetUniformLocation(program, "uTexAspect")
        val text = TextTexture.build(androidContext.getString(R.string.scroller_text))
        textureId = text.textureId
        texAspect = text.aspect
    }

    override fun onRender(program: Int, time: Float) {
        GLES20.glUniform1f(uTexAspect, texAspect)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTex, 0)
    }

    override fun onDispose() {
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
    }

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;
            uniform sampler2D uTex;
            uniform float uTexAspect;

            void main() {
                float aspect = uResolution.x / uResolution.y;
                vec2 uv = vec2(vPos.x * aspect, vPos.y);

                // --- Copper bars background ---
                float bar = sin(vPos.y * 7.0 + uTime * 1.3);
                vec3 copper = vec3(0.55, 0.30, 0.08)
                    + vec3(0.45, 0.32, 0.12) * bar;
                copper *= 0.45 + 0.55 * bar * bar;
                float bar2 = sin(vPos.y * 3.0 - uTime * 0.7);
                copper += vec3(0.10, 0.05, 0.20) * (0.5 + 0.5 * bar2);
                vec3 col = max(copper, 0.0);

                // --- Sine scroller ---
                float bandHalf = 0.26;
                float amp = 0.11;
                float waveCenter = -0.10 + amp * sin(uv.x * 1.7 + uTime * 1.6);
                float v = (uv.y - waveCenter) / bandHalf;

                if (abs(v) < 1.0) {
                    float speed = 0.55;
                    float textWidthNDC = uTexAspect * (2.0 * bandHalf);
                    float u = fract((uv.x + uTime * speed) / textWidthNDC);
                    float tv = 0.5 - 0.5 * v;
                    vec4 texel = texture2D(uTex, vec2(u, tv));
                    // texel is premultiplied; composite over the copper bars.
                    col = texel.rgb + col * (1.0 - texel.a);
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
