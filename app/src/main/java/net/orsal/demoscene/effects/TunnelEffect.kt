package net.orsal.demoscene.effects

/**
 * The classic textured tunnel: screen polar coordinates map to an angle and a
 * depth (1/radius), the texture scrolls down the depth axis, and a gentle twist
 * keeps it from feeling like a flat pipe. The vanishing centre fades to black.
 */
class TunnelEffect : FragmentEffect("Tunnel", 14f) {

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

                float r = length(uv);
                float angle = atan(uv.y, uv.x);

                float depth = 1.0 / (r + 0.001) + uTime * 1.5;
                float around = angle / 3.14159265;
                around += 0.25 * sin(depth * 0.5 + uTime); // twist

                vec2 cell = floor(vec2(around * 6.0, depth * 1.5));
                float checker = mod(cell.x + cell.y, 2.0);
                vec3 c1 = vec3(0.15, 0.0, 0.35);
                vec3 c2 = vec3(0.95, 0.65, 0.20);
                vec3 col = mix(c1, c2, checker);

                // Light rings running down the tunnel.
                col *= 0.6 + 0.4 * sin(depth * 3.1415);

                // Depth fog: the centre (r -> 0) is infinitely far, so darken it.
                col *= smoothstep(0.0, 0.55, r);

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
