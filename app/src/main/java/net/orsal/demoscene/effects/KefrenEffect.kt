package net.orsal.demoscene.effects

/**
 * A spectrum analyser: vertical bars whose heights follow the live frequency
 * bands of the chiptune (low frequencies on the left, highs on the right),
 * coloured like a VU meter and capped with a bright top.
 */
class KefrenEffect : FragmentEffect("Kefren", 13f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;
            uniform float uBands[16];

            void main() {
                vec2 uv = vPos * 0.5 + 0.5; // 0..1

                float n = 16.0;
                float fx = uv.x * n;
                int idx = int(floor(fx));
                float local = fract(fx);

                // Read this column's band height (constant-index access only).
                float h = 0.0;
                for (int i = 0; i < 16; i++) {
                    if (i == idx) { h = uBands[i]; }
                }
                h = 0.04 + 0.92 * h;

                // Bar body with a gap between columns.
                float body = smoothstep(0.06, 0.12, local) * smoothstep(0.06, 0.12, 1.0 - local);
                float filled = step(uv.y, h);

                // VU colouring: green low, yellow mid, red near the top.
                vec3 vu = mix(vec3(0.0, 0.9, 0.35), vec3(1.0, 0.85, 0.0), uv.y);
                vu = mix(vu, vec3(1.0, 0.15, 0.1), smoothstep(0.6, 1.0, uv.y));

                vec3 col = vu * filled * body;
                // Bright cap riding on top of each bar.
                col += vu * body * smoothstep(0.04, 0.0, abs(uv.y - h)) * 1.2;
                // Faint unlit column so the layout stays readable.
                col += vec3(0.04, 0.05, 0.09) * body * (1.0 - filled);

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
