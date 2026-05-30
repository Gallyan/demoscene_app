package net.orsal.demoscene.effects

import android.opengl.GLES20
import net.orsal.demoscene.R

/**
 * The kids' very own effect: four little "Poulmouslip" hens (poules mouillées en
 * slip) sitting in a kid's inflatable dinghy, bobbing down a flowing river. The
 * hens, dinghy and water are raymarched / shaded procedurally, with a sine
 * scroller chanting the battle cry underneath. While it plays, a robotic talkbox
 * voice says "poulmouslip" over the music (see ChiptunePlayer / VoiceSynth).
 */
class PoulmouslipEffect : FragmentEffect("Poulmouslip", 16f) {

    private var uTex = 0
    private var uTexAspect = 0

    private var textureId = 0
    private var texAspect = 8f

    override fun fragmentSource() = FRAGMENT

    override fun onProgramReady(program: Int) {
        uTex = GLES20.glGetUniformLocation(program, "uTex")
        uTexAspect = GLES20.glGetUniformLocation(program, "uTexAspect")
        val text = TextTexture.build(androidContext.getString(R.string.poulmouslip_text))
        textureId = text.textureId
        texAspect = text.aspect
    }

    override fun onRender(program: Int, time: Float) {
        GLES20.glUniform1f(uTexAspect, texAspect)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTex, 0)
    }

    override fun onDispose() {
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
    }

    companion object {
        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;
            uniform sampler2D uTex;
            uniform float uTexAspect;

            float sdSphere(vec3 p, float r) { return length(p) - r; }

            float sdEllipsoid(vec3 p, vec3 r) {
                float k0 = length(p / r);
                float k1 = length(p / (r * r));
                return k0 * (k0 - 1.0) / max(k1, 0.0001);
            }

            float sdTorus(vec3 p, vec2 t) {
                vec2 q = vec2(length(p.xz) - t.x, p.y);
                return length(q) - t.y;
            }

            float smin(float a, float b, float k) {
                float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
                return mix(b, a, h) - k * h * (1.0 - h);
            }

            vec2 opU(vec2 a, vec2 b) { return (a.x < b.x) ? a : b; }

            // One small hen in local space (facing +z, sitting). Materials:
            // 1 feathers, 2 beak, 3 eye, 4 comb, 5 slip.
            vec2 hen(vec3 p) {
                float body = sdEllipsoid(p - vec3(0.0, 0.18, 0.0), vec3(0.22, 0.20, 0.22));
                float head = sdSphere(p - vec3(0.0, 0.42, 0.06), 0.14);
                float d = smin(body, head, 0.08);
                float mat = (p.y < 0.18) ? 5.0 : 1.0; // lower body = slip
                vec2 res = vec2(d, mat);

                float beak = sdEllipsoid(p - vec3(0.0, 0.40, 0.18), vec3(0.04, 0.03, 0.07));
                res = opU(res, vec2(beak, 2.0));

                float comb = sdSphere(p - vec3(0.0, 0.55, 0.04), 0.05);
                res = opU(res, vec2(comb, 4.0));

                float eyes = min(
                    sdSphere(p - vec3(0.06, 0.46, 0.15), 0.02),
                    sdSphere(p - vec3(-0.06, 0.46, 0.15), 0.02)
                );
                res = opU(res, vec2(eyes, 3.0));
                return res;
            }

            float boatY() {
                return 0.18 + 0.05 * sin(uTime * 1.5);
            }

            vec2 mapScene(vec3 p) {
                float by = boatY();
                vec3 bp = p - vec3(0.0, by, 0.0);

                // Inflatable dinghy: a fat tube ring plus a rounded bottom.
                float ring = sdTorus(bp, vec2(0.95, 0.22));
                float bottom = sdEllipsoid(bp - vec3(0.0, -0.18, 0.0), vec3(1.05, 0.18, 1.05));
                vec2 res = vec2(min(ring, bottom), 10.0);

                // Four hens seated inside.
                res = opU(res, hen(p - vec3(0.42, by, 0.42)));
                res = opU(res, hen(p - vec3(-0.42, by, 0.42)));
                res = opU(res, hen(p - vec3(0.42, by, -0.42)));
                res = opU(res, hen(p - vec3(-0.42, by, -0.42)));
                return res;
            }

            vec3 calcNormal(vec3 p) {
                vec2 e = vec2(0.002, 0.0);
                return normalize(vec3(
                    mapScene(p + e.xyy).x - mapScene(p - e.xyy).x,
                    mapScene(p + e.yxy).x - mapScene(p - e.yxy).x,
                    mapScene(p + e.yyx).x - mapScene(p - e.yyx).x
                ));
            }

            vec3 skyCol(vec3 rd) {
                float h = clamp(rd.y * 0.5 + 0.5, 0.0, 1.0);
                vec3 sky = mix(vec3(0.75, 0.86, 1.0), vec3(0.2, 0.45, 0.85), h);
                float sun = pow(max(dot(rd, normalize(vec3(0.3, 0.5, -0.6))), 0.0), 220.0);
                sky += vec3(1.0, 0.95, 0.8) * sun * 2.0;
                return sky;
            }

            float riverWave(vec2 xz) {
                float h = sin(xz.x * 3.0 + xz.y * 1.0 + uTime * 1.6) * 0.5;
                h += sin(xz.x * 1.5 - xz.y * 2.5 + uTime * 2.2) * 0.3;
                return h;
            }

            // Background: flowing river between grassy banks, under a sky.
            vec3 scene(vec3 ro, vec3 rd) {
                if (rd.y < -0.001) {
                    float t = -ro.y / rd.y;
                    vec3 p = ro + rd * t;
                    float fog = clamp(1.0 - exp(-t * 0.04), 0.0, 1.0);
                    vec3 horizon = vec3(0.72, 0.83, 0.96);

                    if (abs(p.x) > 2.6) {
                        float n = fract(sin(dot(floor(p.xz * 2.0), vec2(12.9898, 78.233))) * 43758.5);
                        vec3 grass = vec3(0.18, 0.42, 0.14) * (0.8 + 0.3 * n);
                        return mix(grass, horizon, fog);
                    }

                    vec2 e = vec2(0.05, 0.0);
                    float hx = riverWave(p.xz + e.xy) - riverWave(p.xz - e.xy);
                    float hz = riverWave(p.xz + e.yx) - riverWave(p.xz - e.yx);
                    vec3 n = normalize(vec3(-hx * 0.3, 1.0, -hz * 0.3));
                    vec3 refl = reflect(rd, n);
                    vec3 water = mix(vec3(0.05, 0.2, 0.28), skyCol(refl), 0.55);
                    float spec = pow(max(dot(refl, normalize(vec3(0.3, 0.5, -0.6))), 0.0), 64.0);
                    water += spec * vec3(1.0);
                    return mix(water, horizon, fog);
                }
                return skyCol(rd);
            }

            vec3 matColor(float mat) {
                if (mat < 1.5) { return vec3(1.0, 0.93, 0.72); } // feathers
                if (mat < 2.5) { return vec3(1.0, 0.6, 0.05); }  // beak
                if (mat < 3.5) { return vec3(0.03, 0.03, 0.03); } // eye
                if (mat < 4.5) { return vec3(0.9, 0.12, 0.12); }  // comb
                if (mat < 5.5) { return vec3(0.1, 0.35, 0.95); }  // slip
                return vec3(1.0, 0.8, 0.1);                       // dinghy
            }

            void main() {
                float aspect = uResolution.x / uResolution.y;
                vec2 uv = vPos;
                uv.x *= aspect;

                vec3 ro = vec3(0.0, 1.3, 3.7);
                vec3 rd = normalize(vec3(uv.x, uv.y - 0.28, -1.6));

                float t = 0.0;
                bool hit = false;
                for (int i = 0; i < 90; i++) {
                    vec3 pos = ro + rd * t;
                    float d = mapScene(pos).x;
                    if (d < 0.001) { hit = true; break; }
                    t += d;
                    if (t > 14.0) break;
                }

                vec3 col;
                if (hit) {
                    vec3 hp = ro + rd * t;
                    vec3 nrm = calcNormal(hp);
                    float mat = mapScene(hp).y;
                    vec3 base = matColor(mat);

                    vec3 lightDir = normalize(vec3(0.4, 0.8, 0.3));
                    float diff = max(dot(nrm, lightDir), 0.0);
                    float rim = pow(1.0 - max(dot(nrm, -rd), 0.0), 2.0);
                    col = base * (0.3 + 0.8 * diff) + rim * 0.12;

                    // Glossy highlight on eyes and the rubbery dinghy.
                    if (mat > 9.5 || (mat > 2.5 && mat < 3.5)) {
                        float glint = pow(max(dot(reflect(-lightDir, nrm), -rd), 0.0), 24.0);
                        col += vec3(1.0) * glint;
                    }
                } else {
                    col = scene(ro, rd);
                }

                // --- Battle-cry scroller along the bottom ---
                float bandHalf = 0.15;
                float waveCenter = -0.80 + 0.05 * sin(uv.x * 2.0 + uTime * 2.0);
                float vb = (vPos.y - waveCenter) / bandHalf;
                if (abs(vb) < 1.0) {
                    float speed = 0.45;
                    float textWidthNDC = uTexAspect * (2.0 * bandHalf);
                    float u = fract((uv.x + uTime * speed) / textWidthNDC);
                    float tv = 0.5 - 0.5 * vb;
                    vec4 texel = texture2D(uTex, vec2(u, tv));
                    col = texel.rgb + col * (1.0 - texel.a);
                }

                gl_FragColor = vec4(col * uFade, 1.0);
            }
        """
    }
}
