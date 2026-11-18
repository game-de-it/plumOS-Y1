# User Guide

[日本語](../ja/user-guide.md) | [Home](../../README.md)

## Installation and recovery

Use only the **Y1 Type A** package. The current candidate is `plumOS-Y1-v0.10.24-TypeA.zip`; download it from [GitHub Releases](https://github.com/game-de-it/plumOS-Y1/releases/tag/v0.10.24). Back up music and settings before flashing. Keep the matching original Type A firmware from the [manufacturer support page](https://www.innioasis.com/pages/support-details?type=y-1) for recovery. A failed or interrupted flash can prevent booting.

1. Charge the player. Close other flashing utilities, switch Y1 off, and disconnect USB.
2. In the compatible Innioasis Updater, select Y1 / Type A and its installation-from-file option, then select the package ZIP.
3. Start installation and connect the powered-off player when prompted.
4. Leave the cable connected until the updater reports completion. Disconnect, then power on.
5. If Android asks for a Home app, select Y1 Theme Player and choose Always.

The logo candidate selects only `LOGO` and `ANDROID` in its scatter file. `PRELOADER`, `BOOTIMG`, and `USRDATA` must not be enabled for this procedure. This is not a guarantee against data loss or behavior differences between updater versions.

If recovery is needed, the original Type A firmware can be written with a compatible Windows SP Flash Tool using **Download Only** and the original package's instructions. Do not use Format All or substitute a Type B/Y2 image. Recovery with original firmware has been demonstrated on a Y1 Type A; recognition and drivers depend on the host computer.

## Controls

The Y1 has a rotating wheel, a center button, and four clickable ring directions. There is no touchscreen or separate volume key.

| Control | Action |
| --- | --- |
| Rotate wheel | Move selection; select an action on the playback screen |
| Center | Confirm/open/execute |
| Upper ring / MENU | Back to the previous screen |
| Lower ring / play-pause | Toggle playback |
| Left/right ring, short press during playback | Previous/next track |
| Left/right ring, hold on playback screen | Rewind/fast-forward, approximately 10 seconds per second held |

On the playback screen, select the play/pause, volume down, volume up, EQ, or playback-order icon with the wheel, then press Center. Ring controls may have different meanings inside Android or FM applications. Android settings can be operated with wheel selection and Center without a connected computer.

## Library and music transfer

Put music in `Music` on the exported music disk (`music` is also accepted). Device-side storage is normally `/storage/sdcard0/Music`; this is a Y1 path, not a computer-side folder. Albums and Songs browse the indexed library. Directories starts at Music and lets you enter subfolders. Playing a file there queues **only files directly inside that directory**, excluding its subdirectories.

1. Connect USB and open Settings → USB file transfer on Y1.
2. Start USB storage. Playback stops while the disk is shared.
3. Open the disk in Finder or Windows Explorer and copy music into Music. The volume name varies; no MTP application is required.
4. Eject the disk on the computer, then end USB storage on Y1.
5. Wait for the scan indication to finish before checking newly added music.

A normal USB connection does not automatically open the stock storage screen. ADB is enabled for development; connect only to a trusted computer. If Finder does not show a mounted disk, check its sidebar settings or relaunch Finder. If music storage does not return after ending sharing, safely eject first, reconnect the cable, and retry the library scan.

The scanner recognizes MP3, M4A, AAC, FLAC, OGG, WAV, AIF/AIFF/AIFC, WMA, APE, OPUS, WV, MKA, ALAC, and CAF extensions. Playback depends on the actual codec/container and the Android or LibVLC decoder; extension recognition does not guarantee every file variant will play.

Select filename or tag-title display in Settings. Missing title tags fall back to the full filename on the playback screen. Long text scrolls. Japanese and unsupported primary-font characters use fallback fonts.

## Update the player without ADB

For 0.10.24, first apply the full firmware ZIP, including when upgrading an earlier development firmware. An APK-only update does not replace the native-library copies in the system partition. Later player-only updates use the procedure below.


After installing the full firmware, later player-only updates can be installed from the device:

1. Start USB storage and copy `plumOS-Y1-v0.10.24-player.apk` into an `Updates` folder at the root of the music disk, alongside Music. Create the folder if needed.
2. Eject the disk on the computer, then end USB storage on Y1.
3. Open Settings → Update player. If Updates exists it opens first; other folders can also be browsed.
4. Select the APK with the wheel and Center. Check the filename and current/new version, then choose Install.
5. Wait on the update-progress screen without switching off. The player restarts after installation; app settings are retained.

The selected file is copied into internal storage and checked before confirmation. The system installation API explicitly selects internal app storage; the vendor storage-selection dialog is not used. Only this player's package with a matching signing certificate is accepted; older version codes are rejected. The same version can be reinstalled. This is an in-place update that retains app settings and themes. It updates the app, not the firmware, boot logo, or wheel mapping. Do not uninstall the app first. ADB and a network connection are not needed for this route. The APK size limit is 256 MiB; internal free space is also required.

## Scanning and cache

Cached metadata appears first. A background scan still walks the music files, but only new files or files whose size or modification time changed need metadata extraction. Deleted files are removed from the index after a completed scan. An unavailable music volume does not intentionally erase the saved cache. Wheel navigation remains available during scanning.

- Music rescan: check changes and reuse cached metadata.
- Clear/rebuild cache: delete the metadata cache and read tags again. Music files, themes, and EQ settings are not deleted.

The first scan or a rebuild takes longer. If tags change without changing either file size or modification time, use Clear/rebuild cache. Directory size and storage speed affect scan time.

## Audio and settings

Playback order offers sequential play, repeat all, repeat one, and shuffle. The ten-band EQ includes presets and a custom curve. In custom EQ, select a band, press Center to edit, and rotate to change it in 1 dB steps. EQ settings persist. EQ does not automatically reduce gain for boosted bands; strong boosts can clip.

Normal volume is 0–13. Levels `14/13`, `15/13`, and `16/13` appear in red and request 110%, 120%, and 130% software volume. Peaks may distort. Boost is not retained across app restarts and is cleared when the output route changes.

Settings includes shutdown with confirmation; shutdown timer OFF/10/20/30/60/90/120 minutes; display timeout; click sound; vibration; brightness; Bluetooth settings; FM radio; Android settings; date/time and 24-hour display; disk capacity; English/Japanese; airplane mode; USB transfer; and library/cache actions. App language does not translate music tags or change the Android system language.

Bluetooth opens Android's pairing and connection screen. If a device is paired but silent, confirm the media-audio connection there. FM opens the built-in MediaTek radio application; turn airplane mode off and connect wired earphones as its antenna.


### Settings order

Related items are adjacent: filename/tag-title display and library maintenance; click sound and vibration; brightness, timeout, and language; USB transfer, player update, capacity, Bluetooth, airplane mode, and FM; date/time, Android settings, shutdown timer, and shutdown. Existing setting values are retained when updating.

## Known limitations

- The stock frontend is disabled, and its non-music functions are not reproduced.
- System-app privileges supplied by the firmware are required for USB switching, shutdown, and some device settings.
- Sporadic pops have been heard on speaker and wired earphones at varying playback positions; they have not been reported during the same Bluetooth listening tests. The cause is not confirmed.
- Successful playback tests do not establish bit-perfect output, high-resolution DAC operation, or compatibility with every codec/headset.

When reporting a problem, include the app/FW version, Type A hardware, output route, file format, steps to reproduce, and whether a rescan or reboot changes the result. Remove personal paths, serial numbers, filenames, and artwork from public logs/screenshots.
