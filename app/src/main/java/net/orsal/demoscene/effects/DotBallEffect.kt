package net.orsal.demoscene.effects

/**
 * Vector/dot balls: points spread evenly over a sphere (Fibonacci lattice),
 * rotated in 3D and projected with perspective so the near dots are bigger and
 * brighter, forming a rolling ball of bobs.
 */
class DotBallEffect : FragmentEffect("DotBall", 13f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;

            mat3 rotX(float a) {
                float c = cos(a); float s = sin(a);
                return mat3(1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c);
            }
            mat3 rotY(float a) {
                float c = cos(a); float s = sin(a);
                return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
            }

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                mat3 rot = rotY(uTime * 0.8) * rotX(uTime * 0.5);
                vec3 col = vec3(0.0);

                const float N = 150.0;
                for (int i = 0; i < 150; i++) {
                    float fi = float(i);
                    float y = 1.0 - 2.0 * (fi + 0.5) / N;
                    float r = sqrt(max(0.0, 1.0 - y * y));
                    float theta = 6.2831853 * fi * 0.6180339;
                    vec3 sp = vec3(r * cos(theta), y, r * sin(theta));
                    sp = rot * sp;

                    float persp = 1.0 / (2.4 - sp.z * 1.1);
                    vec2 pp = sp.xy * persp * 1.6;
                    float depth = sp.z * 0.5 + 0.5;

                    // Bigger, softer dots so they don't shimmer/strobe.
                    float rad = 0.016 + 0.030 * depth;
                    float d = length(uv - pp);
                    float dot = smoothstep(rad, rad * 0.2, d) * (0.25 + 0.85 * depth);

                    vec3 hue = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + fi * 0.05);
                    col += dot * hue;
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
