# FFmpeg codec allow-list patch

`ffmpeg-codec-allowlist.patch` is a `git apply`-able diff against the
`androidx/media` checkout (at the pinned `media3` tag) that makes the FFmpeg
**video** renderer advertise every decoder we compiled into `libavcodec`.

## Why it's needed

`./configure --enable-decoder=...` compiles the decoders into the FFmpeg
shared libs, but the media3 extension still gates which formats it will *claim*
in two places:

1. `libraries/decoder_ffmpeg/src/main/java/androidx/media3/decoder/ffmpeg/FfmpegLibrary.java`
   — `getSupportedCodecs()` / `supportsFormat()` returns a fixed MIME list.
2. `libraries/decoder_ffmpeg/src/main/jni/ffmpeg_jni.cc` (name may vary by tag)
   — the `getCodecName(...)` switch maps Android MIME types to FFmpeg decoder
   names. Unmapped MIME types are rejected before decode.

Compiling a decoder without adding it to BOTH lists = the codec is present but
never selected.

## How to (re)generate it

This is tag-specific, so generate it against the actual checkout (done as part
of the first `build-all.sh` run):

```bash
MEDIA=tools/build-decoders/src/media          # populated by build-all.sh
# 1. Edit the two files above to add the video MIME <-> ffmpeg-codec mappings:
#      VIDEO_MPEG2 -> "mpeg2video", VIDEO_VC1 -> "vc1", VIDEO_MP4V_ES -> "mpeg4",
#      VIDEO_VP8 -> "vp8", VIDEO_VP9 -> "vp9", VIDEO_H264 -> "h264",
#      VIDEO_H265 -> "hevc", VIDEO_AV1 -> "libdav1d", VIDEO_MJPEG -> "mjpeg"
#    (mirror NextLib's NextRenderersFactory / ffmpeg patch for exact symbol names)
# 2. Capture the diff:
git -C "$MEDIA" diff > tools/build-decoders/patches/ffmpeg-codec-allowlist.patch
```

Reference implementation: NextLib (`io.github.anilbeesetti:nextlib-media3ext`)
applies the equivalent patch — its `ffmpeg` module is the canonical example of
exposing FFmpeg video decoders through a media3 `FfmpegVideoRenderer`.
