package net.orsal.demoscene.effects

/**
 * The Amiga-era rotozoomer: an infinite tiled texture (here a checkerboard)
 * spun and zoomed about the centre, with a little colour cycling on top.
 */
class RotozoomEffect : FragmentEffect("Rotozoom", 13f) {

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

                float a = uTime * 0.5;
                float zoom = 1.6 + 1.1 * sin(uTime * 0.4);
                mat2 rot = mat2(cos(a), -sin(a), sin(a), cos(a));
                vec2 p = rot * uv * zoom + vec2(uTime * 0.25, uTime * 0.15);

                vec2 cell = floor(p * 2.0);
                float checker = mod(cell.x + cell.y, 2.0);
                vec3 a1 = vec3(0.10, 0.10, 0.35);
                vec3 a2 = vec3(0.95, 0.55, 0.15);
                vec3 col = mix(a1, a2, checker);

                // Colour cycling rings to keep it lively.
                col *= 0.7 + 0.3 * sin(length(p) * 2.0 - uTime * 2.0);

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
