package net.orsal.demoscene.effects

/**
 * A 3D starfield flying toward the camera: each star sits at a fixed direction
 * and cycles in depth, so as it approaches it streaks outward from the centre
 * and brightens, the way an old hyperspace tunnel of stars does.
 */
class StarfieldEffect : FragmentEffect("Starfield", 12f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;

            float hash(float n) {
                return fract(sin(n) * 43758.5453123);
            }

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                vec3 col = vec3(0.0);

                for (int i = 0; i < 110; i++) {
                    float fi = float(i);
                    // Fixed screen direction for this star.
                    vec2 dir = vec2(hash(fi) * 2.0 - 1.0, hash(fi + 7.1) * 2.0 - 1.0);
                    // Depth cycles 0 (far) -> 1 (near), staggered per star.
                    float z = fract(hash(fi + 3.3) + uTime * 0.35);

                    vec2 pos = dir / (1.05 - z);
                    float d = length(uv - pos);

                    // A small soft dot, plus a short streak for the near ones.
                    float dot = smoothstep(0.012, 0.0, d);
                    float star = dot * z * z;

                    // Twinkle / colour per star.
                    vec3 tint = 0.6 + 0.4 * vec3(
                        hash(fi + 1.0), hash(fi + 2.0), hash(fi + 5.0)
                    );
                    col += star * tint;
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
