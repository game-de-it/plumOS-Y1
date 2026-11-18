#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname "$0")/../../.." && pwd)
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT="$SDK/build-tools/35.0.1"
OUT="$ROOT/build/libvlc-lgpl/instrumentation"
mkdir -p "$OUT/classes" "$OUT/dex"
"$BT/aapt2" link --manifest "$ROOT/tools/libvlc/test/AndroidManifest.xml" -I "$SDK/platforms/android-34/android.jar" --min-sdk-version 17 --target-sdk-version 17 -o "$OUT/base.apk"
javac -source 8 -target 8 -classpath "$SDK/platforms/android-34/android.jar" -d "$OUT/classes" "$ROOT/tools/libvlc/test/AudioRegression.java"
"$BT/d8" --min-api 17 --lib "$SDK/platforms/android-34/android.jar" --output "$OUT/dex" "$OUT"/classes/com/kroot/y1themes/audiotest/*.class
cp "$OUT/base.apk" "$OUT/test.apk"
(cd "$OUT/dex" && zip -q -u ../test.apk classes.dex)
"$BT/zipalign" -f 4 "$OUT/test.apk" "$OUT/aligned.apk"
: "${SIGNING_KEYSTORE:?Use the same key as the player}"
: "${SIGNING_STORE_PASSWORD:?Set signing password}"
export SIGNING_STORE_PASSWORD
"$BT/apksigner" sign --ks "$SIGNING_KEYSTORE" --ks-key-alias "${SIGNING_ALIAS:-y1theme}" --ks-pass env:SIGNING_STORE_PASSWORD --out "$OUT/audio-test.apk" "$OUT/aligned.apk"
