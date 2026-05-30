# DemoScene

Une petite app Android qui démarre directement sur une démo old-school, dans
l'esprit de la demoscene. Pas de menu, pas d'UI : ça se lance, ça joue.

## Effets

Les parties s'enchaînent automatiquement sur une timeline (fondus au noir entre
chaque), et un **tap** sur l'écran saute à la suivante.

1. **Plasma** — le plasma classique : somme de champs sinusoïdaux poussée dans
   une palette qui cycle.
2. **Chrome Torus** — un donut 3D chromé *raymarché* (SDF de tore), avec
   réflexion d'environnement procédurale, fresnel et spéculaire.
3. **Scroller** — un scroll sinusoïdal façon logon-screen, au-dessus de copper
   bars. Le texte est cuit une fois dans une texture puis ondulé par le shader.

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
- `Effect` — un contrat simple (`onSurfaceCreated` / `onResize` / `render`) ;
  ajouter un effet = écrire une classe et l'ajouter à la liste du renderer.
- `effects/` — `PlasmaEffect`, `ChromeTorusEffect`, `ScrollerEffect`.

## Ajouter un effet

Implémenter `Effect`, puis l'ajouter à la liste dans `DemoRenderer`. La plupart
des effets demoscene tiennent dans un fragment shader plein écran (le quad et
les helpers GLSL sont déjà fournis).
