package net.orsal.demoscene

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView

/**
 * Launches straight into the demo: a fullscreen, immersive GL surface that keeps
 * the screen awake while the effects play, with a small speaker button to mute
 * the chiptune. The demo itself is tap-driven (tap anywhere else to skip on).
 */
class MainActivity : Activity() {

    private lateinit var surfaceView: DemoSurfaceView
    private lateinit var soundButton: ImageButton
    private lateinit var effectLabel: TextView
    private val music = ChiptunePlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        effectLabel = buildEffectLabel()
        surfaceView = DemoSurfaceView(this, music) { name ->
            effectLabel.post { effectLabel.text = name }
        }
        soundButton = buildSoundButton()

        val root = FrameLayout(this).apply {
            addView(
                surfaceView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            addView(soundButton, soundButtonLayout())
            addView(effectLabel, effectLabelLayout())
        }

        setContentView(root)
        goImmersive()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            goImmersive()
        }
    }

    override fun onResume() {
        super.onResume()
        surfaceView.onResume()
        music.start()
    }

    override fun onPause() {
        super.onPause()
        surfaceView.onPause()
        music.stop()
    }

    private fun buildSoundButton(): ImageButton {
        val pad = dp(10)
        return ImageButton(this).apply {
            setImageResource(R.drawable.ic_volume_up)
            background = null
            alpha = 0.7f
            setPadding(pad, pad, pad, pad)
            contentDescription = getString(R.string.sound_toggle)
            setOnClickListener {
                val muted = music.toggleMuted()
                setImageResource(
                    if (muted) R.drawable.ic_volume_off else R.drawable.ic_volume_up,
                )
                alpha = if (muted) 0.45f else 0.7f
            }
        }
    }

    private fun buildEffectLabel(): TextView {
        return TextView(this).apply {
            typeface = Typeface.create("sans-serif-thin", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            alpha = 0.75f
            isAllCaps = false
        }
    }

    private fun effectLabelLayout(): FrameLayout.LayoutParams {
        val margin = dp(12)
        return FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            bottomMargin = margin
            leftMargin = margin
            marginStart = margin
        }
    }

    private fun soundButtonLayout(): FrameLayout.LayoutParams {
        val size = dp(48)
        val margin = dp(12)
        return FrameLayout.LayoutParams(size, size).apply {
            gravity = Gravity.TOP or Gravity.END
            topMargin = margin
            marginEnd = margin
            rightMargin = margin
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun goImmersive() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
    }
}
