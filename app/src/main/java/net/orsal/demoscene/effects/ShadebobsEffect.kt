package net.orsal.demoscene.effects

/**
 * Shadebobs: a few bobs gliding along Lissajous paths, each leaving a fading
 * phosphor trail. The trail is summed analytically by sampling each bob's past
 * positions, so no accumulation buffer is needed.
 */
class ShadebobsEffect : FragmentEffect("Shadebobs", 13f) {

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

                vec3 col = vec3(0.0);
                for (int b = 0; b < 3; b++) {
                    float fb = float(b);
                    for (int k = 0; k < 48; k++) {
                        float t = uTime - float(k) * 0.03;
                        vec2 pos = vec2(
                            cos(t * 1.3 + fb * 2.1) * 0.8,
                            sin(t * 1.7 + fb * 2.1) * 0.55
                        );
                        float d = length(uv - pos);
                        float inten = exp(-d * d * 55.0) * (1.0 - float(k) / 48.0) * 0.5;
                        vec3 hue = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + fb * 2.0 + t);
                        col += inten * hue;
                    }
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
