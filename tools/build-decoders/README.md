# AFinity native decoder build

Builds AFinity's media3 **core** + **FFmpeg/AV1** (and later IAMF/MPEG-H) decoder
AARs from one pinned `androidx/media` checkout, plus the prebuilt FFmpeg/dav1d
native libs, and drops the AARs into `app/libs/`.

This is what gives the **ExoPlayer** backend full software-decode coverage so it
plays virtually any codec (the same approach Just Player uses).

## The one hard rule

`media3` core, the decoder extensions, and every Maven `media3-*` satellite in
`gradle/libs.versions.toml` **must all be the same version**. `build-all.sh`
reads the `media3 = "x.y.z"` value from that file and checks out `androidx/media`
at the matching tag automatically. **Every media3 bump → re-run this build.**

## Requirements

Only **Docker** is needed on the host. The image installs the NDK (r27c, for
16 KB page alignment), `nasm`/`yasm`, `meson`/`ninja`, JDK 17, and the rest.

## Run

```bash
# from the repo root
docker build -t afinity-decoders tools/build-decoders
docker run --rm -v "$PWD":/work afinity-decoders bash tools/build-decoders/build-all.sh
```

Outputs (committed to git):
- `app/libs/lib-exoplayer-release.aar`
- `app/libs/lib-decoder-ffmpeg-release.aar`
- (Phase 2) `app/libs/lib-decoder-iamf-release.aar`, `lib-decoder-mpegh-release.aar`

Build intermediates (`src/`, `out/`, `.android-sdk/`) are git-ignored.

## Scripts

| File | Purpose |
| --- | --- |
| `Dockerfile` | self-contained build image (NDK r27c + toolchains) |
| `build-dav1d.sh` | cross-compile dav1d (AV1) static libs, 4 ABIs, 16 KB-aligned |
| `build-ffmpeg.sh` | cross-compile FFmpeg shared libs with full decoder set + dav1d |
| `build-all.sh` | clone media tag → dav1d → ffmpeg → stage → patch → build AARs → copy |
| `patches/` | the codec allow-list patch (see `patches/README.md`) |

## ABIs

All four: `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`. (32-bit AV1 software decode
is slow — fallback only.)

## Test notes

Record codec-matrix and APK-size results here after device verification
(Tasks 12 / 16 of the implementation plan).

- Codec matrix: _pending first build_
- APK size delta: _pending first build_
