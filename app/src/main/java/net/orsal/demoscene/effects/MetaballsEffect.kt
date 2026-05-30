package net.orsal.demoscene.effects

/**
 * Metaballs: several moving point fields summed into a scalar field, then
 * thresholded so the blobs merge and split organically.
 */
class MetaballsEffect : FragmentEffect("Metaballs", 13f) {

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

                float field = 0.0;
                for (int i = 0; i < 6; i++) {
                    float fi = float(i);
                    vec2 c = 0.7 * vec2(
                        sin(uTime * (0.5 + 0.11 * fi) + fi),
                        cos(uTime * (0.4 + 0.13 * fi) + fi * 1.7)
                    );
                    vec2 d = uv - c;
                    field += 0.08 / (dot(d, d) + 0.001);
                }

                float inside = smoothstep(1.6, 2.4, field);
                vec3 hue = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + uTime * 0.5);
                vec3 col = mix(vec3(0.0, 0.0, 0.06), hue, inside);
                // Glowing rim just outside the surface.
                col += vec3(0.6, 0.3, 0.9) * smoothstep(1.3, 1.6, field) * (1.0 - inside);

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
