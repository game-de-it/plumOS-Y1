#!/bin/sh
# Run from a local plumOS-Y1 checkout; never touch a connected device.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/../.." && pwd)
cd "$ROOT"
IMAGE=plumos-y1-libvlc:ndk21
CONTAINER="${LIBVLC_BUILD_CONTAINER:-plumos-y1-libvlc-audio}"
VOLUME="${LIBVLC_BUILD_VOLUME:-plumos-y1-libvlc-lgpl-work}"
mkdir -p build/libvlc-lgpl
NDK_ZIP=build/libvlc-lgpl/android-ndk-r21e-linux-x86_64.zip
if [ ! -f "$NDK_ZIP" ]; then
 curl -fL --retry 3 -o "$NDK_ZIP" https://dl.google.com/android/repository/android-ndk-r21e-linux-x86_64.zip
fi
echo "ad7ce5467e18d40050dc51b8e7affc3e635c85bd8c59be62de32352328ed467e  $NDK_ZIP" | shasum -a 256 -c -
if docker container inspect "$CONTAINER" >/dev/null 2>&1; then
 echo "Container already exists: $CONTAINER. Choose a fresh LIBVLC_BUILD_CONTAINER and LIBVLC_BUILD_VOLUME for a clean build." >&2
 exit 1
fi
docker build --platform linux/amd64 -t "$IMAGE" tools/libvlc
docker run --name "$CONTAINER" --platform linux/amd64 \
 --mount "type=volume,source=$VOLUME,target=/work" \
 --mount "type=bind,source=$ROOT,target=/workspace" \
 "$IMAGE" sh -c '/workspace/tools/libvlc/build-in-container.sh > /workspace/build/libvlc-lgpl/native-build.log 2>&1'
python3 tools/libvlc/export_build.py --container "$CONTAINER"
