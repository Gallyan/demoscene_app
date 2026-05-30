package net.orsal.demoscene.effects

import android.opengl.GLES20
import net.orsal.demoscene.Effect
import net.orsal.demoscene.EffectContext
import net.orsal.demoscene.FullscreenQuad
import net.orsal.demoscene.GLUtil

/**
 * A spinning chrome torus, raymarched against a signed-distance field. The
 * chrome look comes from reflecting the view ray off the surface and sampling
 * a procedural environment, plus a tight specular highlight.
 */
class ChromeTorusEffect : Effect {

    override val name = "ChromeTorus"
    override val duration = 16f

    private var program = 0
    private var aPos = 0
    private var uTime = 0
    private var uResolution = 0
    private var uFade = 0

    private var width = 1f
    private var height = 1f

    private lateinit var quad: FullscreenQuad

    override fun onSurfaceCreated(context: EffectContext) {
        quad = context.quad
        program = GLUtil.buildProgram(VERTEX, FRAGMENT)
        aPos = GLES20.glGetAttribLocation(program, "aPos")
        uTime = GLES20.glGetUniformLocation(program, "uTime")
        uResolution = GLES20.glGetUniformLocation(program, "uResolution")
        uFade = GLES20.glGetUniformLocation(program, "uFade")
    }

    override fun onResize(width: Int, height: Int) {
        this.width = width.toFloat()
        this.height = height.toFloat()
    }

    override fun render(time: Float, fade: Float) {
        GLES20.glUseProgram(program)
        GLES20.glUniform1f(uTime, time)
        GLES20.glUniform2f(uResolution, width, height)
        GLES20.glUniform1f(uFade, fade)
        quad.draw(aPos)
    }

    override fun dispose() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }

    companion object {
        private const val VERTEX = """
            attribute vec2 aPos;
            varying vec2 vPos;
            void main() {
                vPos = aPos;
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
        """

        private const val FRAGMENT = """
            precision highp float;
            varying vec2 vPos;
            uniform float uTime;
            uniform vec2 uResolution;
            uniform float uFade;

            mat3 rotX(float a) {
                float c = cos(a); float s = sin(a);
                return mat3(1.0, 0.0, 0.0,
                            0.0, c, -s,
                            0.0, s, c);
            }

            mat3 rotY(float a) {
                float c = cos(a); float s = sin(a);
                return mat3(c, 0.0, s,
                            0.0, 1.0, 0.0,
                            -s, 0.0, c);
            }

            mat3 rotZ(float a) {
                float c = cos(a); float s = sin(a);
                return mat3(c, -s, 0.0,
                            s, c, 0.0,
                            0.0, 0.0, 1.0);
            }

            // Signed distance to a torus with radii t.x (ring) and t.y (tube).
            float sdTorus(vec3 p, vec2 t) {
                vec2 q = vec2(length(p.xz) - t.x, p.y);
                return length(q) - t.y;
            }

            mat3 gRot;

            float map(vec3 p) {
                p = gRot * p;
                return sdTorus(p, vec2(1.0, 0.4));
            }

            vec3 calcNormal(vec3 p) {
                vec2 e = vec2(0.001, 0.0);
                return normalize(vec3(
                    map(p + e.xyy) - map(p - e.xyy),
                    map(p + e.yxy) - map(p - e.yxy),
                    map(p + e.yyx) - map(p - e.yyx)
                ));
            }

            const float FLOOR_Y = -1.7;

            // The whole world: a checkerboard floor and a graded sky with a hot
            // horizon. Used both for the visible background AND for the torus
            // reflection, so the donut mirrors the same floor you can see under
            // it -- which is what finally reads as polished metal.
            vec3 env(vec3 ro, vec3 rd) {
                vec3 horizon = vec3(0.90, 0.95, 1.0);
                vec3 zenith = vec3(0.10, 0.26, 0.70);

                if (rd.y > 0.0) {
                    vec3 col = mix(horizon, zenith, pow(rd.y, 0.5));
                    col += horizon * 0.45 * exp(-rd.y * 11.0); // hot horizon glow

                    float ang = atan(rd.x, rd.z);
                    float strip = smoothstep(0.66, 0.70, sin(ang * 2.0 + 0.7) * 0.5 + 0.5);
                    col += strip * 0.4 * smoothstep(0.05, 0.7, rd.y);

                    vec3 sunDir = normalize(vec3(0.45, 0.5, 0.5));
                    col += vec3(1.0, 0.96, 0.85) * pow(max(dot(rd, sunDir), 0.0), 300.0) * 4.0;
                    return col;
                }

                float td = (FLOOR_Y - ro.y) / rd.y;
                vec3 gp = ro + rd * td;

                vec2 cell = floor(gp.xz);
                float checker = mod(cell.x + cell.y, 2.0);
                vec3 floorCol = mix(vec3(0.02, 0.03, 0.05), vec3(0.12, 0.16, 0.24), checker);

                vec2 edge = abs(fract(gp.xz) - 0.5);
                float line = 1.0 - smoothstep(0.0, 0.04, min(edge.x, edge.y));
                floorCol = mix(floorCol, vec3(0.35, 0.5, 0.8), line * 0.6);

                float fog = clamp(1.0 - exp(-td * 0.06), 0.0, 1.0);
                return mix(floorCol, horizon, fog);
            }

            void main() {
                vec2 uv = vPos;
                uv.x *= uResolution.x / uResolution.y;

                // Tumble on three axes with non-constant angular velocity so
                // the donut keeps changing the way it turns instead of spinning
                // flatly in one direction.
                float ax = uTime * 0.45 + 1.0 * sin(uTime * 0.27);
                float ay = uTime * 0.30 + 1.2 * sin(uTime * 0.19 + 1.3);
                float az = uTime * 0.40 + 1.1 * sin(uTime * 0.23);
                gRot = rotZ(az) * rotY(ay) * rotX(ax);

                vec3 ro = vec3(0.0, 0.55, 4.0);
                vec3 rd = normalize(vec3(uv, -2.2));

                float t = 0.0;
                float d = 0.0;
                bool hit = false;
                for (int i = 0; i < 90; i++) {
                    vec3 pos = ro + rd * t;
                    d = map(pos);
                    if (d < 0.001) { hit = true; break; }
                    t += d;
                    if (t > 12.0) break;
                }

                vec3 color;
                if (hit) {
                    vec3 pos = ro + rd * t;
                    vec3 n = calcNormal(pos);
                    vec3 refl = reflect(rd, n);

                    vec3 chrome = env(pos, refl);

                    // Fresnel makes grazing angles brighter, like real metal.
                    float fresnel = pow(1.0 - max(dot(n, -rd), 0.0), 3.0);
                    chrome = mix(chrome, vec3(1.0), fresnel * 0.75);

                    // Tight specular glint.
                    vec3 lightDir = normalize(vec3(0.6, 0.7, 0.4));
                    float spec = pow(max(dot(refl, lightDir), 0.0), 48.0);
                    chrome += vec3(1.0) * spec;

                    // Faint blue metallic tint.
                    color = chrome * vec3(0.92, 0.96, 1.0);
                } else {
                    // Background: the same world (checker floor + sky) the torus
                    // reflects, so the donut visibly sits on a reflective floor.
                    color = env(ro, rd);
                }

                color = pow(color, vec3(0.4545)); // gamma
                gl_FragColor = vec4(color * uFade, 1.0);
            }
        """
    }
}
