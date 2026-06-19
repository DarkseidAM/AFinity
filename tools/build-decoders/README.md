# ExoPlayer software decoders (NextLib, repackaged)

Gives AFinity's **ExoPlayer** backend full software audio **and video** decoding so
it can play files the device can't hardware-decode (10-bit HEVC on weak SoCs, VP8/9,
and the broad audio set: AC3/E-AC3/DTS/DTS-HD/TrueHD/…). MPV remains the default
backend; this brings ExoPlayer much closer to parity.

## How it works

We use [NextLib](https://github.com/anilbeesetti/nextlib)'s `media3ext` module — the
only maintained library with a working media3 `FfmpegVideoRenderer` (media3's own
`ExperimentalFfmpegVideoRenderer` is an unimplemented stub; Just Player and the
jellyfin decoder are FFmpeg-**audio**-only, verified at the JNI-symbol level).

NextLib ships FFmpeg as separate shared libraries (`libavcodec.so`, `libavutil.so`,
`libswresample.so`, `libswscale.so`). Those filenames **collide** with the ones
`libmpv` (the MPV backend) already bundles, and they're different FFmpeg builds, so a
straight dependency breaks `mergeDebugNativeLibs` ("2 files found with path
lib/arm64-v8a/libavcodec.so").

`repack-nextlib.sh` resolves this by renaming NextLib's four FFmpeg `.so` with an
`nx` prefix (`libnxavcodec.so`, …) and patching the `SONAME` / `DT_NEEDED` entries
(via `patchelf`) so `libmedia3ext.so` loads the renamed copies. libmpv keeps its own
`libavcodec.so`; both sets ship side-by-side with no collision.

## Regenerating the AAR

```bash
bash tools/build-decoders/repack-nextlib.sh
```

Produces `app/libs/nextlib-media3ext-nxstatic-<version>.aar`, which `app/build.gradle.kts`
consumes via `implementation(files(...))`. The renderer is wired through
`AfinityRenderersFactory : NextRenderersFactory`.

Bump `NEXTLIB_VERSION` in the script when updating; keep it aligned with the app's
`media3` version (NextLib's version prefix is the media3 version it targets, e.g.
`1.10.0-…` ↔ media3 `1.10.0`).
