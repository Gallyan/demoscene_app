package net.orsal.demoscene.effects

/**
 * A Mandelbrot set with a continuous zoom into the "seahorse valley" and a
 * smooth, time-cycled palette. Smooth iteration counting avoids the hard colour
 * banding of the naive escape-time render.
 */
class MandelbrotEffect : FragmentEffect("Mandelbrot", 16f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;

            const int MAX_ITER = 160;

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                // Continuous zoom + rotation toward a fixed detailed point.
                float scale = 2.6 * exp(-uTime * 0.22);
                float ang = uTime * 0.25;
                mat2 rot = mat2(cos(ang), -sin(ang), sin(ang), cos(ang));
                vec2 center = vec2(-0.743643887037151, 0.131825904205330);
                vec2 c = center + rot * uv * scale;

                vec2 z = vec2(0.0);
                float iter = 0.0;
                for (int i = 0; i < MAX_ITER; i++) {
                    z = vec2(z.x * z.x - z.y * z.y, 2.0 * z.x * z.y) + c;
                    if (dot(z, z) > 256.0) {
                        break;
                    }
                    iter += 1.0;
                }

                vec3 col;
                if (iter >= float(MAX_ITER) - 0.5) {
                    col = vec3(0.0);
                } else {
                    // Smooth (fractional) iteration count for banding-free colour.
                    float sm = iter - log2(log2(dot(z, z))) + 4.0;
                    col = 0.5 + 0.5 * cos(
                        vec3(0.0, 0.6, 1.0) + sm * 0.18 + uTime * 0.4
                    );
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
