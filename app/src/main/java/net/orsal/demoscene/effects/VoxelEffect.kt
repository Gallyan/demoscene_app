package net.orsal.demoscene.effects

/**
 * A voxel-style landscape: a procedural noise height field flown over by the
 * camera, raymarched per pixel, shaded grass-to-rock-to-snow by altitude with
 * distance fog into a sky gradient.
 */
class VoxelEffect : FragmentEffect("Voxel", 15f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
            }

            float noise(vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);
                f = f * f * (3.0 - 2.0 * f);
                float a = hash(i);
                float b = hash(i + vec2(1.0, 0.0));
                float c = hash(i + vec2(0.0, 1.0));
                float d = hash(i + vec2(1.0, 1.0));
                return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
            }

            float terrain(vec2 p) {
                return noise(p * 0.3) * 1.6 + noise(p * 0.9) * 0.5 - 1.0;
            }

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                vec3 ro = vec3(0.0, 1.6, uTime * 3.0);
                vec3 rd = normalize(vec3(uv.x, uv.y - 0.25, 1.0));

                float t = 0.0;
                bool hit = false;
                for (int i = 0; i < 130; i++) {
                    vec3 p = ro + rd * t;
                    if (p.y < terrain(p.xz)) { hit = true; break; }
                    t += 0.05 + t * 0.013;
                    if (t > 45.0) break;
                }

                vec3 col;
                if (hit) {
                    vec3 p = ro + rd * t;
                    vec2 e = vec2(0.06, 0.0);
                    float hl = terrain(p.xz - e.xy);
                    float hr = terrain(p.xz + e.xy);
                    float hd = terrain(p.xz - e.yx);
                    float hu = terrain(p.xz + e.yx);
                    vec3 n = normalize(vec3(hl - hr, 2.0 * e.x, hd - hu));
                    float diff = max(dot(n, normalize(vec3(0.5, 0.8, -0.3))), 0.0);

                    vec3 grass = vec3(0.18, 0.4, 0.12);
                    vec3 rock = vec3(0.4, 0.34, 0.28);
                    vec3 snow = vec3(0.9, 0.92, 0.96);
                    vec3 base = mix(grass, rock, smoothstep(0.0, 0.9, p.y));
                    base = mix(base, snow, smoothstep(1.1, 1.7, p.y));

                    col = base * (0.3 + 0.7 * diff);
                    float fog = 1.0 - exp(-t * 0.05);
                    col = mix(col, vec3(0.6, 0.72, 0.88), fog);
                } else {
                    float g = smoothstep(-0.3, 0.6, uv.y);
                    col = mix(vec3(0.62, 0.74, 0.9), vec3(0.2, 0.42, 0.8), g);
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
