# Permission-request drafts — not sent

These drafts are optional, unsent research aids. Firmware community publishing guidance is now recorded; direct vendor contact is not the only possible distribution basis. The VideoLAN draft concerns the old Maven native binary and is historical for 0.10.22, which uses its own audio source build. See the current [action register](distribution-review.md).

[日本語](../ja/permission-requests.md) | [Findings](distribution-review.md)

These are drafts only. No email, form or other message has been sent.

## Innioasis / vendor rights

Subject: Permission and OSS-source information for a modified Y1 Type A firmware

We are preparing a free community music-player firmware, plumOS-Y1, based on original Y1 Type A firmware 3.1.2. We would like to host a modified system image and boot logo on GitHub, retaining vendor software components and their notices. Please confirm whether the appropriate rights holder authorizes modification, repackaging and public redistribution, what attribution/conditions apply, and whether any component is excluded. Please also identify corresponding source releases or valid source-offer arrangements for GPL/LGPL components, including stock native dependencies and the kernel if it is included. Does your answer also cover your suppliers' redistribution rights? We can supply an exact file/partition inventory before distribution.

Use the manufacturer's official contact listed in its [terms](https://www.innioasis.com/policies/terms-of-service). Confirm the replying entity's authority; community hosting alone is not the requested permission.

## Legacy Winamp default-skin artwork

Subject: Rights inquiry for adapting Winamp 2.91 default-skin graphics on a separate player

We are preparing an independent music-player frontend for a handheld device. A local prototype uses PNGs extracted/adapted from the Winamp 2.91 default skin, distributed in Webamp as base-2.91.wsz. We do not seek to redistribute the Winamp application. Who can authorize this artwork, and is copying, extracting, resizing/adapting, embedding it in this non-Winamp application, and distributing source/binary packages and documentation screenshots permitted? Please state attribution, naming/trademark, hosting, commercial/noncommercial and other conditions, or confirm that this use is not licensed. Webamp's MIT code license does not answer our artwork question.

Start with [Winamp's official support](https://support.winamp.com/). Do not assume a Webamp maintainer can grant rights owned by another party.

## VideoLAN artifact/source clarification

Subject: libvlc-all 3.7.1 ARMv7 license/source clarification

We are auditing the Maven artifact org.videolan.android:libvlc-all:3.7.1 (AAR SHA-256 bc1e14d4344c90e4931ad524671024e9605d599d8ca22a894f709e3c0887fb48). The POM lists LGPL 2.1. Its ARMv7 libvlc.so defines dvdcss_open/dvdcss_read and DVDOpen, while the release recipe has a default GPL-enabled license mode. Please clarify the licenses covering the exact shipped native combination, its complete corresponding sources/build configuration and dependency notices, and whether an official LGPL-2.1-only API-17/ARMv7 artifact is available. We are not asking for an exception where the published license already authorizes distribution; we need to identify which conditions apply to this exact build.

Use the official project issue/support channel after authorization to send. This draft has not been submitted.
