package net.orsal.demoscene

/**
 * One demo part. The renderer owns a list of these and steps through them on a
 * timeline. Every callback runs on the GL thread.
 */
interface Effect {

    /** Display name, handy for logging. */
    val name: String

    /**
     * Suggested natural length of this part, in seconds. The demo is currently
     * tap-driven so this is informational, but it documents intended pacing and
     * could drive an optional auto-advance mode later.
     */
    val duration: Float

    /** Compile shaders and allocate GL resources. */
    fun onSurfaceCreated(context: EffectContext)

    /** Viewport size changed. */
    fun onResize(width: Int, height: Int)

    /**
     * Draw a single frame.
     *
     * @param time seconds since this effect became active
     * @param fade 0..1 master brightness used for cross-part fades
     */
    fun render(time: Float, fade: Float)

    /** Release GL resources. */
    fun dispose() {}
}
