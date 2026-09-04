# Zen FNF Toolkit

An all-in-one Android toolkit for FNF/Psych Engine modding.

## Tools

- **Spritesheet Converter** — pick individual frame PNGs, packs them into one spritesheet
  + an Adobe Animate / Psych Engine style `TextureAtlas` XML.
- **Icon Grid Creator** — pick icon images, packs them into a uniform grid (like Psych's
  icon-grid system) + matching XML, using each file's name as the SubTexture name.
- **GIF to Spritesheet** — pick a GIF, choose how many frames to sample, and it's split
  and packed the same way as the Spritesheet Converter.

Output is saved to `Pictures/ZenFNFToolkit/*.png` and `Downloads/ZenFNFToolkit/*.xml` —
no storage permission needed (uses MediaStore, API 29+).

## Building

Push this repo to GitHub — the included workflow (`.github/workflows/build.yml`) builds
a debug APK automatically on every push to `main` and uploads it as an artifact you can
download from the Actions tab.

To build locally instead (needs Android Studio / SDK + JDK 17):

```
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

## Notes / known limitations

- The spritesheet packer uses simple shelf (row) packing — not maximally space-efficient,
  but predictable and correct.
- GIF frame extraction uses `android.graphics.Movie`, which samples the GIF at evenly
  spaced time points rather than pulling exact GIF frame boundaries. For most FNF-length
  GIFs this looks fine; bump the frame count if motion looks choppy.
- Icon Grid Creator stretches each source image to fill its cell — crop/pad icons to
  square beforehand if you want them undistorted.
