package net.orsal.demoscene.effects

/**
 * Kefren bars: a row of vertical bars whose heights ripple as a travelling sine
 * wave, each bar cylindrically shaded across its width and tinted from a cycling
 * palette, like a waving curtain of bars.
 */
class KefrenEffect : FragmentEffect("Kefren", 12f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;
            uniform float uBeat;

            void main() {
                vec2 uv = vPos;

                float bars = 36.0;
                float cell = (uv.x * 0.5 + 0.5) * bars;
                float idx = floor(cell);
                float fx = fract(cell);

                // Sharp pump on every musical beat.
                float pulse = pow(1.0 - fract(uBeat), 3.0);

                float wave = sin(idx * 0.45 + uTime * 2.2);
                float center = wave * 0.55 * (1.0 + 0.35 * pulse);
                float halfH = 0.30 + 0.15 * pulse + 0.1 * sin(idx * 0.3 - uTime);

                float d = abs(uv.y - center);
                float bar = smoothstep(halfH, halfH - 0.06, d);
                float curve = sin(fx * 3.14159265); // cylindrical shading

                vec3 hue = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + idx * 0.3 + uTime * 0.5);
                vec3 col = hue * bar * curve * (1.0 + 0.5 * pulse);

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
