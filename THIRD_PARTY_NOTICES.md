# Third-Party Notices


[日本語](THIRD_PARTY_NOTICES_JA.md) | [Home](README.md)

These notices describe the current development tree, not blanket permission to redistribute the firmware. Project-authored code and documentation are covered by the [MIT License](LICENSE).

| Component | Source and terms | Included notice |
| --- | --- | --- |
| Android key layouts | Android Open Source Project, Apache License 2.0 | [License](third_party/android/LICENSE-2.0.txt); original headers retained |
| Chicago Kare | [Duane King](https://github.com/KingDuane/Chicago-Kare), copyright 2024, MIT | [License](assets/fonts/chicago/LICENSE.txt), [notice](assets/fonts/chicago/NOTICE.txt) |
| Source Sans 3 | [Adobe](https://github.com/adobe-fonts/source-sans), SIL OFL 1.1 | [License](assets/fonts/source-sans/LICENSE.txt), [notice](assets/fonts/source-sans/NOTICE.txt) |
| TeX Gyre Heros | [GUST](https://www.gust.org.pl/projects/e-foundry/tex-gyre/heros), GUST Font License, based on LPPL | [License](assets/fonts/heros/LICENSE.txt), [LPPL](assets/fonts/heros/LPPL-1.3c.txt), [notice](assets/fonts/heros/NOTICE.txt) |
| U-M+ bitmap fonts | Obtained from [Pyxel](https://github.com/kitao/pyxel); retain their M+ bitmap-font terms, not merely Pyxel's code license | [License](assets/fonts/LICENSE_MPLUS_BITMAP.txt), [notice](assets/fonts/NOTICE.txt) |
| LibVLC audio build / Java 3.7.1 | [Source, profile and device validation](docs/en/libvlc-audio-build.md); LGPL 2.1 and dependency-specific terms | [LGPL text](third_party/libvlc/COPYING.LIB), [notices](assets/licenses/libvlc/), [ELF evidence](docs/licensing/native-audio-binaries.json) |

U-M+ BDF sources and their packed runtime forms are retained under `assets/fonts`. Chicago Kare and the color-theme fonts are substitutes or visual reproductions, not Apple font extractions. No material from Apple's web iPod experience was imported.

## Items to resolve before binary publication

- **Stock firmware:** the community [developer guide](https://innioasis.app/developers.html) explicitly supports custom ROM publishing. The exact stock-3.1.2 vendor-component terms and OSS/source inventory still need an evidenced distribution basis; individual manufacturer permission is one route, not the only route. A no-vendor-bytes local assembly is another possible approach.
- **PLUMOSAMP skin:** The development theme uses creator-supplied `PLUMOSAMP-pixel-redesign` regions, composed with a new peach pixel icon. Only required regions are retained; unused sheets, cursors and third-party tutorial text are omitted. Referencing the original design alone is not treated as copied pixels. Provenance and artifact-specific distribution review are recorded in `assets/skins/winamp/NOTICE.txt` and [the distribution review](docs/en/distribution-review.md).
- **LibVLC:** 0.10.22 is rebuilt from source with the old Maven GPL implementations excluded. Matching native source material, configuration and dependency notices are included. Publish the source archive alongside the binary and retain replacement/relink instructions. The independently signed modified-firmware route is documented but has not been separately flashed using a different key.

See the [detailed distribution review and action register](docs/en/distribution-review.md).

Project-provided boot branding and the requested device photo are included as supplied for this release. The MIT license for original code/documentation does not change the terms of third-party material.

Product names identify design references and compatibility. They do not imply endorsement or ownership of third-party trademarks. Music files, unrelated private screenshots, private firmware backups, and signing keys are outside the intended public source package.

Music metadata and cover art visible in the requested Aviator demonstration screenshots retain their respective owners’ rights and are not licensed under the project MIT license.

NDK runtime/compiler and static Android support notices are retained in [NDK-NOTICE.txt](third_party/libvlc/licenses/NDK-NOTICE.txt) and [ANDROID-SUPPORT-NOTICE.txt](third_party/libvlc/licenses/ANDROID-SUPPORT-NOTICE.txt), including their BSD/MIT and runtime-specific terms. These accompany the release as RUNTIME_NOTICES.txt.
