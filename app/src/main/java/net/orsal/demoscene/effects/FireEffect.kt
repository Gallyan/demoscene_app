package net.orsal.demoscene.effects

/**
 * A rising fire effect: fractal value noise scrolling upward, faded toward the
 * top and pushed through a black-red-orange-yellow flame ramp.
 */
class FireEffect : FragmentEffect("Fire", 12f) {

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

            float fbm(vec2 p) {
                float v = 0.0;
                float amp = 0.5;
                for (int i = 0; i < 5; i++) {
                    v += amp * noise(p);
                    p *= 2.0;
                    amp *= 0.5;
                }
                return v;
            }

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                vec2 p = vec2(uv.x * 2.0, uv.y * 1.5);
                float n = fbm(p * 1.5 + vec2(0.0, -uTime * 2.0));
                float grad = smoothstep(1.0, -0.85, vPos.y);
                float flame = n * grad * 1.9;

                vec3 col = vec3(0.0);
                col += vec3(1.0, 0.15, 0.0) * smoothstep(0.25, 0.5, flame);
                col += vec3(1.0, 0.6, 0.0) * smoothstep(0.45, 0.75, flame);
                col += vec3(1.0, 1.0, 0.7) * smoothstep(0.75, 1.05, flame);

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
