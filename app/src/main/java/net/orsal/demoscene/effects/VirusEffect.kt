package net.orsal.demoscene.effects

/**
 * A virus in the classic HIV-render style: a spherical capsid studded with
 * lollipop glycoprotein spikes (a thin stalk topped by a knob) spread evenly
 * over the surface. The spikes grow outward with the music's bass energy. Grey-
 * blue capsid, red spike heads.
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
            float gLen;

            const float RBODY = 1.0;
            const float KNOB = 0.13;

            float sdCapsule(vec3 p, vec3 a, vec3 b, float r) {
                vec3 pa = p - a;
                vec3 ba = b - a;
                float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
                return length(pa - ba * h) - r;
            }

            float smin(float a, float b, float k) {
                float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
                return mix(b, a, h) - k * h * (1.0 - h);
            }

            vec3 spikeDir(int i) {
                float fi = float(i);
                float y = 1.0 - 2.0 * (fi + 0.5) / 24.0;
                float r = sqrt(max(0.0, 1.0 - y * y));
                float th = 2.399963 * fi; // golden angle
                return vec3(r * cos(th), y, r * sin(th));
            }

            float map(vec3 p) {
                p = gRot * p;
                float d = length(p) - RBODY;
                float spikes = 1e9;
                for (int i = 0; i < 24; i++) {
                    vec3 dir = spikeDir(i);
                    vec3 base = dir * (RBODY * 0.85);
                    vec3 tip = dir * (RBODY + gLen);
                    float stalk = sdCapsule(p, base, tip, 0.038);
                    float knob = length(p - tip) - KNOB;
                    spikes = min(spikes, min(stalk, knob));
                }
                return smin(d, spikes, 0.06);
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

                float energy = 0.0;
                for (int i = 0; i < 10; i++) {
                    energy += uBands[i];
                }
                energy /= 10.0;
                gLen = 0.35 + 0.75 * energy;

                gRot = rotY(uTime * 0.4) * rotX(uTime * 0.25);

                vec3 ro = vec3(0.0, 0.0, 4.5);
                vec3 rd = normalize(vec3(uv, -2.4));

                // Bounding sphere so we only march inside the virus shell.
                float rMax = RBODY + gLen + KNOB + 0.05;
                float b = dot(ro, rd);
                float c = dot(ro, ro) - rMax * rMax;
                float disc = b * b - c;

                vec3 col = vec3(0.02, 0.03, 0.05);
                if (disc > 0.0) {
                    float tEnter = max(-b - sqrt(disc), 0.0);
                    float tExit = -b + sqrt(disc);
                    float t = tEnter;
                    bool hit = false;
                    for (int i = 0; i < 90; i++) {
                        vec3 p = ro + rd * t;
                        float d = map(p);
                        if (d < 0.001) { hit = true; break; }
                        t += d;
                        if (t > tExit) break;
                    }

                    if (hit) {
                        vec3 p = ro + rd * t;
                        vec3 n = calcNormal(p);
                        float r = length(gRot * p);

                        vec3 capsid = vec3(0.45, 0.5, 0.62);
                        vec3 spikeCol = vec3(0.85, 0.12, 0.14);
                        float s = smoothstep(RBODY + 0.04, RBODY + gLen * 0.6, r);
                        vec3 base = mix(capsid, spikeCol, s);

                        vec3 lightDir = normalize(vec3(0.5, 0.7, 0.6));
                        float diff = max(dot(n, lightDir), 0.0);
                        float rim = pow(1.0 - max(dot(n, -rd), 0.0), 2.0);
                        col = base * (0.25 + 0.85 * diff) + rim * 0.25;
                        float spec = pow(max(dot(reflect(-lightDir, n), -rd), 0.0), 28.0);
                        col += vec3(1.0) * spec * 0.6;
                    }
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
