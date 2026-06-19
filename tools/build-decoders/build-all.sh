#!/usr/bin/env bash
# Orchestrates the full native-decoder build and drops the resulting AARs into app/libs/.
#
# Run inside the Docker image from the repo root:
#   docker run --rm -v "$PWD":/work afinity-decoders bash tools/build-decoders/build-all.sh
#
# The media3 git tag is read from gradle/libs.versions.toml so core+extensions are
# ALWAYS built against the exact version the app links (the one hard rule of this stack).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"

MEDIA3_TAG="$(grep -E '^media3 = ' gradle/libs.versions.toml | sed -E 's/.*"([^"]+)".*/\1/')"
[ -n "$MEDIA3_TAG" ] || { echo "Could not read media3 version from libs.versions.toml"; exit 1; }
echo ">>> Building against media3 $MEDIA3_TAG"

MEDIA="$ROOT/tools/build-decoders/src/media"
LIBS="$ROOT/app/libs"
mkdir -p "$LIBS"

[ -d "$MEDIA/.git" ] || git clone --depth 1 -b "$MEDIA3_TAG" \
  https://github.com/androidx/media.git "$MEDIA"

# 1. Native codecs ----------------------------------------------------------
bash tools/build-decoders/build-dav1d.sh
bash tools/build-decoders/build-ffmpeg.sh

# 2. Stage FFmpeg into the decoder_ffmpeg extension JNI tree -----------------
#    VALIDATE the expected layout against libraries/decoder_ffmpeg/README.md of
#    the checked-out tag; staging path occasionally shifts between releases.
FF_JNI="$MEDIA/libraries/decoder_ffmpeg/src/main/jni"
for abi in arm64-v8a armeabi-v7a x86 x86_64; do
  dst="$FF_JNI/ffmpeg/android-libs/$abi"
  mkdir -p "$dst"
  cp -a "tools/build-decoders/out/ffmpeg/$abi/lib/." "$dst/"
done
mkdir -p "$FF_JNI/ffmpeg/include"
cp -a "tools/build-decoders/out/ffmpeg/arm64-v8a/include/." "$FF_JNI/ffmpeg/include/"

# 3. Patch the codec allow-list so all compiled decoders are advertised ------
#    The media3 FFmpeg VIDEO renderer only claims formats listed in
#    FfmpegLibrary.getSupportedCodecs() / the JNI codec-name map. Without this
#    patch the extra video decoders we compiled are decoded by libavcodec but
#    never SELECTED. See patches/README.md for what the patch must contain.
PATCH="$ROOT/tools/build-decoders/patches/ffmpeg-codec-allowlist.patch"
if [ -f "$PATCH" ]; then
  git -C "$MEDIA" apply --verbose "$PATCH"
  echo "  applied codec allow-list patch"
else
  echo "!!! WARNING: $PATCH missing — software VIDEO decoding will be limited to"
  echo "!!! the extension's default allow-list. Generate the patch (see patches/README.md)"
  echo "!!! against this checkout and re-run before shipping."
fi

# 4. Build the extension native libs + AARs ---------------------------------
"$ANDROID_NDK_HOME/ndk-build" -C "$FF_JNI" \
  APP_ABI="arm64-v8a armeabi-v7a x86 x86_64" APP_PLATFORM="android-35"

GRADLE_TARGETS=(:lib-exoplayer:assembleRelease :lib-decoder-ffmpeg:assembleRelease)
# Phase 2 (uncomment once IAMF/MPEG-H are in scope):
# GRADLE_TARGETS+=(:lib-decoder-iamf:assembleRelease :lib-decoder-mpegh:assembleRelease)
( cd "$MEDIA" && ./gradlew --no-daemon "${GRADLE_TARGETS[@]}" )

# 5. Collect AARs -----------------------------------------------------------
copy_aar() { # <module-dir> <dest-name>
  local found
  found="$(find "$MEDIA/libraries/$1" -path '*/outputs/aar/*-release.aar' | head -1)"
  [ -n "$found" ] || { echo "AAR not found for $1"; exit 1; }
  cp "$found" "$LIBS/$2"
  echo "  -> $LIBS/$2"
}
copy_aar exoplayer     lib-exoplayer-release.aar
copy_aar decoder_ffmpeg lib-decoder-ffmpeg-release.aar
# copy_aar decoder_iamf  lib-decoder-iamf-release.aar
# copy_aar decoder_mpegh lib-decoder-mpegh-release.aar

echo ">>> Done. AARs in app/libs/:"
ls -la "$LIBS"/lib-*.aar
