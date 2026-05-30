package net.orsal.demoscene.effects

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.opengl.GLES20
import android.opengl.GLUtils
import kotlin.math.ceil
import kotlin.math.min

/**
 * Bakes a string into an OpenGL texture (bold monospace, outlined warm fill on a
 * transparent background), shrinking the type size if needed to fit the GL
 * texture-size limit. Shared by the scroller-style effects.
 */
object TextTexture {

    class Result(val textureId: Int, val aspect: Float)

    fun build(text: String): Result {
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

        val bitmap = Bitmap.createBitmap(texWidth, texHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val baseline = -metrics.top + padding / 2f

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

        return Result(ids[0], texWidth.toFloat() / texHeight.toFloat())
    }
}
