package net.orsal.demoscene.effects

/**
 * A twister: vertical ribbons whose rotation angle increases with height, so a
 * flat band projects to a column that appears to twist and fold on itself.
 */
class TwisterEffect : FragmentEffect("Twister", 13f) {

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

                vec3 col = vec3(0.02, 0.0, 0.05);
                for (int s = 0; s < 3; s++) {
                    float fs = float(s);
                    float a = uv.y * 3.0 + uTime * 2.0 + fs * 2.094;
                    float w = 0.34;
                    float edge = w * cos(a);
                    float face = sin(a);

                    float band = smoothstep(0.025, 0.0, abs(uv.x) - abs(edge));
                    float shade = 0.35 + 0.65 * max(face, 0.0);
                    vec3 hue = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + fs * 2.0 + uv.y);
                    col += band * shade * hue;
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
