package net.orsal.demoscene.effects

/**
 * Water ripples: several circular wave sources sum into a height field, whose
 * gradient becomes a surface normal used to refract a checker floor and add a
 * specular glint.
 */
class WaterEffect : FragmentEffect("Water", 13f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;

            float waves(vec2 p) {
                float h = 0.0;
                for (int i = 0; i < 4; i++) {
                    float fi = float(i);
                    vec2 src = vec2(
                        sin(uTime * 0.5 + fi * 1.7),
                        cos(uTime * 0.4 + fi * 2.3)
                    ) * 0.6;
                    float d = length(p - src);
                    h += sin(d * 24.0 - uTime * 3.0) / (1.0 + d * 6.0);
                }
                return h;
            }

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                vec2 e = vec2(0.004, 0.0);
                float hx = waves(uv + e.xy) - waves(uv - e.xy);
                float hy = waves(uv + e.yx) - waves(uv - e.yx);
                vec3 n = normalize(vec3(-hx, -hy, 0.05));

                vec2 duv = uv + n.xy * 0.3;
                vec2 cell = floor(duv * 5.0);
                float checker = mod(cell.x + cell.y, 2.0);
                vec3 base = mix(vec3(0.0, 0.1, 0.25), vec3(0.1, 0.45, 0.65), checker);

                float spec = pow(max(dot(n, normalize(vec3(0.4, 0.5, 1.0))), 0.0), 32.0);
                vec3 col = base + spec * vec3(1.0);

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
