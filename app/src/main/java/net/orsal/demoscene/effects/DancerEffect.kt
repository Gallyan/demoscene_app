package net.orsal.demoscene.effects

/**
 * A dancing silhouette in the spirit of Spaceballs' "State of the Art": an
 * articulated figure built from 2D capsules (torso, head, swinging arms, stepping
 * legs) shown as a solid silhouette, with figure and background colours flipping
 * in time with the music.
 */
class DancerEffect : FragmentEffect("Dancer", 15f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;
            uniform float uBeat;

            float sdSeg(vec2 p, vec2 a, vec2 b, float r) {
                vec2 pa = p - a;
                vec2 ba = b - a;
                float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
                return length(pa - ba * h) - r;
            }

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                float t = uTime * 3.0;
                float bounce = 0.04 * abs(sin(t));
                float sway = 0.13 * sin(t);

                vec2 hip = vec2(sway, -0.15 + bounce);
                vec2 neck = hip + vec2(0.06 * sin(t + 0.5), 0.5);
                vec2 head = neck + vec2(0.0, 0.17);
                vec2 shL = neck + vec2(-0.15, 0.02);
                vec2 shR = neck + vec2(0.15, 0.02);

                // Arms swing in opposite phase.
                float aL = 2.3 + 0.8 * sin(t);
                float aR = 0.84 - 0.8 * sin(t);
                vec2 elL = shL + 0.26 * vec2(cos(aL), sin(aL));
                vec2 elR = shR + 0.26 * vec2(cos(aR), sin(aR));
                float fL = aL + 0.6 + 0.5 * sin(t * 1.3);
                float fR = aR - 0.6 - 0.5 * sin(t * 1.3);
                vec2 haL = elL + 0.24 * vec2(cos(fL), sin(fL));
                vec2 haR = elR + 0.24 * vec2(cos(fR), sin(fR));

                // Legs step in opposite phase.
                vec2 hipL = hip + vec2(-0.08, 0.0);
                vec2 hipR = hip + vec2(0.08, 0.0);
                float lL = -1.30 + 0.45 * sin(t);
                float lR = -1.84 - 0.45 * sin(t);
                vec2 knL = hipL + 0.30 * vec2(cos(lL), sin(lL));
                vec2 knR = hipR + 0.30 * vec2(cos(lR), sin(lR));
                float kL = lL - 0.4 - 0.4 * max(0.0, sin(t));
                float kR = lR - 0.4 - 0.4 * max(0.0, -sin(t));
                vec2 foL = knL + 0.28 * vec2(cos(kL), sin(kL));
                vec2 foR = knR + 0.28 * vec2(cos(kR), sin(kR));

                float d = 1e9;
                d = min(d, sdSeg(uv, hip, neck, 0.085));      // torso
                d = min(d, length(uv - head) - 0.12);          // head
                d = min(d, sdSeg(uv, shL, shR, 0.05));         // shoulders
                d = min(d, sdSeg(uv, hipL, hipR, 0.06));       // hips
                d = min(d, sdSeg(uv, shL, elL, 0.05));
                d = min(d, sdSeg(uv, elL, haL, 0.045));
                d = min(d, sdSeg(uv, shR, elR, 0.05));
                d = min(d, sdSeg(uv, elR, haR, 0.045));
                d = min(d, sdSeg(uv, hipL, knL, 0.06));
                d = min(d, sdSeg(uv, knL, foL, 0.05));
                d = min(d, sdSeg(uv, hipR, knR, 0.06));
                d = min(d, sdSeg(uv, knR, foR, 0.05));

                float mask = smoothstep(0.006, -0.006, d);

                // Figure / background colours flip every half beat.
                float flip = step(0.5, fract(uBeat));
                vec3 c1 = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + uTime * 0.7);
                vec3 c2 = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + uTime * 0.7 + 3.14159);
                vec3 bg = mix(c1, c2, flip);
                vec3 fig = mix(c2, c1, flip);
                vec3 col = mix(bg, fig, mask);

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
