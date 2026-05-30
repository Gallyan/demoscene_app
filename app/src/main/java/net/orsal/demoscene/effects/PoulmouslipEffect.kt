package net.orsal.demoscene.effects

import android.opengl.GLES20
import net.orsal.demoscene.R

/**
 * The kids' very own effect: a spinning 3D space hen in underpants -- a
 * "Poulmouslip" (poule mouillée en slip) -- raymarched from SDF primitives over
 * a starfield, with a sine scroller chanting the battle cry underneath.
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

            mat3 rotY(float a) {
                float c = cos(a); float s = sin(a);
                return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
            }

            float sdSphere(vec3 p, float r) { return length(p) - r; }

            float sdEllipsoid(vec3 p, vec3 r) {
                float k0 = length(p / r);
                float k1 = length(p / (r * r));
                return k0 * (k0 - 1.0) / max(k1, 0.0001);
            }

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

            vec2 opU(vec2 a, vec2 b) {
                return (a.x < b.x) ? a : b;
            }

            mat3 gRot;

            // Returns (distance, material id) in object space.
            vec2 hen(vec3 p) {
                vec2 res = vec2(sdEllipsoid(p - vec3(0.0, -0.1, 0.0), vec3(0.5, 0.6, 0.45)), 1.0);
                float head = sdSphere(p - vec3(0.0, 0.55, 0.05), 0.34);
                res.x = smin(res.x, head, 0.18);

                float wingL = sdEllipsoid(p - vec3(0.46, 0.0, 0.0), vec3(0.12, 0.34, 0.3));
                float wingR = sdEllipsoid(p - vec3(-0.46, 0.0, 0.0), vec3(0.12, 0.34, 0.3));
                res = opU(res, vec2(min(wingL, wingR), 1.0));

                float beak = sdEllipsoid(p - vec3(0.0, 0.48, 0.37), vec3(0.09, 0.07, 0.18));
                res = opU(res, vec2(beak, 2.0));

                float eyeL = sdSphere(p - vec3(0.14, 0.62, 0.27), 0.06);
                float eyeR = sdSphere(p - vec3(-0.14, 0.62, 0.27), 0.06);
                res = opU(res, vec2(min(eyeL, eyeR), 3.0));

                float comb = sdSphere(p - vec3(0.0, 0.9, 0.12), 0.11);
                comb = min(comb, sdSphere(p - vec3(0.0, 0.92, -0.02), 0.1));
                comb = min(comb, sdSphere(p - vec3(0.0, 0.85, 0.24), 0.09));
                res = opU(res, vec2(comb, 4.0));

                float wattle = sdSphere(p - vec3(0.0, 0.36, 0.32), 0.07);
                res = opU(res, vec2(wattle, 4.0));

                float ant = sdCapsule(p, vec3(0.0, 0.9, 0.0), vec3(0.0, 1.2, 0.0), 0.02);
                res = opU(res, vec2(ant, 2.0));
                float antBall = sdSphere(p - vec3(0.0, 1.25, 0.0), 0.07);
                res = opU(res, vec2(antBall, 4.0));

                float legL = sdCapsule(p, vec3(0.15, -0.6, 0.05), vec3(0.17, -0.95, 0.1), 0.04);
                float legR = sdCapsule(p, vec3(-0.15, -0.6, 0.05), vec3(-0.17, -0.95, 0.1), 0.04);
                res = opU(res, vec2(min(legL, legR), 2.0));

                float footL = sdCapsule(p, vec3(0.17, -0.95, 0.1), vec3(0.17, -0.95, 0.3), 0.04);
                float footR = sdCapsule(p, vec3(-0.17, -0.95, 0.1), vec3(-0.17, -0.95, 0.3), 0.04);
                res = opU(res, vec2(min(footL, footR), 2.0));

                return res;
            }

            vec2 map(vec3 p) {
                return hen(gRot * p);
            }

            vec3 calcNormal(vec3 p) {
                vec2 e = vec2(0.002, 0.0);
                return normalize(vec3(
                    map(p + e.xyy).x - map(p - e.xyy).x,
                    map(p + e.yxy).x - map(p - e.yxy).x,
                    map(p + e.yyx).x - map(p - e.yyx).x
                ));
            }

            vec3 spaceBg(vec2 uv) {
                vec3 c = mix(vec3(0.02, 0.0, 0.06), vec3(0.0, 0.03, 0.13), uv.y * 0.5 + 0.5);
                vec2 g = floor(uv * 60.0);
                float h = fract(sin(dot(g, vec2(12.9898, 78.233))) * 43758.5453);
                c += step(0.985, h);
                return c;
            }

            void main() {
                float aspect = uResolution.x / uResolution.y;
                vec2 uv = vPos;
                uv.x *= aspect;

                gRot = rotY(uTime * 1.0);

                vec3 ro = vec3(0.0, -0.05, 4.0);
                vec3 rd = normalize(vec3(uv, -2.2));

                float t = 0.0;
                bool hit = false;
                for (int i = 0; i < 90; i++) {
                    vec3 pos = ro + rd * t;
                    float d = map(pos).x;
                    if (d < 0.001) { hit = true; break; }
                    t += d;
                    if (t > 10.0) break;
                }

                vec3 col;
                if (hit) {
                    vec3 hp = ro + rd * t;
                    vec3 nrm = calcNormal(hp);
                    vec3 objp = gRot * hp;
                    float mat = map(hp).y;

                    vec3 base;
                    if (mat < 1.5) {
                        base = vec3(1.0, 0.93, 0.72); // feathers
                    } else if (mat < 2.5) {
                        base = vec3(1.0, 0.6, 0.05);  // beak / legs
                    } else if (mat < 3.5) {
                        base = vec3(0.03, 0.03, 0.03); // eyes
                    } else {
                        base = vec3(0.9, 0.12, 0.12);  // comb / wattle / antenna ball
                    }

                    // The slip: paint the lower torso of the body.
                    if (mat < 1.5 && objp.y < -0.02 && objp.y > -0.5
                            && length(objp.xz) < 0.62) {
                        vec3 slip = vec3(0.1, 0.35, 0.95);
                        float dots = sin(objp.x * 26.0) * sin(objp.y * 26.0 + objp.z * 12.0);
                        if (dots > 0.45) { slip = vec3(1.0); }
                        if (objp.y > -0.08) { slip = vec3(1.0); } // waistband
                        base = slip;
                    }

                    vec3 lightDir = normalize(vec3(0.4, 0.7, 0.6));
                    float diff = max(dot(nrm, lightDir), 0.0);
                    float rim = pow(1.0 - max(dot(nrm, -rd), 0.0), 2.0);
                    col = base * (0.28 + 0.8 * diff) + rim * 0.15;

                    if (mat > 2.5 && mat < 3.5) {
                        float glint = pow(max(dot(reflect(-lightDir, nrm), -rd), 0.0), 20.0);
                        col += vec3(1.0) * glint;
                    }
                } else {
                    col = spaceBg(uv);
                }

                // --- Battle-cry scroller along the bottom ---
                float bandHalf = 0.15;
                float waveCenter = -0.78 + 0.05 * sin(uv.x * 2.0 + uTime * 2.0);
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
