# Build environment for AFinity's media3 core + FFmpeg/AV1/IAMF/MPEG-H decoder AARs.
#
# The host does NOT need nasm/yasm/NDK — everything is installed here so the
# cross-compile is fully self-contained and reproducible.
#
# Usage (from repo root):
#   docker build -t afinity-decoders tools/build-decoders
#   docker run --rm -v "$PWD":/work afinity-decoders bash tools/build-decoders/build-all.sh
FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends \
        git curl ca-certificates unzip xz-utils \
        openjdk-17-jdk-headless \
        python3 python3-pip python3-setuptools \
        meson ninja-build nasm yasm pkg-config build-essential cmake file && \
    rm -rf /var/lib/apt/lists/*

# NDK r27c — first NDK series that defaults to 16 KB-aligned shared libraries
# (required for Android 15 / minSdk 35). Pinned for reproducibility.
ENV NDK_ZIP=android-ndk-r27c-linux.zip
RUN curl -fsSL -o /tmp/ndk.zip "https://dl.google.com/android/repository/${NDK_ZIP}" && \
    unzip -q /tmp/ndk.zip -d /opt && \
    rm /tmp/ndk.zip

ENV ANDROID_NDK_HOME=/opt/android-ndk-r27c
ENV PATH="${ANDROID_NDK_HOME}:${PATH}"

# Android SDK is provided by the androidx/media gradle build via the foojay
# toolchain resolver + sdkmanager auto-download; set a writable SDK home.
ENV ANDROID_SDK_ROOT=/work/tools/build-decoders/.android-sdk
ENV ANDROID_HOME=/work/tools/build-decoders/.android-sdk

WORKDIR /work
