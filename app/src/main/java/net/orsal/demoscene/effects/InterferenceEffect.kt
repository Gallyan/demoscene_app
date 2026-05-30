package net.orsal.demoscene.effects

/**
 * Moiré interference: two sets of concentric rings from two drifting centres,
 * multiplied together so the classic shimmering interference pattern appears.
 */
class InterferenceEffect : FragmentEffect("Interference", 12f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                vec2 c1 = vec2(sin(uTime * 0.5), cos(uTime * 0.4)) * 0.4;
                vec2 c2 = vec2(sin(uTime * 0.6 + 2.0), cos(uTime * 0.45 + 1.0)) * 0.4;

                float a = sin(length(uv - c1) * 42.0);
                float b = sin(length(uv - c2) * 42.0);
                float m = a * b;

                vec3 col = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + m * 3.0 + uTime);
                col *= 0.4 + 0.6 * m * m;

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
