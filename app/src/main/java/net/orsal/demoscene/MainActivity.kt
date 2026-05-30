package net.orsal.demoscene

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton

/**
 * Launches straight into the demo: a fullscreen, immersive GL surface that keeps
 * the screen awake while the effects play, with a small speaker button to mute
 * the chiptune. The demo itself is tap-driven (tap anywhere else to skip on).
 */
class MainActivity : Activity() {

    private lateinit var surfaceView: DemoSurfaceView
    private lateinit var soundButton: ImageButton
    private val music = ChiptunePlayer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        surfaceView = DemoSurfaceView(this)
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
