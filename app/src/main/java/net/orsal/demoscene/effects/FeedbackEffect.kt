package net.orsal.demoscene.effects

/**
 * A fake video-feedback effect: instead of an accumulation buffer, the domain is
 * repeatedly zoomed and rotated in a loop, stamping a bright repeating motif at
 * each step, which produces the endless recursive feedback tunnel.
 */
class FeedbackEffect : FragmentEffect("Feedback", 13f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;

            mat2 rot(float a) {
                float c = cos(a); float s = sin(a);
                return mat2(c, -s, s, c);
            }

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                vec2 p = uv;
                vec3 col = vec3(0.0);
                float decay = 1.0;
                mat2 warp = rot(0.25 + 0.08 * sin(uTime * 0.3)) * 1.09;

                for (int i = 0; i < 26; i++) {
                    float fi = float(i);
                    p = warp * p;
                    p += 0.02 * vec2(sin(uTime + fi), cos(uTime * 0.7 + fi));

                    vec2 q = fract(p * 0.5) - 0.5;
                    float spot = exp(-dot(q, q) * 9.0);
                    vec3 hue = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + fi * 0.4 + uTime);
                    col += spot * hue * decay;
                    decay *= 0.88;
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
