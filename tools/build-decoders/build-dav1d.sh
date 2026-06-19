#!/usr/bin/env bash
# Cross-compile dav1d (AV1 software decoder) for all four Android ABIs.
# Output: tools/build-decoders/out/dav1d/<abi>/{lib,include}
# Consumed by build-ffmpeg.sh via --enable-libdav1d.
set -euo pipefail

DAV1D_VERSION="${DAV1D_VERSION:-1.4.3}"
API="${API:-35}"
ABIS=(arm64-v8a armeabi-v7a x86 x86_64)

# ABI -> (clang triple, meson cpu_family, meson cpu)
declare -A TRIPLE=(
  [arm64-v8a]=aarch64-linux-android
  [armeabi-v7a]=armv7a-linux-androideabi
  [x86]=i686-linux-android
  [x86_64]=x86_64-linux-android
)
declare -A CPU_FAMILY=( [arm64-v8a]=aarch64 [armeabi-v7a]=arm [x86]=x86 [x86_64]=x86_64 )
declare -A CPU=( [arm64-v8a]=aarch64 [armeabi-v7a]=armv7a [x86]=i686 [x86_64]=x86_64 )

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/tools/build-decoders/out/dav1d"
SRC="$ROOT/tools/build-decoders/src/dav1d"

: "${ANDROID_NDK_HOME:?ANDROID_NDK_HOME must be set (run inside the Docker image)}"
TC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"

[ -d "$SRC/.git" ] || git clone --depth 1 -b "$DAV1D_VERSION" \
  https://code.videolan.org/videolan/dav1d.git "$SRC"

for abi in "${ABIS[@]}"; do
  t="${TRIPLE[$abi]}"
  cross="/tmp/dav1d-cross-$abi.txt"
  cat > "$cross" <<EOF
[binaries]
c = '$TC/bin/${t}${API}-clang'
cpp = '$TC/bin/${t}${API}-clang++'
ar = '$TC/bin/llvm-ar'
strip = '$TC/bin/llvm-strip'
pkg-config = 'pkg-config'

[host_machine]
system = 'android'
cpu_family = '${CPU_FAMILY[$abi]}'
cpu = '${CPU[$abi]}'
endian = 'little'
EOF

  rm -rf "$SRC/build-$abi"
  meson setup "$SRC/build-$abi" "$SRC" \
    --cross-file "$cross" \
    --default-library=static \
    --buildtype=release \
    -Denable_tools=false \
    -Denable_tests=false \
    -Dc_args="-fPIC" \
    -Dc_link_args="-Wl,-z,max-page-size=16384" \
    --prefix "$OUT/$abi" \
    --libdir lib
  ninja -C "$SRC/build-$abi"
  ninja -C "$SRC/build-$abi" install
  echo "dav1d built: $OUT/$abi/lib/libdav1d.a"
done

echo "dav1d: all ABIs done."
