package net.orsal.demoscene.effects

/**
 * A spectrum analyser: 32 vertical bars whose heights follow the live frequency
 * bands of the chiptune (lows on the left, highs on the right), filled with a
 * vivid rainbow gradient running up their height and topped with a bright cap.
 */
class SpectrumEffect : FragmentEffect("Spectrum", 13f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;
            uniform float uBands[32];

            void main() {
                vec2 uv = vPos * 0.5 + 0.5; // 0..1

                float n = 32.0;
                float fx = uv.x * n;
                int idx = int(floor(fx));
                float local = fract(fx);

                // This column's band height (constant-index access only).
                float h = 0.0;
                for (int i = 0; i < 32; i++) {
                    if (i == idx) { h = uBands[i]; }
                }
                h = 0.03 + 0.95 * h;

                float body = smoothstep(0.08, 0.18, local) * smoothstep(0.08, 0.18, 1.0 - local);
                float filled = step(uv.y, h);

                // Vivid rainbow running up the bar height.
                vec3 rainbow = 0.5 + 0.5 * cos(
                    6.2831853 * (uv.y + vec3(0.0, 0.33, 0.67)) - 1.5
                );

                vec3 col = rainbow * filled * body;
                col += vec3(1.0) * body * smoothstep(0.03, 0.0, abs(uv.y - h)) * 0.9; // cap
                col += rainbow * body * (1.0 - filled) * 0.06; // faint unlit column

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
