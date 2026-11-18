# LibVLC 3.7.1

> From 0.10.22, the player uses the [audio source build](../../docs/en/libvlc-audio-build.md). The Maven native-binary description and GPL findings below document 0.10.21 and earlier.

[日本語](README_JA.md)

The player uses VideoLAN's unmodified `libvlc-all:3.7.1` Android AAR and its ARMv7 libraries. Extract `classes.jar` and `jni/armeabi-v7a/*.so` into `extracted/` before building.

- Artifact: https://repo.maven.apache.org/maven2/org/videolan/android/libvlc-all/3.7.1/libvlc-all-3.7.1.aar
- SHA-256: `bc1e14d4344c90e4931ad524671024e9605d599d8ca22a894f709e3c0887fb48`
- POM/bindings license metadata: LGPL 2.1; see [COPYING.LIB](COPYING.LIB). **The shipped native binary contains GPL implementations; do not describe the combination as LGPL-only.**
- Source collection and remaining native-source checks: [SOURCES.md](SOURCES.md).

Android artifact 3.7.1 bundles a native VLC core identifying as 3.0.24-beta1. These version numbers describe different layers.
