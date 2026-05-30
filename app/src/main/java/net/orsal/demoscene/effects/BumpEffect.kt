package net.orsal.demoscene.effects

/**
 * Classic bump mapping: a procedural height field turned into per-pixel normals
 * and lit by a point light that circles over the surface (diffuse + specular).
 */
class BumpEffect : FragmentEffect("Bump", 12f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;

            float hgt(vec2 p) {
                float a = sin(p.x * 3.0) * cos(p.y * 3.0);
                float b = sin(length(p) * 5.0);
                return 0.5 + 0.35 * a + 0.15 * b;
            }

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;
                vec2 p = uv * 4.0;

                vec2 e = vec2(0.012, 0.0);
                float hx = hgt(p + e.xy) - hgt(p - e.xy);
                float hy = hgt(p + e.yx) - hgt(p - e.yx);
                vec3 n = normalize(vec3(-hx, -hy, 0.08));

                vec3 frag = vec3(uv, 0.0);
                vec3 lightPos = vec3(1.4 * sin(uTime), 1.4 * cos(uTime * 0.8), 1.0);
                vec3 l = normalize(lightPos - frag);

                float diff = max(dot(n, l), 0.0);
                float spec = pow(max(dot(reflect(-l, n), vec3(0.0, 0.0, 1.0)), 0.0), 32.0);

                vec3 base = vec3(0.2, 0.3, 0.75);
                vec3 col = base * (0.12 + diff) + vec3(1.0) * spec;

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
