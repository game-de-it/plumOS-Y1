#!/bin/sh
# Incremental relink of a completed source build; regenerate the module selection too.
set -eu
export ANDROID_NDK=/work/android-ndk-r21e
export Y1_RELINK_ONLY=1
export MAKEFLAGS=-j6
cd /work/libvlcjni-2fca0d2b5eb1d94e2c41e23408e8087358cb983a
./buildsystem/compile-libvlc.sh -a armeabi-v7a --release --license a
