# LibVLC source provenance

> From 0.10.22, the player uses the [audio source build](../../docs/en/libvlc-audio-build.md). The Maven native-binary description and GPL findings below document 0.10.21 and earlier.

[日本語](SOURCES_JA.md)

## Binary license finding

GPL implementation symbols are now confirmed in the shipped libvlc.so. Maven/bindings LGPL metadata does not clear this native combination as LGPL-only. See the [distribution review](../../docs/en/distribution-review.md) and [binary inventory](../../docs/licensing/native-binaries.json). The remaining route is GPL-compliant distribution or a verified LGPL-2.1-only rebuild; complete matching sources/notices/relink materials are still required.

The unmodified Android dependency is `org.videolan.android:libvlc-all:3.7.1`, downloaded from [Maven Central](https://repo.maven.apache.org/maven2/org/videolan/android/libvlc-all/3.7.1/). Its POM identifies the LibVLC JNI project as the source and LGPL 2.1 as the artifact license. The project uses only the ARMv7 native libraries.

Collected material under `upstream/`:

- Maven's Java sources JAR, verified against its published SHA-1, and the POM.
- The [VLC Android release recipe](https://github.com/videolan/vlc-android/tree/c05d7a53209d309b862bf07b04896db11bc10fd6), whose compile script pins LibVLC JNI commit `2fca0d2b5eb1d94e2c41e23408e8087358cb983a`.
- The LibVLC JNI archive at that commit, including JNI code, patches and build recipes.
- The VLC base archive at `4c608da16c33560821be7e5c1ad4e34bf5a65398`, selected by the JNI recipe before applying its `libvlc/patches` series.
- `SOURCE_PROVENANCE.json`, recording upstream pins, binary observations and archive hashes.

The shipped native library reports `3.0.24-beta1 Vetinari` and revision `3.0.23-2-650-g95458d4ad1`. Do not confuse this native-core version with Maven's Android artifact version 3.7.1. Applying JNI patches creates additional commits, so a binary revision string is not by itself a complete correspondence proof.

**Still open before publication:** verify the exact native build configuration and all statically linked dependency/source and license requirements, including the bundled `libc++_shared.so`. The collected archives and upstream recipes are preparation evidence, not a claim that all LGPL distribution obligations have been closed. Keep [COPYING.LIB](COPYING.LIB) and the upstream attribution texts.

To rebuild/relink the player, prepare the LibVLC AAR as documented in the developer guide and replace its `classes.jar` and ARMv7 JNI inputs with the rebuilt versions before building the app. A locally built APK signed with a different key cannot update the installed public player directly. Use a consistently signed firmware/app pair for a modified device; the private project signing key is not distributed.
