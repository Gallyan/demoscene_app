package net.orsal.demoscene.effects

/**
 * Glenz vectors: a rotating translucent octahedron. The ray is traced to the
 * front face and then continued through the interior to the back face, summing
 * face colours so you see through the solid the way old glenz objects did.
 */
class GlenzEffect : FragmentEffect("Glenz", 14f) {

    override fun fragmentSource() = FRAGMENT

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;

            mat3 rotX(float a) {
                float c = cos(a); float s = sin(a);
                return mat3(1.0, 0.0, 0.0, 0.0, c, -s, 0.0, s, c);
            }
            mat3 rotY(float a) {
                float c = cos(a); float s = sin(a);
                return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
            }

            mat3 gRot;

            float sdOcta(vec3 p, float s) {
                p = abs(p);
                return (p.x + p.y + p.z - s) * 0.57735;
            }

            float map(vec3 p) {
                return sdOcta(gRot * p, 1.1);
            }

            vec3 normalAt(vec3 p) {
                vec2 e = vec2(0.002, 0.0);
                return normalize(vec3(
                    map(p + e.xyy) - map(p - e.xyy),
                    map(p + e.yxy) - map(p - e.yxy),
                    map(p + e.yyx) - map(p - e.yyx)
                ));
            }

            vec3 faceColor(vec3 n) {
                return 0.5 + 0.5 * cos(
                    vec3(0.0, 2.0, 4.0) + n.x * 3.0 + n.y * 5.0 + n.z * 7.0 + uTime
                );
            }

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                gRot = rotY(uTime * 0.7) * rotX(uTime * 0.5 + 0.6);

                vec3 ro = vec3(0.0, 0.0, 4.0);
                vec3 rd = normalize(vec3(uv, -2.2));

                vec3 col = vec3(0.02, 0.02, 0.06);

                float t = 0.0;
                bool hit = false;
                for (int i = 0; i < 80; i++) {
                    vec3 p = ro + rd * t;
                    float d = map(p);
                    if (d < 0.001) { hit = true; break; }
                    t += d;
                    if (t > 8.0) break;
                }

                if (hit) {
                    vec3 p = ro + rd * t;
                    vec3 n1 = normalAt(p);
                    float fres = pow(1.0 - max(dot(n1, -rd), 0.0), 2.0);
                    col += faceColor(n1) * (0.5 + 0.5 * fres);

                    // March through the interior to the exit (back) face.
                    float tb = t + 0.05;
                    for (int j = 0; j < 90; j++) {
                        vec3 pb = ro + rd * tb;
                        if (map(pb) > 0.001) {
                            vec3 n2 = normalAt(pb);
                            col += faceColor(-n2) * 0.4;
                            break;
                        }
                        tb += 0.03;
                        if (tb > 8.0) break;
                    }
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
