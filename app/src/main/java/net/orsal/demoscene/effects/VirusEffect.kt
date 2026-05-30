package net.orsal.demoscene.effects

/**
 * A spiky "virus" ball: a raymarched sphere displaced by a 3D lattice of spikes
 * whose length grows with the music's bass energy, so the spikes pump out in
 * rhythm. Sickly-green body with bright spike tips.
 */
class VirusEffect : FragmentEffect("Virus", 14f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;
            uniform float uBands[32];

            mat3 rotY(float a) {
                float c = cos(a); float s = sin(a);
                return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
            }
            mat3 rotX(float a) {
                float c = cos(a); float s = sin(a);
                return mat3(1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c);
            }

            mat3 gRot;
            float gSpike;

            float spikePattern(vec3 d) {
                return pow(abs(sin(d.x * 8.0) * sin(d.y * 8.0) * sin(d.z * 8.0)), 0.35);
            }

            float map(vec3 p) {
                p = gRot * p;
                float r = length(p);
                vec3 d = p / max(r, 0.0001);
                return r - (1.0 + gSpike * spikePattern(d));
            }

            vec3 calcNormal(vec3 p) {
                vec2 e = vec2(0.002, 0.0);
                return normalize(vec3(
                    map(p + e.xyy) - map(p - e.xyy),
                    map(p + e.yxy) - map(p - e.yxy),
                    map(p + e.yyx) - map(p - e.yyx)
                ));
            }

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                // Bass-weighted energy drives how far the spikes grow.
                float energy = 0.0;
                for (int i = 0; i < 10; i++) {
                    energy += uBands[i];
                }
                energy /= 10.0;
                gSpike = 0.15 + 1.3 * energy;

                gRot = rotY(uTime * 0.4) * rotX(uTime * 0.3);

                vec3 ro = vec3(0.0, 0.0, 4.2);
                vec3 rd = normalize(vec3(uv, -2.2));

                float t = 0.0;
                bool hit = false;
                for (int i = 0; i < 140; i++) {
                    vec3 p = ro + rd * t;
                    float d = map(p);
                    if (d < 0.001) { hit = true; break; }
                    t += d * 0.6; // step short: displacement is not a true SDF
                    if (t > 10.0) break;
                }

                vec3 col;
                if (hit) {
                    vec3 p = ro + rd * t;
                    vec3 n = calcNormal(p);
                    vec3 d = normalize(gRot * p);
                    float pat = spikePattern(d);

                    vec3 lightDir = normalize(vec3(0.5, 0.7, 0.6));
                    float diff = max(dot(n, lightDir), 0.0);
                    float rim = pow(1.0 - max(dot(n, -rd), 0.0), 2.0);

                    vec3 body = mix(vec3(0.08, 0.35, 0.14), vec3(0.7, 1.0, 0.2), pat);
                    col = body * (0.25 + 0.85 * diff) + rim * vec3(0.25, 0.8, 0.4);
                    float spec = pow(max(dot(reflect(-lightDir, n), -rd), 0.0), 24.0);
                    col += vec3(0.8, 1.0, 0.7) * spec;
                } else {
                    col = vec3(0.02, 0.05, 0.03);
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
