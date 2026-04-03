# ImgEdt

**Open source, GPU-accelerated photo editor for Android**

[Website](https://k1monfared.github.io/imgedt/) | [Download APK](https://github.com/k1monfared/imgedt/releases/latest) | [Changelog](CHANGELOG.md)

**Status**: POC | **Version**: 0.1.0 | **License**: GPL v2 | **Min Android**: 5.0+

---

## What is ImgEdt?

ImgEdt is a standalone Android photo editing application built from scratch with a focus on real-time performance and clean architecture. The image processing pipeline runs entirely on the GPU using OpenGL ES 2.0 fragment shaders, delivering instant visual feedback as you adjust parameters.

The core filter algorithms (color space conversions, tone mapping, noise generation, vignette curves) are ported from [Telegram's open source photo editor](https://github.com/DrKLO/Telegram). Rather than extracting Telegram's tightly coupled codebase, ImgEdt reimplements the GLSL shaders in a clean, modular architecture with zero Telegram dependencies.

The project has two goals:

- Be a useful standalone photo editor that respects user privacy (no ads, no tracking, no network permissions)
- Serve as a reusable Android library (AAR) that other developers can embed in their own apps

---

## Features (v0.1.0)

### Photo Adjustments (GPU-accelerated)

All adjustments run as a single-pass GLSL fragment shader on the GPU. Changes appear instantly as you drag the slider.

| Parameter | Technique |
|-----------|-----------|
| Exposure | Gamma curve power function |
| Contrast | Center-relative linear scaling |
| Saturation | Luminance-weighted desaturation mix |
| Warmth | YUV color shift with luma-dependent curve |
| Highlights | Power curve on bright tones with white target interpolation |
| Shadows | Power curve on dark tones with black target interpolation |
| Sharpen | Laplacian edge enhancement (separate pass) |
| Fade | Polynomial curve that lifts blacks |
| Grain | 3D Perlin noise with luma-dependent density |
| Vignette | Radial distance with sigmoid falloff |

### Crop and Rotate

- 6 aspect ratio presets: Free, 1:1, 4:3, 3:4, 16:9, 9:16
- Free rotation from -90 to +90 degrees with a rotation wheel
- 90-degree rotation (tap 4 times to return to original)
- Horizontal mirror
- Expand mode toggle: allow black areas when rotating, or auto-zoom to fill the frame
- Pinch-to-zoom and pan
- Full reset to original state

### Drawing

- Freehand brush with 8 colors and visible selection indicator
- Adjustable brush size (2px to 100px) and opacity
- Eraser tool
- Undo up to 20 strokes

### App

- Photo picker with Android 13+ READ_MEDIA_IMAGES permission handling
- Save to gallery via MediaStore (JPEG quality 95, full resolution)
- Aspect-ratio-preserving preview

---

## Technical Overview

### Architecture

ImgEdt is a two-module Gradle project:

- **app/** (`com.imgedt.app`): Thin launcher shell. Handles photo picking, permissions, and launches the editor.
- **editor/** (`com.imgedt.editor`): The editor library. Contains all editing logic, UI, shaders, and activities. Designed to eventually be distributed as a standalone AAR.

All UI is built programmatically in Java (no XML layouts). This keeps the codebase compact and makes the rendering pipeline easier to follow.

### Filter Pipeline

The rendering happens on a dedicated `HandlerThread` with its own EGL context:

1. **Source upload**: Bitmap is uploaded as a GL texture
2. **Pass 1 (Sharpen)**: Unsharp mask via neighboring texel sampling in a separate shader program
3. **Pass 2 (Tools)**: A single fragment shader applies all 10 adjustments simultaneously. It performs RGB to HSL, HSV, and YUV conversions, Perlin noise generation for grain, and sigmoid curves for vignette, all in one draw call.
4. **Pass 3 (Display)**: Renders the result to a `TextureView` surface via `eglSwapBuffers`

For saving, the pipeline renders at full bitmap resolution to an offscreen framebuffer, reads pixels back with `glReadPixels`, flips vertically (GL origin is bottom-left), and writes JPEG via `MediaStore`.

### Crop System

- `CropAreaView`: Draws the crop overlay (dimmed surround, rule-of-thirds grid, corner handles) and handles resize touch events with aspect ratio constraints
- `CropView`: Displays the image with `Matrix`-based transformations via `ScaleGestureDetector`. Enforces a content-covers-frame constraint after gesture release.
- `CropRotationWheel`: Horizontal drag-to-rotate control with tick marks and snap-to-zero

### Paint System

- Stamp-based brush rendering on a `Canvas` double buffer (persistent canvas + temporary stroke layer)
- Strokes are committed via Porter-Duff compositing (`SRC_OVER` for brush, `DST_OUT` for eraser)
- Undo via bitmap snapshot stack

### Tech Stack

| | |
|---|---|
| Language | Java |
| Min SDK | API 21 (Android 5.0) |
| Target SDK | API 35 |
| Build | Gradle 8.7, AGP 8.6.1 |
| Rendering | OpenGL ES 2.0 via EGL |
| CI/CD | GitHub Actions |

---

## Roadmap

### Near Term
- Test and fix rendering on physical devices
- Curves editor (RGB channel-level spline control with 200x1 LUT texture)
- Blur tool (radial and linear, using dynamically generated Gaussian blur shaders)
- Enhance / auto-adjust (CDT-based histogram equalization)

### Medium Term
- Text overlays with draggable/rotatable entities and font selection
- Shape tools (rectangle, circle, star, arrow) via SDF fragment shaders
- Neon brush effect (3-channel stamp texture with glow compositing)
- Skin smoothing (multi-pass high-pass filter with mask)

### Long Term
- Package editor module as standalone AAR for library distribution
- F-Droid submission
- Release signing with dedicated keystore
- Sticker support
- Video frame editing

---

## Building

### Requirements
- Android SDK (platform 35, build tools 36.1.0)
- JDK 17 or later

### Build and Install

```bash
git clone https://github.com/k1monfared/imgedt.git
cd imgedt
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Releases

APKs are published on [GitHub Releases](https://github.com/k1monfared/imgedt/releases). GitHub Actions automatically builds and publishes an APK when a version tag (`v*`) is pushed.

For automatic updates on your device, use [Obtainium](https://github.com/ImranR98/Obtainium) pointed at this repository.

---

## Contributing

Contributions are welcome. Here is how to get involved.

### Getting Started

1. Fork the repository
2. Create a feature branch from `master`
3. Make your changes and ensure `./gradlew assembleDebug` passes
4. Submit a pull request with a clear description of what you changed and why

### What to Work On

- Check the [Roadmap](#roadmap) for planned features
- Bug reports and fixes are always appreciated
- UI/UX improvements that bring the editor closer to Telegram's polish level
- Performance optimizations, especially for the GL pipeline on lower-end devices
- New brush types or filter effects

### Code Conventions

- **Java, not Kotlin**, to match the existing codebase
- All UI is built programmatically (no XML layouts)
- New image processing should use GLSL shaders where possible for GPU acceleration
- Follow existing patterns: `FilterShaders` for shader source strings, `FilterRenderer` for the GL pipeline, `FilterParams` for parameter mapping

### Pull Request Guidelines

- One feature or fix per PR
- Include a description of what the change does and how to test it
- If adding a new filter parameter, document the shader math and the slider-to-uniform mapping
- No automated test suite yet, so manual testing on a physical device is expected

---

## Project Structure

```
imgedt/
├── app/
│   └── src/main/java/com/imgedt/app/
│       └── MainActivity.java           # Photo picker, permissions
├── editor/
│   └── src/main/java/com/imgedt/editor/
│       ├── EditorActivity.java         # Main editor with GL preview
│       ├── filter/
│       │   ├── FilterShaders.java      # All GLSL shader source code
│       │   ├── FilterRenderer.java     # OpenGL pipeline (EGL, textures, passes)
│       │   └── FilterParams.java       # Slider-to-uniform conversion
│       ├── crop/
│       │   ├── CropActivity.java       # Crop screen with presets and controls
│       │   ├── CropView.java           # Image pan/zoom/rotate gestures
│       │   ├── CropAreaView.java       # Crop overlay with draggable handles
│       │   ├── CropRotationWheel.java  # Rotation control
│       │   └── CropState.java          # Transform state
│       └── paint/
│           ├── PaintActivity.java      # Drawing screen with tools
│           ├── PaintView.java          # Brush stamp rendering with undo
│           └── BrushShaders.java       # GLSL shaders (for future GL brushes)
├── docs/
│   └── index.html                      # GitHub Pages site
├── .github/workflows/
│   ├── build.yml                       # CI: build on push/PR
│   └── release.yml                     # CD: APK release on version tag
├── CHANGELOG.md
└── LICENSE                             # GNU GPL v2.0
```

---

## License

ImgEdt is licensed under the **GNU General Public License v2.0** (GPL-2.0).

### Why GPL v2?

The core image processing shaders are derived from [Telegram's Android app](https://github.com/DrKLO/Telegram), which is licensed under GPL v2. Under the terms of GPL v2, any derivative work that incorporates GPL-licensed code must itself be distributed under the same license. Since ImgEdt ports Telegram's GLSL shader algorithms (color space conversions, tone curves, noise functions, vignette math), the entire project inherits the GPL v2 obligation.

### What this means for contributors

All contributions to ImgEdt will be licensed under GPL v2. You can freely use, modify, and distribute ImgEdt, but any distributed derivative must also be GPL v2 and include source code.

### What this means for users

You will always have access to the full source code. You can build the app yourself from source at any time. No one can take this project proprietary.

### Embedding the editor library

If you embed the editor library (AAR) in your own app, your app must also be GPL v2 compatible. If you need a permissive license for embedding, the long-term plan is to rewrite the shader algorithms from scratch (without reference to Telegram's code) and relicense those components. This has not happened yet.

---

## Acknowledgments

[Telegram](https://github.com/DrKLO/Telegram): The GLSL filter shaders and image processing algorithms are derived from Telegram's open source Android client. Telegram's photo editor implements a sophisticated multi-pass OpenGL pipeline with color space transformations, tone mapping, and noise generation that ImgEdt ports into a clean standalone architecture.
