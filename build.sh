#!/bin/sh
set -eu
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT="$SDK/build-tools/35.0.1"
PLATFORM="$SDK/platforms/android-34/android.jar"
VERSION=$(python3 -c 'import xml.etree.ElementTree as E; print(E.parse("AndroidManifest.xml").getroot().attrib["{http://schemas.android.com/apk/res/android}versionName"])')
VLC_INPUT="${LIBVLC_DIR:-third_party/libvlc/audio/extracted}"
VLC_JAR="$VLC_INPUT/classes.jar"
VLC_JNI="$VLC_INPUT/jni/armeabi-v7a"
[ -f "$VLC_JAR" ] || { echo "LibVLC AAR を展開してください" >&2; exit 1; }
rm -rf build/classes
mkdir -p build/classes build/dex dist
rm -f build/base.apk build/unaligned.apk build/aligned.apk build/dex/classes.dex
"$BT/aapt2" link --manifest AndroidManifest.xml -I "$PLATFORM" --min-sdk-version 17 --target-sdk-version 17 -o build/base.apk
javac -source 8 -target 8 -classpath "$PLATFORM:$VLC_JAR" -d build/classes src/com/kroot/y1themes/*.java
"$BT/d8" --min-api 17 --lib "$PLATFORM" --output build/dex build/classes/com/kroot/y1themes/*.class "$VLC_JAR"
cp build/base.apk build/unaligned.apk
(cd build/dex && zip -q -u ../unaligned.apk classes.dex)
zip -q -u build/unaligned.apk assets/themes/*.json
zip -q -u build/unaligned.apk assets/skins/winamp/*.png
zip -q -u build/unaligned.apk assets/fonts/chicago/* assets/fonts/source-sans/* assets/fonts/heros/*
zip -q -u build/unaligned.apk assets/fonts/*.bin assets/fonts/LICENSE_MPLUS_BITMAP.txt assets/fonts/NOTICE.txt
if [ -d assets/licenses/libvlc ]; then zip -q -r build/unaligned.apk assets/licenses/libvlc; fi
rm -rf build/package/lib/armeabi-v7a
mkdir -p build/package/lib/armeabi-v7a
cp "$VLC_JNI"/*.so build/package/lib/armeabi-v7a/
(cd build/package && zip -q -r ../unaligned.apk lib)
"$BT/zipalign" -f 4 build/unaligned.apk build/aligned.apk
if [ -n "${SIGNING_KEYSTORE:-}" ]; then
  : "${SIGNING_STORE_PASSWORD:?Set SIGNING_STORE_PASSWORD for the private signing key}"
  SIGNING_KEY_PASSWORD="${SIGNING_KEY_PASSWORD:-$SIGNING_STORE_PASSWORD}"
  SIGNING_ALIAS="${SIGNING_ALIAS:-y1theme}"
else
  SIGNING_KEYSTORE="build/debug.keystore"
  SIGNING_STORE_PASSWORD="android"
  SIGNING_KEY_PASSWORD="android"
  SIGNING_ALIAS="y1theme"
  if [ ! -f "$SIGNING_KEYSTORE" ]; then
    keytool -genkeypair -noprompt -keystore "$SIGNING_KEYSTORE" -storepass android -keypass android -alias "$SIGNING_ALIAS" -keyalg RSA -keysize 2048 -validity 3650 -dname 'CN=Y1 Theme Player Debug,O=Local Development' >/dev/null 2>&1
  fi
fi
export SIGNING_STORE_PASSWORD SIGNING_KEY_PASSWORD
"$BT/apksigner" sign --ks "$SIGNING_KEYSTORE" --ks-key-alias "$SIGNING_ALIAS" --ks-pass env:SIGNING_STORE_PASSWORD --key-pass env:SIGNING_KEY_PASSWORD --out "dist/Y1ThemePlayer-$VERSION.apk" build/aligned.apk
"$BT/apksigner" verify --min-sdk-version 17 "dist/Y1ThemePlayer-$VERSION.apk"
shasum -a 256 "dist/Y1ThemePlayer-$VERSION.apk"
