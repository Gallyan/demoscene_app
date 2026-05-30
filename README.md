# DemoScene

Une petite app Android qui démarre directement sur une démo old-school, dans
l'esprit de la demoscene. Pas de menu, pas d'UI : ça se lance, ça joue.

## Effets

Les parties s'enchaînent automatiquement sur une timeline (fondus au noir entre
chaque), et un **tap** sur l'écran saute à la suivante. Dans l'ordre :

1. **Plasma** — somme de champs sinusoïdaux poussée dans une palette qui cycle.
2. **Rotozoom** — damier infini qui tourne et zoome, avec cycling de couleurs.
3. **Tunnel** — couloir texturé infini, légèrement tordu, avec fog au centre.
4. **Chrome Torus** — donut 3D chromé *raymarché* (SDF de tore) posé sur un sol
   damier réfléchissant, fresnel et spéculaire ; il culbute sur 3 axes.
5. **Starfield** — champ d'étoiles 3D qui foncent vers la caméra.
6. **Mandelbrot** — zoom continu dans la fractale avec palette qui cycle.
7. **Scroller** — scroll sinusoïdal façon logon-screen au-dessus de copper bars.
   Le texte est cuit une fois dans une texture puis ondulé par le shader.

## Stack

- Kotlin + OpenGL ES 2.0 (`GLSurfaceView`), plein écran immersif.
- Aucune dépendance externe : tout est dans le framework Android.
- AGP 8.5.2 / Gradle 8.7, `minSdk 26`, `compileSdk 34`.

## Build & run

Ouvrir le dossier dans Android Studio et lancer (Run ▶) sur un appareil ou un
émulateur. En ligne de commande :

```bash
./gradlew assembleDebug      # APK : app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # installe sur l'appareil connecté
```

## Architecture

- `MainActivity` — plein écran immersif, garde l'écran allumé.
- `DemoSurfaceView` — surface GL ES 2.0, relaie les taps.
- `DemoRenderer` — la timeline : enchaîne les `Effect`, gère fondus et tap.
- `Effect` — le contrat (`onSurfaceCreated` / `onResize` / `render`).
- `FragmentEffect` — classe de base pour les effets « plein écran » : gère le
  quad et les uniforms standard (`uTime`, `uResolution`, `uFade`), avec des
  hooks pour les uniforms/ressources supplémentaires (textures…).
- `effects/` — les 7 effets ci-dessus.

## Ajouter un effet

La plupart des effets demoscene tiennent dans un fragment shader plein écran.
Le plus souvent il suffit d'étendre `FragmentEffect`, de fournir le shader, puis
d'ajouter la classe à la liste dans `DemoRenderer` :

```kotlin
class MonEffet : FragmentEffect("MonEffet", durationSeconds) {
    override fun fragmentSource() = FRAGMENT
    companion object { private const val FRAGMENT = """ ... """ }
}
```

Le shader reçoit `uTime` (secondes depuis le début de la partie), `uResolution`
et `uFade` (0..1 pour les fondus). Pour des uniforms ou ressources en plus
(texture, etc.), surcharger `onProgramReady` / `onRender` / `onDispose` — voir
`ScrollerEffect`.
