package net.orsal.demoscene.effects

/**
 * A dancing silhouette in the spirit of Spaceballs' "State of the Art": a smooth,
 * curvy female figure built from tapered rounded-cones smoothly merged into one
 * continuous silhouette (hips, waist, bust, tapering limbs), dancing fluidly,
 * with figure and background colours flipping in time with the music.
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

            // 2D rounded cone (capsule with two radii) -- iq.
            float sdRoundCone(vec2 p, vec2 a, vec2 b, float r1, float r2) {
                vec2 ba = b - a;
                float l2 = dot(ba, ba);
                float rr = r1 - r2;
                float a2 = l2 - rr * rr;
                float il2 = 1.0 / l2;
                vec2 pa = p - a;
                float y = dot(pa, ba);
                float z = y - l2;
                vec2 xv = pa * l2 - ba * y;
                float x2 = dot(xv, xv);
                float y2 = y * y * l2;
                float z2 = z * z * l2;
                float k = sign(rr) * rr * rr * x2;
                if (sign(z) * a2 * z2 > k) { return sqrt(x2 + z2) * il2 - r2; }
                if (sign(y) * a2 * y2 < k) { return sqrt(x2 + y2) * il2 - r1; }
                return (sqrt(x2 * a2 * il2) + y * rr) * il2 - r1;
            }

            float smin(float a, float b, float k) {
                float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
                return mix(b, a, h) - k * h * (1.0 - h);
            }

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                float t = uTime * 2.2;
                float sway = 0.10 * sin(t);

                // Spine, bottom to top, with a feminine hourglass.
                vec2 pelvis = vec2(sway, -0.18);
                vec2 waist = pelvis + vec2(0.03 * sin(t), 0.16);
                vec2 chest = waist + vec2(0.04 * sin(t + 0.3), 0.20);
                vec2 neck = chest + vec2(0.0, 0.12);
                vec2 head = neck + vec2(0.02 * sin(t), 0.12);

                vec2 shL = chest + vec2(-0.12, 0.05);
                vec2 shR = chest + vec2(0.12, 0.05);
                vec2 hipL = pelvis + vec2(-0.09, 0.0);
                vec2 hipR = pelvis + vec2(0.09, 0.0);

                // Arms flow overhead and sway in opposite phase.
                float aL = 2.1 + 0.6 * sin(t);
                float aR = 1.04 - 0.6 * sin(t);
                vec2 elL = shL + 0.24 * vec2(cos(aL), sin(aL));
                vec2 elR = shR + 0.24 * vec2(cos(aR), sin(aR));
                float fL = aL + 0.4 + 0.4 * sin(t * 1.4);
                float fR = aR - 0.4 - 0.4 * sin(t * 1.4);
                vec2 wrL = elL + 0.22 * vec2(cos(fL), sin(fL));
                vec2 wrR = elR + 0.22 * vec2(cos(fR), sin(fR));

                // Legs shift weight and step.
                float lL = -1.45 + 0.32 * sin(t);
                float lR = -1.69 - 0.32 * sin(t);
                vec2 knL = hipL + 0.30 * vec2(cos(lL), sin(lL));
                vec2 knR = hipR + 0.30 * vec2(cos(lR), sin(lR));
                float sL = lL - 0.2 - 0.3 * max(0.0, sin(t));
                float sR = lR - 0.2 - 0.3 * max(0.0, -sin(t));
                vec2 anL = knL + 0.30 * vec2(cos(sL), sin(sL));
                vec2 anR = knR + 0.30 * vec2(cos(sR), sin(sR));

                float k = 0.05;
                float d = sdRoundCone(uv, pelvis, waist, 0.12, 0.075);
                d = smin(d, sdRoundCone(uv, waist, chest, 0.075, 0.105), k);
                d = smin(d, sdRoundCone(uv, chest, neck, 0.105, 0.05), k);
                d = smin(d, sdRoundCone(uv, neck, head, 0.05, 0.02), k);
                d = smin(d, length(uv - head) - 0.11, k);
                d = smin(d, sdRoundCone(uv, shL, elL, 0.055, 0.04), k);
                d = smin(d, sdRoundCone(uv, elL, wrL, 0.04, 0.022), k);
                d = smin(d, sdRoundCone(uv, shR, elR, 0.055, 0.04), k);
                d = smin(d, sdRoundCone(uv, elR, wrR, 0.04, 0.022), k);
                d = smin(d, sdRoundCone(uv, hipL, knL, 0.085, 0.055), k);
                d = smin(d, sdRoundCone(uv, knL, anL, 0.055, 0.028), k);
                d = smin(d, sdRoundCone(uv, hipR, knR, 0.085, 0.055), k);
                d = smin(d, sdRoundCone(uv, knR, anR, 0.055, 0.028), k);

                float mask = smoothstep(0.004, -0.004, d);

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
