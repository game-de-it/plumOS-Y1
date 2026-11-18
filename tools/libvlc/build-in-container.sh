#!/bin/sh
set -eu
export ANDROID_NDK=/work/android-ndk-r21e
export ANDROID_SDK=/usr/lib/jvm/java-17-openjdk-amd64
export MAKEFLAGS=-j6
if [ ! -d "$ANDROID_NDK" ]; then
 echo "ad7ce5467e18d40050dc51b8e7affc3e635c85bd8c59be62de32352328ed467e  /workspace/build/libvlc-lgpl/android-ndk-r21e-linux-x86_64.zip" | sha256sum -c -
 unzip -q /workspace/build/libvlc-lgpl/android-ndk-r21e-linux-x86_64.zip -d /work
fi
cd /work
JNI=libvlcjni-2fca0d2b5eb1d94e2c41e23408e8087358cb983a
if [ ! -d "$JNI" ]; then
 tar -xzf /workspace/third_party/libvlc/upstream/libvlcjni-2fca0d2b5eb1d94e2c41e23408e8087358cb983a.tar.gz
 mkdir "$JNI/vlc"
 tar -xzf /workspace/third_party/libvlc/upstream/vlc-4c608da16c33560821be7e5c1ad4e34bf5a65398.tar.gz -C "$JNI/vlc" --strip-components=1
 cd "$JNI/vlc"
 git init -q
 git -c user.name=Build -c user.email=build@localhost add .
 git -c user.name=Build -c user.email=build@localhost commit -qm 'Pinned upstream VLC source archive'
 git -c user.name=Build -c user.email=build@localhost am --message-id ../libvlc/patches/*.patch
 cd /work
fi
python3 /workspace/tools/libvlc/configure_audio_build.py "$JNI"
cd "$JNI"
./buildsystem/compile-libvlc.sh -a armeabi-v7a --release --license a
