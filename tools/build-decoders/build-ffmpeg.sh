#!/usr/bin/env bash
# Cross-compile FFmpeg as SHARED libs for all four Android ABIs, with the full
# decoder set AFinity needs plus AV1 via dav1d. Output:
#   tools/build-decoders/out/ffmpeg/<abi>/{lib/*.so,include}
# build-all.sh stages these into the media3 decoder_ffmpeg JNI tree.
#
# NOTE: decoder names are validated by `configure` itself — it fails loudly on
# an unknown --enable-decoder. If FFmpeg renames a decoder between versions,
# the build stops here with a clear error (not a silent gap).
set -euo pipefail

FFMPEG_VERSION="${FFMPEG_VERSION:-n7.1}"
API="${API:-35}"
ABIS=(arm64-v8a armeabi-v7a x86 x86_64)

declare -A TRIPLE=(
  [arm64-v8a]=aarch64-linux-android
  [armeabi-v7a]=armv7a-linux-androideabi
  [x86]=i686-linux-android
  [x86_64]=x86_64-linux-android
)
declare -A ARCH=( [arm64-v8a]=aarch64 [armeabi-v7a]=arm [x86]=x86 [x86_64]=x86_64 )
declare -A CPREFIX=(
  [arm64-v8a]=aarch64-linux-android
  [armeabi-v7a]=arm-linux-androideabi
  [x86]=i686-linux-android
  [x86_64]=x86_64-linux-android
)

# Decoders to expose. Video first, then audio, then bitmap/text subtitle decoders.
VIDEO_DECODERS="h264 hevc mpeg2video mpeg4 msmpeg4v1 msmpeg4v2 msmpeg4v3 vc1 wmv3 vp8 vp9 av1 mjpeg theora"
AUDIO_DECODERS="aac aac_latm ac3 eac3 dca truehd mlp flac alac vorbis opus mp1 mp2 mp3 wmav1 wmav2 wmapro pcm_s16le pcm_s24le pcm_s32le pcm_u8 pcm_mulaw pcm_alaw"
SUB_DECODERS="ass ssa srt subrip webvtt dvbsub dvdsub pgssub xsub"

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$ROOT/tools/build-decoders/out/ffmpeg"
SRC="$ROOT/tools/build-decoders/src/ffmpeg"
DAV1D_OUT="$ROOT/tools/build-decoders/out/dav1d"

: "${ANDROID_NDK_HOME:?ANDROID_NDK_HOME must be set (run inside the Docker image)}"
TC="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"

[ -d "$SRC/.git" ] || git clone --depth 1 -b "$FFMPEG_VERSION" \
  https://github.com/FFmpeg/FFmpeg.git "$SRC"

enable_flags() { for d in $1; do printf -- "--enable-decoder=%s " "$d"; done; }
parser_flags() { printf -- "--enable-parser=h264 --enable-parser=hevc --enable-parser=mpegaudio --enable-parser=aac --enable-parser=ac3 --enable-parser=vp9 --enable-parser=av1 "; }

for abi in "${ABIS[@]}"; do
  t="${TRIPLE[$abi]}"
  prefix="$OUT/$abi"
  dav1d_pc="$DAV1D_OUT/$abi/lib/pkgconfig"
  [ -d "$dav1d_pc" ] || { echo "dav1d for $abi missing — run build-dav1d.sh first"; exit 1; }

  ( cd "$SRC" && make distclean >/dev/null 2>&1 || true
    PKG_CONFIG_LIBDIR="$dav1d_pc" \
    ./configure \
      --prefix="$prefix" \
      --libdir="$prefix/lib" \
      --target-os=android \
      --arch="${ARCH[$abi]}" \
      --enable-cross-compile \
      --cross-prefix="$TC/bin/llvm-" \
      --cc="$TC/bin/${t}${API}-clang" \
      --cxx="$TC/bin/${t}${API}-clang++" \
      --ar="$TC/bin/llvm-ar" \
      --ranlib="$TC/bin/llvm-ranlib" \
      --strip="$TC/bin/llvm-strip" \
      --nm="$TC/bin/llvm-nm" \
      --sysroot="$TC/sysroot" \
      --enable-shared --disable-static \
      --disable-programs --disable-doc --disable-avdevice --disable-postproc \
      --disable-everything \
      --enable-avformat --enable-avcodec --enable-swresample --enable-avutil \
      --enable-libdav1d \
      --enable-decoder=libdav1d \
      $(enable_flags "$VIDEO_DECODERS") \
      $(enable_flags "$AUDIO_DECODERS") \
      $(enable_flags "$SUB_DECODERS") \
      $(parser_flags) \
      --enable-demuxer=mov,matroska,avi,mpegts,mpegps,flv,ogg,wav,flac,mp3,aac \
      --enable-protocol=file,pipe \
      --extra-cflags="-O2 -fPIC" \
      --extra-ldflags="-Wl,-z,max-page-size=16384" \
      --extra-libs="-ldl"
    make -j"$(nproc)"
    make install
  )
  echo "ffmpeg built: $prefix/lib"
done

echo "ffmpeg: all ABIs done."
