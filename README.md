# DemoScene

Une petite app Android qui démarre directement sur une démo old-school, dans
l'esprit de la demoscene. Pas de menu, pas d'UI : ça se lance, ça joue.

## Effets

La démo est **pilotée au tap** : chaque partie fait son fondu d'entrée et reste
à l'écran ; un **tap** (ailleurs que sur l'icône son) passe à la suivante. Les
20 parties, dans l'ordre :

1. **Plasma** — somme de champs sinusoïdaux dans une palette qui cycle.
2. **Rotozoom** — damier infini qui tourne et zoome.
3. **Tunnel** — couloir texturé tordu avec fog au centre.
4. **Interference** — moiré de deux jeux de cercles concentriques.
5. **Twister** — rubans verticaux qui se vrillent en colonne.
6. **Chrome Torus** — donut 3D chromé *raymarché* sur sol damier réfléchissant.
7. **Metaballs** — blobs qui fusionnent (champ scalaire seuillé).
8. **Fire** — feu par bruit fractal montant, palette de flammes.
9. **Water** — ondes circulaires réfractant un damier, avec spéculaire.
10. **Bump** — bump mapping procédural éclairé par une lampe qui tourne.
11. **Glenz** — octaèdre translucide (faces avant + arrière) façon glenz vectors.
12. **DotBall** — sphère de points (bobs) projetée en perspective.
13. **Kefren** — rideau de barres verticales qui ondule.
14. **Shadebobs** — bobs laissant une traînée de phosphore.
15. **Voxel** — paysage de hauteur survolé, raymarché, avec fog.
16. **Starfield** — étoiles 3D fonçant vers la caméra (avec traînées).
17. **Particles** — fontaine de particules balistiques sous gravité.
18. **Feedback** — feedback vidéo simulé (zoom-rotation récursif).
19. **Mandelbrot** — zoom continu dans la fractale, palette qui cycle.
20. **Poulmouslip** — une poulette de l'espace en slip, en 3D raymarchée, qui
    tourne sur un fond étoilé, avec un scroll « ON EST DES POULMOUSLIP !! ».
    Pendant cette partie, une voix robotique façon talkbox (synthétisée par
    formants) scande « poulmouslip » par-dessus la musique.
21. **Scroller** — scroll sinusoïdal sur copper bars (texte cuit en texture).

Une **musique chiptune** synthétisée en code tourne en fond ; une icône
haut-parleur en haut à droite coupe/réactive le son.

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
- `ChiptunePlayer` — synthétise et streame la musique, et mixe la voix quand
  `voiceActive` est vrai (aucun fichier audio).
- `VoiceSynth` — synthèse de parole par formants ("poulmouslip", style talkbox).
- `TextTexture` — cuit une chaîne en texture pour les effets de scroll.
- `effects/` — les 21 effets ci-dessus.

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
