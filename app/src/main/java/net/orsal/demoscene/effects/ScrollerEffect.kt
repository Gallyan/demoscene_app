package net.orsal.demoscene.effects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLUtils
import net.orsal.demoscene.R
import kotlin.math.ceil
import kotlin.math.min

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
        textureId = buildTextTexture(androidContext.getString(R.string.scroller_text))
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

    /** Render the scroller message into a texture, fitting GL size limits. */
    private fun buildTextTexture(text: String): Int {
        val maxTex = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTex, 0)
        val targetMaxWidth = min(maxTex[0], 8192)
        val padding = 16

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 160f
            style = Paint.Style.FILL
        }

        var measured = paint.measureText(text)
        if (measured + padding > targetMaxWidth) {
            paint.textSize *= (targetMaxWidth - padding) / measured
            measured = paint.measureText(text)
        }

        val metrics = paint.fontMetrics
        val texWidth = ceil(measured).toInt() + padding
        val texHeight = ceil(metrics.bottom - metrics.top).toInt() + padding
        texAspect = texWidth.toFloat() / texHeight.toFloat()

        val bitmap = Bitmap.createBitmap(texWidth, texHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val baseline = -metrics.top + padding / 2f

        // Outline for that crisp old-school look, then a warm fill on top.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = paint.textSize * 0.06f
        paint.color = Color.rgb(20, 10, 60)
        canvas.drawText(text, padding / 2f, baseline, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(255, 230, 120)
        canvas.drawText(text, padding / 2f, baseline, paint)

        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE,
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()

        return ids[0]
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
