package net.orsal.demoscene.effects

/**
 * A particle fountain: each particle is launched from the bottom centre with a
 * hashed angle and speed, follows ballistic motion under gravity, recycles when
 * its life ends, and fades from yellow to red as it ages.
 */
class ParticlesEffect : FragmentEffect("Particles", 13f) {

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
                float life = 2.6;
                for (int i = 0; i < 140; i++) {
                    float fi = float(i);
                    float birth = hash(fi) * life;
                    float t = mod(uTime - birth, life);

                    float ang = (hash(fi + 1.0) - 0.5) * 1.2 + 1.5708;
                    float spd = 1.1 + hash(fi + 2.0) * 0.9;
                    vec2 vel = vec2(cos(ang), sin(ang)) * spd;
                    vec2 pos = vec2(0.0, -0.9) + vel * t + vec2(0.0, -1.6) * t * t * 0.5;

                    float age = t / life;
                    float d = length(uv - pos);
                    float dot = smoothstep(0.022, 0.0, d) * (1.0 - age);
                    vec3 hue = mix(vec3(1.0, 0.85, 0.3), vec3(1.0, 0.2, 0.05), age);
                    col += dot * hue;
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
