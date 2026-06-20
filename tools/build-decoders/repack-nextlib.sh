#!/usr/bin/env bash
# Repackages NextLib's media3 FFmpeg-decoder AAR so it can coexist with the MPV
# backend (libmpv) in the same APK.
#
# Problem: NextLib (io.github.anilbeesetti:nextlib-media3ext) ships FFmpeg as
# separate shared libs (libavcodec.so, libavutil.so, libswresample.so,
# libswscale.so) whose filenames collide with the ones libmpv already bundles —
# Android can only package one lib/<abi>/libavcodec.so, and the two are different
# FFmpeg builds, so `mergeDebugNativeLibs` fails.
#
# Fix: rename NextLib's four FFmpeg .so with an "nx" prefix and patch every
# SONAME / DT_NEEDED reference (with patchelf) so libmedia3ext.so loads the
# renamed copies. libmpv keeps its own libavcodec.so; no collision.
#
# Requires: patchelf, jar (JDK), unzip. Run from the repo root:
#   bash tools/build-decoders/repack-nextlib.sh
#
# Output: app/libs/nextlib-media3ext-nxstatic-<version>.aar
set -euo pipefail

NEXTLIB_VERSION="1.10.0-0.12.1"
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$REPO_ROOT/app/libs/nextlib-media3ext-nxstatic-${NEXTLIB_VERSION}.aar"

# Locate the Maven AAR in the Gradle cache (resolve it once via a build if absent).
GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
AAR="$(find "$GRADLE_HOME/caches" -name "nextlib-media3ext-${NEXTLIB_VERSION}.aar" 2>/dev/null | head -1)"
[ -n "$AAR" ] || {
  echo "Maven AAR not in Gradle cache. Temporarily add"
  echo "  implementation(\"io.github.anilbeesetti:nextlib-media3ext:${NEXTLIB_VERSION}\")"
  echo "and run a build once so Gradle downloads it, then re-run this script."
  exit 1
}

WORK="$(mktemp -d)"; trap 'rm -rf "$WORK"' EXIT
unzip -o -q "$AAR" -d "$WORK"

OLDS=(libavutil.so libavcodec.so libswresample.so libswscale.so)
NEWS=(libnxavutil.so libnxavcodec.so libnxswresample.so libnxswscale.so)

for abidir in "$WORK"/jni/*/; do
  pushd "$abidir" >/dev/null
  for i in "${!OLDS[@]}"; do
    if [ -f "${OLDS[$i]}" ]; then
      mv "${OLDS[$i]}" "${NEWS[$i]}"
      patchelf --set-soname "${NEWS[$i]}" "${NEWS[$i]}"
    fi
  done
  for elf in libnxavutil.so libnxavcodec.so libnxswresample.so libnxswscale.so libmedia3ext.so; do
    [ -f "$elf" ] || continue
    for i in "${!OLDS[@]}"; do
      patchelf --replace-needed "${OLDS[$i]}" "${NEWS[$i]}" "$elf" 2>/dev/null || true
    done
  done
  popd >/dev/null
done

mkdir -p "$REPO_ROOT/app/libs"
rm -f "$OUT"
( cd "$WORK" && jar cMf "$OUT" . )
echo "Wrote $OUT"
echo "Renamed libs (arm64-v8a):"; ( cd "$WORK/jni/arm64-v8a" && ls libnx*.so libmedia3ext.so )
