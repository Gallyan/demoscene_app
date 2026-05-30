package net.orsal.demoscene.effects

/**
 * The classic sine-based plasma: a handful of overlapping sine fields summed
 * together and pushed through a cycling palette.
 */
class PlasmaEffect : FragmentEffect("Plasma", 14f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;

            void main() {
                vec2 p = vPos;
                p.x *= uResolution.x / uResolution.y;

                float t = uTime;
                float v = 0.0;
                v += sin(p.x * 5.0 + t);
                v += sin((p.y * 5.0 + t) * 0.5);
                v += sin((p.x * 4.0 + p.y * 4.0 + t) * 0.5);

                float cx = p.x + 0.6 * sin(t * 0.5);
                float cy = p.y + 0.6 * cos(t * 0.33);
                v += sin(sqrt(60.0 * (cx * cx + cy * cy) + 1.0) + t);

                v *= 0.5;
                float pi = 3.14159265;
                vec3 col = vec3(
                    sin(v * pi),
                    sin(v * pi + 2.094),
                    sin(v * pi + 4.188)
                );
                col = col * 0.5 + 0.5;

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
