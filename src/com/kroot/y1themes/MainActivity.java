package com.kroot.y1themes;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.os.StatFs;
import android.os.Handler;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.provider.Settings;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.ArrayList;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;

public final class MainActivity extends Activity {
    private enum Page { HOME, ALBUMS, SONGS, DIRECTORY, ALBUM_SONGS, THEMES, SETTINGS, LANGUAGE, OPTIONS, BLUETOOTH, STORAGE, USB_TRANSFER, APK_FILES, APK_CONFIRM, APK_INSTALLING, SHUTDOWN, PLAYER, EQ, EQ_BANDS }
    private static final String[] HOME = {"再生中", "アルバム", "曲", "ディレクトリ", "テーマ", "設定"};
    private static final String[] EQ_NAMES = {"オフ", "フラット", "低音", "ボーカル", "明瞭", "温かみ", "カスタム調整"};
    private static final String[] PLAY_MODES = {"順番", "リピート全曲", "リピート1曲", "シャッフル"};
    private enum Setting {
        DISPLAY_FILENAME("ファイル名で表示"), DISPLAY_TITLE("タグの曲名で表示"),
        RESCAN("音楽を再スキャン"), REBUILD_CACHE("キャッシュ削除・再構築"),
        CLICK_SOUND("クリック音"), VIBRATION("バイブ"),
        BRIGHTNESS("画面輝度"), SCREEN_TIMEOUT("画面タイムアウト"), LANGUAGE("言語"),
        USB_TRANSFER("USBファイル転送"), APP_UPDATE("アプリを更新"), STORAGE("ディスク容量"),
        BLUETOOTH("Bluetooth"), AIRPLANE_MODE("エアプレーンモード"), FM_RADIO("FMラジオ"),
        DATE_TIME("日付と時刻"), ANDROID_SETTINGS("Android設定"),
        SHUTDOWN_TIMER("シャットダウンタイマー"), SHUTDOWN("シャットダウン");

        final String label;
        Setting(String label) { this.label = label; }
    }
    private static final Setting[] SETTINGS_ITEMS = Setting.values();
    private static final int[] SHUTDOWN_MINUTES = {0,10,20,30,60,90,120};
    private static final int[] TIMEOUT_SECONDS = {15,30,60,120,300,600,1800};
    private static final int[] BRIGHTNESS_LEVELS = {16,32,64,96,128,160,192,224,255};
    private static final float[][] EQ_CURVES = {
        {0,0,0,0,0,0,0,0,0,0},
        {4,3,2,1,0,0,0,0,0,0},
        {-2,-1,0,1,3,3,2,1,0,0},
        {-2,-1,0,0,1,2,3,3,2,1},
        {2,2,1,1,0,0,-1,-1,-2,-2}
    };
    private Page page = Page.HOME;
    private int selection = 0;
    private String currentAlbum = "";
    private List<Library.Track> tracks = new ArrayList<Library.Track>();
    private final List<File> directoryFolders = new ArrayList<File>();
    private final List<Library.Track> directorySongs = new ArrayList<Library.Track>();
    private File directoryRoot;
    private File currentDirectory;
    private String queueDirectory;
    private List<String> albums = new ArrayList<String>();
    private List<Theme> themes = new ArrayList<Theme>();
    private Theme theme;
    private int playingIndex = -1;
    private String nowPlayingTitle;
    private MediaPlayer player;
    private org.videolan.libvlc.LibVLC vlc;
    private org.videolan.libvlc.MediaPlayer vlcPlayer;
    private int playbackSerial;
    private int boostLevel;
    private long abStartMs = -1, abEndMs = -1;
    private long lastAbSeekMs;
    private final Runnable abLoop = new Runnable() {
        @Override public void run() {
            if (abStartMs < 0 || abEndMs < 0 || screen == null) return;
            long now = SystemClock.uptimeMillis();
            if (isPlaying() && playbackPositionMs() >= abEndMs && now - lastAbSeekMs >= 500) {
                lastAbSeekMs = now;
                try {
                    if (player != null) player.seekTo((int)abStartMs);
                    else if (vlcPlayer != null) vlcPlayer.setTime(abStartMs);
                } catch (Exception ignored) {}
                screen.invalidate();
            }
            screen.postDelayed(this, 100);
        }
    };
    private int listFirst;
    private final BroadcastReceiver outputChange = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (boostLevel == 0) return;
            boostLevel = 0;
            if (vlcPlayer != null) vlcPlayer.setVolume(100);
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (player != null && player.isPlaying()) player.pause();
                if (vlcPlayer != null && vlcPlayer.isPlaying()) vlcPlayer.pause();
            }
            if (screen != null) screen.invalidate();
        }
    };
    private int eqMode;
    private boolean fileNameFirst;
    private boolean english;
    private int playMode;
    private int shutdownMinutes;
    private Setting optionSetting;
    private boolean clickSound, vibrate;
    private android.media.SoundPool cursorSounds;
    private int cursorSoundId;
    private boolean cursorSoundReady;
    private File apkRoot, apkDirectory;
    private final List<File> apkEntries = new ArrayList<File>();
    private boolean apkBusy;
    private int apkRequest;
    private ApkUpdates.Prepared apkPrepared;
    private final java.util.concurrent.ExecutorService apkWorker =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private Object apkInstallObserver;
    private boolean apkInstalling;
    private boolean usbBusy;
    private boolean usbReturnPending;
    private int libraryScanSerial;
    private int emptyScanRetries;
    private final Runnable storageRescan = new Runnable() {
        @Override public void run() { refreshLibrary(); }
    };
    private final Runnable usbReturn = new Runnable() {
        @Override public void run() {
            if (!usbReturnPending || usbBusy || usbStorageActive()) return;
            startActivity(new Intent(MainActivity.this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }
    };
    private final Runnable clearUsbReturn = new Runnable() {
        @Override public void run() { usbReturnPending = false; }
    };
    private final BroadcastReceiver storageEvents = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (screen == null || !Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction())) return;
            screen.removeCallbacks(storageRescan);
            screen.postDelayed(storageRescan, 1000);
            if (usbReturnPending) {
                screen.removeCallbacks(usbReturn);
                screen.postDelayed(usbReturn, 2500);
            }
        }
    };
    private int originalSystemSound = -1;
    private Vibrator motor;
    private final ArrayList<Integer> shuffleHistory = new ArrayList<Integer>();
    private boolean skipHeld;
    private int skipKey;
    private long skipDownTimeMs;
    private long skipStartPositionMs;
    private BluetoothAdapter bluetooth;
    private final ArrayList<BluetoothDevice> paired = new ArrayList<BluetoothDevice>();
    private final ArrayList<BluetoothDevice> nearby = new ArrayList<BluetoothDevice>();
    private BluetoothProfile a2dp;
    private final BroadcastReceiver bluetoothEvents = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !paired.contains(device) && !nearby.contains(device)) nearby.add(device);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) refreshPaired();
            if (screen != null) screen.invalidate();
        }
    };
    private final float[] eqBands = new float[10];
    private boolean editingBand;
    private Bitmap cover;
    private boolean loading = true;
    private boolean directoryLoading;
    private int directoryScanSerial;
    private String displayedDirectory, directoryFocusPath;
    private final java.util.concurrent.ExecutorService directoryWorker =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private String message = "";
    private String observedMessage = "";
    private final Runnable clearTransientMessage = new Runnable() {
        @Override public void run() {
            if (message.equals(observedMessage)) {
                message = "";
                if (screen != null) screen.invalidate();
            }
        }
    };
    private boolean persistentMessage() {
        return message.equals(tr("差分を確認しています")) ||
                message.equals(tr("キャッシュを再構築しています")) ||
                message.equals(tr("切り替え中…")) ||
                message.equals(tr("APKを読み込み中…")) ||
                message.equals(tr("APKを確認しています…")) ||
                message.equals(tr("読み込み中")) ||
                message.equals(tr("検索中…"));
    }
    private String seekMessage = "";
    private final Runnable clearSeekMessage = new Runnable() {
        @Override public void run() {
            if (message.equals(seekMessage)) {
                message = "";
                if (screen != null) screen.invalidate();
            }
        }
    };
    private Screen screen;
    private AudioManager audio;
    private long marqueeStartMs = SystemClock.uptimeMillis();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        DeviceSetupReceiver.apply(this);
        if (getPreferences(0).getBoolean("apkUpdatePending", false)) {
            getPreferences(0).edit().putBoolean("apkUpdatePending", false).commit();
            deleteFile(ApkUpdates.STAGED_NAME);
        }
        cursorSounds = new android.media.SoundPool(2, AudioManager.STREAM_SYSTEM, 0);
        cursorSounds.setOnLoadCompleteListener(new android.media.SoundPool.OnLoadCompleteListener() {
            public void onLoadComplete(android.media.SoundPool pool, int sampleId, int status) {
                cursorSoundReady = status == 0;
            }
        });
        cursorSoundId = cursorSounds.load("/system/media/audio/ui/Effect_Tick.ogg", 1);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        themes = Theme.loadAll(this);
        String saved = getPreferences(0).getString("theme", "gen4");
        if (saved.equals("chicago")) {
            saved = "first";
            getPreferences(0).edit().putString("theme", saved).apply();
        }
        for (Theme t : themes) if (t.id.equals(saved)) theme = t;
        if (theme == null && !themes.isEmpty()) theme = themes.get(0);
        audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        motor = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        fileNameFirst = getPreferences(0).getBoolean("fileNameFirst", true);
        english = UiText.english(this);
        playMode = Math.max(0, Math.min(3, getPreferences(0).getInt("playMode", 0)));
        clickSound = getPreferences(0).getBoolean("clickSound",
                Settings.System.getInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0);
        vibrate = getPreferences(0).getBoolean("vibrate", false);
        shutdownMinutes = getPreferences(0).getInt("shutdownMinutes", 0);
        bluetooth = BluetoothAdapter.getDefaultAdapter();
        IntentFilter bluetoothFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        bluetoothFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        bluetoothFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothEvents, bluetoothFilter);
        if (bluetooth != null) bluetooth.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int profile, BluetoothProfile proxy) { a2dp = proxy; }
            public void onServiceDisconnected(int profile) { a2dp = null; }
        }, BluetoothProfile.A2DP);
        IntentFilter outputFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        outputFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(outputChange, outputFilter);
        IntentFilter storageFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        storageFilter.addDataScheme("file");
        registerReceiver(storageEvents, storageFilter);
        eqMode = Math.max(0, Math.min(6, getPreferences(0).getInt("eqMode", 0)));
        String savedBands = getPreferences(0).getString("eqBands", "");
        String[] pieces = savedBands.split(",");
        for (int i=0; i<eqBands.length && i<pieces.length; i++) {
            try { eqBands[i] = Math.max(-12, Math.min(12, Float.parseFloat(pieces[i]))); }
            catch (NumberFormatException ignored) {}
        }
        screen = new Screen(this);
        setContentView(screen);
        scheduleShutdown();
        refreshLibrary();
    }

    private void refreshLibrary() { refreshLibrary(false); }
    private void refreshLibrary(final boolean clearCache) {
        loading = true;
        if (screen != null) screen.invalidate();
        final boolean namesFirst = fileNameFirst;
        final boolean showCached = tracks.isEmpty();
        final int serial = ++libraryScanSerial;
        new Thread(new Runnable() {
            public void run() {
                if (clearCache) Library.clearCache(MainActivity.this);
                final List<Library.Track> found = Library.load(MainActivity.this,
                        new Library.CachedListener() {
                            public void ready(List<Library.Track> cached) {
                                if (showCached && !clearCache)
                                    publishLibrary(cached, serial, namesFirst, false);
                            }
                        });
                publishLibrary(found, serial, namesFirst, true);
            }
        }, "Y1 library scan").start();
    }
    private void publishLibrary(final List<Library.Track> found, final int serial,
            final boolean namesFirst, final boolean complete) {
                final File foundMusicRoot = musicRoot();
                java.util.Collections.sort(found, new java.util.Comparator<Library.Track>() {
                    public int compare(Library.Track a, Library.Track b) {
                        String left = namesFirst ? a.fileName : a.title;
                        String right = namesFirst ? b.fileName : b.title;
                        int order = left.compareToIgnoreCase(right);
                        return order != 0 ? order : a.path.compareToIgnoreCase(b.path);
                    }
                });
                LinkedHashSet<String> albumNames = new LinkedHashSet<String>();
                for (Library.Track track : found) albumNames.add(track.album);
                final List<String> foundAlbums = new ArrayList<String>(albumNames);
                java.util.Collections.sort(foundAlbums);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (serial != libraryScanSerial || isFinishing()) return;
                        String playingPath = playingIndex >= 0 && playingIndex < tracks.size() ?
                                tracks.get(playingIndex).path : null;
                        if (namesFirst != fileNameFirst) { refreshLibrary(); return; }
                        directoryRoot = foundMusicRoot;
                        tracks = found;
                        shuffleHistory.clear();
                        playingIndex = -1;
                        if (playingPath != null) for (int i=0; i<tracks.size(); i++)
                            if (playingPath.equals(tracks.get(i).path)) { playingIndex = i; break; }
                        refreshDirectory();
                        albums = foundAlbums;
                        loading = !complete;
                        if (complete && (message.equals(tr("差分を確認しています")) ||
                                message.equals(tr("キャッシュを再構築しています")))) message = "";
                        if (page == Page.DIRECTORY) selection = Math.max(0, Math.min(selection, count()-1));
                        else if (page == Page.SONGS || page == Page.ALBUMS || page == Page.ALBUM_SONGS)
                            selection = Math.max(0, Math.min(selection, count()-1));
                        screen.invalidate();
                        if (complete && tracks.isEmpty() && emptyScanRetries++ < 12) {
                            screen.removeCallbacks(storageRescan);
                            screen.postDelayed(storageRescan, 3000);
                        } else if (!tracks.isEmpty()) emptyScanRetries = 0;
                    }
                });
    }

    private int count() {
        if (page == Page.HOME) return HOME.length;
        if (page == Page.ALBUMS) return albums.size();
        if (page == Page.SONGS) return tracks.size();
        if (page == Page.DIRECTORY) return directoryFolders.size() + directorySongs.size();
        if (page == Page.ALBUM_SONGS) return albumTracks().size();
        if (page == Page.THEMES) return themes.size();
        if (page == Page.SETTINGS) return SETTINGS_ITEMS.length;
        if (page == Page.LANGUAGE) return 2;
        if (page == Page.OPTIONS) return optionSetting == Setting.SHUTDOWN_TIMER ? SHUTDOWN_MINUTES.length :
                optionSetting == Setting.SCREEN_TIMEOUT ? TIMEOUT_SECONDS.length : BRIGHTNESS_LEVELS.length;
        if (page == Page.BLUETOOTH) { refreshPaired(); return 2 + paired.size() + nearby.size(); }
        if (page == Page.SHUTDOWN) return 2;
        if (page == Page.USB_TRANSFER) return 2;
        if (page == Page.APK_FILES) return apkEntries.size() + 1;
        if (page == Page.APK_CONFIRM) return 2;
        if (page == Page.STORAGE) return 0;
        if (page == Page.EQ) return EQ_NAMES.length;
        if (page == Page.EQ_BANDS) return eqBands.length;
        return 0;
    }
    private String item(int i) {
        if (page == Page.HOME) return tr(HOME[i]);
        if (page == Page.ALBUMS) return metadata(albums.get(i));
        if (page == Page.SONGS) return fileNameFirst ? tracks.get(i).fileName : tracks.get(i).title;
        if (page == Page.DIRECTORY) {
            if (i < directoryFolders.size()) return directoryFolders.get(i).getName();
            Library.Track track = directorySongs.get(i-directoryFolders.size());
            return fileNameFirst ? track.fileName : track.title;
        }
        if (page == Page.ALBUM_SONGS) {
            Library.Track track = albumTracks().get(i);
            return fileNameFirst ? track.fileName : track.title;
        }
        if (page == Page.THEMES) return themeName(themes.get(i));
        if (page == Page.SETTINGS) {
            Setting setting = SETTINGS_ITEMS[i];
            String prefix = (setting == Setting.DISPLAY_FILENAME && fileNameFirst ||
                    setting == Setting.DISPLAY_TITLE && !fileNameFirst) ? "✓ " : "  ";
            String value = setting == Setting.SHUTDOWN_TIMER ? (shutdownMinutes == 0 ? "OFF" : shutdownMinutes + (english ? " min" : "分")) :
                    setting == Setting.SCREEN_TIMEOUT ? Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 300000)/1000 + (english ? " sec" : "秒") :
                    setting == Setting.CLICK_SOUND ? (clickSound ? "ON" : "OFF") : setting == Setting.VIBRATION ? (vibrate ? "ON" : "OFF") :
                    setting == Setting.BRIGHTNESS ? String.valueOf(Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 128)) :
                    setting == Setting.BLUETOOTH ? (bluetooth != null && bluetooth.isEnabled() ? "ON" : "OFF") :
                    setting == Setting.LANGUAGE ? (english ? "English" : "日本語") :
                    setting == Setting.AIRPLANE_MODE ? (airplaneOn() ? "ON" : "OFF") : "";
            return prefix + tr(setting.label) + (value.length() > 0 ? "  " + value : "");
        }
        if (page == Page.LANGUAGE) return (english == (i == 1) ? "✓ " : "  ") + (i == 0 ? tr("日本語") : "English");
        if (page == Page.OPTIONS) return optionSetting == Setting.SHUTDOWN_TIMER ? (SHUTDOWN_MINUTES[i] == 0 ? "OFF" : SHUTDOWN_MINUTES[i] + (english ? " min" : "分")) :
                optionSetting == Setting.SCREEN_TIMEOUT ? TIMEOUT_SECONDS[i] + (english ? " sec" : "秒") : tr("輝度") + " " + BRIGHTNESS_LEVELS[i];
        if (page == Page.BLUETOOTH) return i == 0 ? "Bluetooth  " +
                (bluetooth != null && bluetooth.isEnabled() ? "ON" : "OFF") :
                i == 1 ? tr(bluetooth != null && bluetooth.isDiscovering() ? "検索中…" : "新しい機器を探す") :
                i < 2 + paired.size() ? deviceName(paired.get(i-2)) +
                "  " + tr(a2dp != null && a2dp.getConnectedDevices().contains(paired.get(i-2)) ? "接続中" : "ペア済み") :
                deviceName(nearby.get(i-2-paired.size())) + "  " + tr("ペアリング");
        if (page == Page.APK_FILES) return i < apkEntries.size() ? apkEntries.get(i).getName() :
                tr(apkDirectory != null && !apkDirectory.equals(apkRoot) ? "親フォルダへ戻る" : "設定へ戻る");
        if (page == Page.APK_CONFIRM) return tr(i == 0 ? "キャンセル" : "インストール");
        if (page == Page.SHUTDOWN) return tr(i == 0 ? "キャンセル" : "電源を切る");
        if (page == Page.USB_TRANSFER) return tr(i == 1 ? "戻る" :
                usbBusy ? "切り替え中…" : usbStorageActive() ? "USBストレージを終了" : "USBストレージを開始");
        if (page == Page.EQ) return (i == eqMode ? "✓ " : "  ") + tr(EQ_NAMES[i]);
        if (page == Page.EQ_BANDS) return String.format(java.util.Locale.US, "%.0f Hz  %+.0f dB",
                org.videolan.libvlc.MediaPlayer.Equalizer.getBandFrequency(i), eqBands[i]);
        return "";
    }
    private List<Library.Track> albumTracks() {
        ArrayList<Library.Track> list = new ArrayList<Library.Track>();
        for (Library.Track t : tracks) if (t.album.equals(currentAlbum)) list.add(t);
        return list;
    }
    private File musicRoot() {
        File storage = Environment.getExternalStorageDirectory();
        File[] entries = storage.listFiles();
        if (entries != null) for (File entry : entries)
            if (entry.isDirectory() && entry.getName().equalsIgnoreCase("music")) return entry;
        return new File(storage, "music");
    }
    private void openMusicRoot() {
        if (directoryRoot == null) directoryRoot = new File(Environment.getExternalStorageDirectory(), "Music");
        directoryFocusPath = null;
        currentDirectory = directoryRoot;
        refreshDirectory();
        navigate(Page.DIRECTORY);
    }
    private void refreshDirectory() {
        final File target = currentDirectory;
        final int serial = ++directoryScanSerial;
        if (target == null) return;
        final String path = target.getAbsolutePath();
        final List<Library.Track> snapshot = new ArrayList<Library.Track>(tracks);
        if (!path.equals(displayedDirectory)) {
            directoryFolders.clear(); directorySongs.clear();
            displayedDirectory = path;
        }
        directoryLoading = true;
        if (screen != null) screen.invalidate();
        directoryWorker.execute(new Runnable() {
            public void run() {
                final long started = SystemClock.uptimeMillis();
                final List<File> folders = new ArrayList<File>();
                final List<Library.Track> songs = new ArrayList<Library.Track>();
                final java.util.HashMap<String, Library.Track> known =
                        new java.util.HashMap<String, Library.Track>();
                for (Library.Track track : snapshot) known.put(track.path, track);
                File[] entries = target.listFiles();
                if (entries != null) for (File entry : entries) {
                    if (entry.getName().startsWith(".")) continue;
                    if (entry.isDirectory()) folders.add(entry);
                    else if (Library.isAudioFile(entry)) {
                        Library.Track track = known.get(entry.getAbsolutePath());
                        songs.add(track != null ? track : Library.readMetadata(entry.getAbsolutePath()));
                    }
                }
                java.util.Collections.sort(folders, new java.util.Comparator<File>() {
                    public int compare(File a, File b) { return a.getName().compareToIgnoreCase(b.getName()); }
                });
                java.util.Collections.sort(songs, new java.util.Comparator<Library.Track>() {
                    public int compare(Library.Track a, Library.Track b) {
                        String left = fileNameFirst ? a.fileName : a.title;
                        String right = fileNameFirst ? b.fileName : b.title;
                        int order = left.compareToIgnoreCase(right);
                        return order != 0 ? order : a.fileName.compareToIgnoreCase(b.fileName);
                    }
                });
                android.util.Log.i("Y1Scan", "directory folders=" + folders.size() +
                        " songs=" + songs.size() + " elapsedMs=" + (SystemClock.uptimeMillis()-started));
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (serial != directoryScanSerial || currentDirectory == null ||
                                !path.equals(currentDirectory.getAbsolutePath()) || isFinishing()) return;
                        directoryFolders.clear(); directoryFolders.addAll(folders);
                        directorySongs.clear(); directorySongs.addAll(songs);
                        java.util.HashSet<String> paths = new java.util.HashSet<String>();
                        for (Library.Track track : tracks) paths.add(track.path);
                        String playingPath = playingIndex >= 0 && playingIndex < tracks.size() ?
                                tracks.get(playingIndex).path : null;
                        boolean added = false;
                        for (Library.Track track : songs)
                            if (paths.add(track.path)) { tracks.add(track); added = true; }
                        if (added) {
                            sortTracksWithoutRefresh();
                            playingIndex = -1;
                            if (playingPath != null) for (int i=0; i<tracks.size(); i++)
                                if (playingPath.equals(tracks.get(i).path)) { playingIndex = i; break; }
                            LinkedHashSet<String> names = new LinkedHashSet<String>();
                            for (Library.Track track : tracks) names.add(track.album);
                            albums = new ArrayList<String>(names);
                            java.util.Collections.sort(albums);
                        }
                        directoryLoading = false;
                        if (page == Page.DIRECTORY) {
                            if (directoryFocusPath != null) {
                                for (int i=0; i<folders.size(); i++)
                                    if (directoryFocusPath.equals(folders.get(i).getAbsolutePath())) selection=i;
                                for (int i=0; i<songs.size(); i++)
                                    if (directoryFocusPath.equals(songs.get(i).path)) selection=folders.size()+i;
                            }
                            selection = Math.max(0, Math.min(selection, folders.size()+songs.size()-1));
                        }
                        directoryFocusPath = null;
                        screen.invalidate();
                    }
                });
            }
        });
    }
    private List<Library.Track> playbackQueue() {
        if (queueDirectory == null) return tracks;
        ArrayList<Library.Track> list = new ArrayList<Library.Track>();
        for (Library.Track track : tracks)
            if (queueDirectory.equals(new File(track.path).getParent())) list.add(track);
        return list;
    }
    private String queuePosition() {
        List<Library.Track> queue = playbackQueue();
        Library.Track current = playingIndex >= 0 && playingIndex < tracks.size() ? tracks.get(playingIndex) : null;
        int position = queue.indexOf(current);
        return (position < 0 ? 0 : position+1) + " / " + queue.size();
    }
    private String subtitle(int i) {
        if (page == Page.SONGS || page == Page.ALBUM_SONGS ||
                page == Page.DIRECTORY && i >= directoryFolders.size()) {
            Library.Track track = page == Page.SONGS ? tracks.get(i) :
                    page == Page.ALBUM_SONGS ? albumTracks().get(i) :
                    directorySongs.get(i-directoryFolders.size());
            int dot = track.fileName.lastIndexOf('.');
            String stem = dot > 0 ? track.fileName.substring(0, dot) : track.fileName;
            return track.title.equalsIgnoreCase(stem) ? "" :
                    (fileNameFirst ? track.title : track.fileName);
        }
        if (page == Page.ALBUMS) {
            int n = 0;
            for (Library.Track t : tracks) if (t.album.equals(albums.get(i))) n++;
            return n + (english ? " songs" : " 曲");
        }
        if (page == Page.THEMES) return tr(themes.get(i).layout.equals("split") ? "分割画面" : "リスト画面");
        return "";
    }
    private String title() {
        if (page == Page.HOME) return tr("ミュージック");
        if (page == Page.ALBUMS) return tr("アルバム");
        if (page == Page.SONGS) return tr("曲") + "  " + tracks.size();
        if (page == Page.DIRECTORY) return currentDirectory == null ||
                currentDirectory.equals(directoryRoot) ? "Music" : currentDirectory.getName();
        if (page == Page.ALBUM_SONGS) return metadata(currentAlbum);
        if (page == Page.THEMES) return tr("テーマ");
        if (page == Page.SETTINGS) return tr("設定");
        if (page == Page.LANGUAGE) return tr("言語");
        if (page == Page.OPTIONS) return tr(optionSetting.label);
        if (page == Page.BLUETOOTH) return "Bluetooth";
        if (page == Page.STORAGE) return tr("ディスク容量");
        if (page == Page.USB_TRANSFER) return tr("USBファイル転送");
        if (page == Page.APK_FILES || page == Page.APK_CONFIRM || page == Page.APK_INSTALLING) return tr("アプリを更新");
        if (page == Page.SHUTDOWN) return tr("シャットダウン確認");
        if (page == Page.EQ || page == Page.EQ_BANDS) return tr("イコライザー");
        return tr("再生中");
    }
    private String tr(String ja) { return UiText.get(english, ja); }
    private String metadata(String value) { return tr(value); }
    private String themeName(Theme value) { return english ? value.nameEn : value.name; }
    private void openSetting(Setting setting) {
        navigate(Page.SETTINGS);
        selection = setting.ordinal();
    }
    private void navigate(Page target) { page = target; selection = 0; listFirst = 0; message = ""; marqueeStartMs = SystemClock.uptimeMillis(); screen.invalidate(); }
    private int visibleStart(int visible, int total) {
        listFirst = Math.max(0, Math.min(listFirst, Math.max(0, total - visible)));
        if (selection < listFirst) listFirst = selection;
        else if (selection >= listFirst + visible) listFirst = selection - visible + 1;
        return listFirst;
    }
    private void activate() {
        if (apkInstalling) return;
        if (page == Page.PLAYER) {
            switch (selection) {
                case 0: togglePlay(); break;
                case 1: adjustVolume(-1); break;
                case 2: adjustVolume(1); break;
                case 3: navigate(Page.EQ); selection = eqMode; break;
                case 4: playMode = (playMode + 1) % PLAY_MODES.length;
                    getPreferences(0).edit().putInt("playMode", playMode).apply();
                    message = tr(PLAY_MODES[playMode]); screen.invalidate(); break;
                case 5: cycleAbRepeat(); break;
            }
            return;
        }
        if (count() == 0) return;
        if (page == Page.HOME) {
            switch (selection) {
                case 0: if (playingIndex >= 0) { navigate(Page.PLAYER); selection = 0; }
                        else message = tr("先に曲を選んでください"); break;
                case 1: navigate(Page.ALBUMS); break;
                case 2: navigate(Page.SONGS); break;
                case 3: openMusicRoot(); break;
                case 4: themes = Theme.loadAll(this); navigate(Page.THEMES); break;
                case 5: openSetting(fileNameFirst ? Setting.DISPLAY_FILENAME : Setting.DISPLAY_TITLE); break;
            }
        } else if (page == Page.ALBUMS) {
            currentAlbum = albums.get(selection); navigate(Page.ALBUM_SONGS);
        } else if (page == Page.SONGS) {
            queueDirectory = null; shuffleHistory.clear();
            playTrack(selection);
        } else if (page == Page.ALBUM_SONGS) {
            int chosen = tracks.indexOf(albumTracks().get(selection));
            queueDirectory = null; shuffleHistory.clear();
            playTrack(chosen);
        } else if (page == Page.DIRECTORY) {
            if (selection < directoryFolders.size()) {
                currentDirectory = directoryFolders.get(selection);
                directoryFocusPath = null;
                refreshDirectory(); navigate(Page.DIRECTORY);
            } else {
                Library.Track chosen = directorySongs.get(selection-directoryFolders.size());
                queueDirectory = currentDirectory.getAbsolutePath();
                shuffleHistory.clear();
                playTrack(tracks.indexOf(chosen));
            }
        } else if (page == Page.THEMES) {
            theme = themes.get(selection);
            getPreferences(0).edit().putString("theme", theme.id).apply();
            message = themeName(theme) + tr("を適用");
            screen.invalidate();
        } else if (page == Page.SETTINGS && (SETTINGS_ITEMS[selection] == Setting.DISPLAY_FILENAME ||
                SETTINGS_ITEMS[selection] == Setting.DISPLAY_TITLE)) {
            fileNameFirst = SETTINGS_ITEMS[selection] == Setting.DISPLAY_FILENAME;
            getPreferences(0).edit().putBoolean("fileNameFirst", fileNameFirst).apply();
            Library.Track playing = playingIndex >= 0 && playingIndex < tracks.size() ?
                    tracks.get(playingIndex) : null;
            sortTracks();
            if (playing != null) playingIndex = tracks.indexOf(playing);
            message = tr(fileNameFirst ? "ファイル名順に変更" : "曲名タグ順に変更");
            screen.invalidate();
        } else if (page == Page.SETTINGS) {
            switch (SETTINGS_ITEMS[selection]) {
                case SHUTDOWN: navigate(Page.SHUTDOWN); break;
                case SHUTDOWN_TIMER: case SCREEN_TIMEOUT: case BRIGHTNESS:
                    optionSetting = SETTINGS_ITEMS[selection]; navigate(Page.OPTIONS); break;
                case CLICK_SOUND: clickSound = !clickSound;
                    getPreferences(0).edit().putBoolean("clickSound", clickSound).apply();
                    syncClickSound(); screen.invalidate(); break;
                case VIBRATION:
                    if (motor == null || !motor.hasVibrator()) {
                        vibrate = false; message = tr("バイブに対応していません");
                    } else {
                        vibrate = !vibrate;
                        if (vibrate) motor.vibrate(180);
                    }
                    getPreferences(0).edit().putBoolean("vibrate", vibrate).apply(); screen.invalidate(); break;
                case BLUETOOTH: launchAction(Settings.ACTION_BLUETOOTH_SETTINGS); break;
                case FM_RADIO:
                    if (airplaneOn()) {
                        message = tr("FMにはエアプレーンモードOFFが必要です");
                        launchAction(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                    }
                    else {
                        if (!audio.isWiredHeadsetOn()) message = tr("FMにはイヤホンが必要な場合があります");
                        launchComponent("com.mediatek.FMRadio", "com.mediatek.FMRadio.FMRadioActivity");
                    }
                    break;
                case ANDROID_SETTINGS: launchAction(Settings.ACTION_SETTINGS); break;
                case DATE_TIME: launchAction(Settings.ACTION_DATE_SETTINGS); break;
                case STORAGE: navigate(Page.STORAGE); break;
                case LANGUAGE: navigate(Page.LANGUAGE); selection = english ? 1 : 0; break;
                case AIRPLANE_MODE: toggleAirplane(); break;
                case USB_TRANSFER: navigate(Page.USB_TRANSFER); break;
                case APP_UPDATE: openApkUpdates(); break;
                case RESCAN: message = tr("差分を確認しています"); refreshLibrary(); break;
                case REBUILD_CACHE: message = tr("キャッシュを再構築しています"); refreshLibrary(true); break;
            }
        } else if (page == Page.LANGUAGE) {
            english = selection == 1;
            getPreferences(0).edit().putBoolean("english", english).apply();
            openSetting(Setting.LANGUAGE);
        } else if (page == Page.OPTIONS) {
            if (optionSetting == Setting.SHUTDOWN_TIMER) {
                if (selection != 0 && checkCallingOrSelfPermission("android.permission.SHUTDOWN") !=
                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    openSetting(Setting.SHUTDOWN_TIMER);
                    message = tr("タイマーにはsystem版が必要です"); screen.invalidate(); return;
                }
                shutdownMinutes = SHUTDOWN_MINUTES[selection];
                long deadline = shutdownMinutes == 0 ? 0 : System.currentTimeMillis() + shutdownMinutes*60000L;
                getPreferences(0).edit().putInt("shutdownMinutes", shutdownMinutes)
                        .putLong("shutdownDeadline", deadline).apply(); scheduleShutdown(); }
            else if (optionSetting == Setting.SCREEN_TIMEOUT) Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_OFF_TIMEOUT, TIMEOUT_SECONDS[selection]*1000);
            else {
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, 0);
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS,
                        BRIGHTNESS_LEVELS[selection]);
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.screenBrightness = BRIGHTNESS_LEVELS[selection] / 255f;
                getWindow().setAttributes(params);
            }
            openSetting(optionSetting);
        } else if (page == Page.BLUETOOTH) {
            if (bluetooth == null) { message = tr("Bluetooth非搭載"); screen.invalidate(); }
            else if (selection == 0) {
                if (bluetooth.isEnabled()) bluetooth.disable(); else bluetooth.enable();
                screen.invalidate();
            } else if (selection == 1) {
                nearby.clear();
                if (bluetooth.isEnabled() && bluetooth.startDiscovery()) message = tr("検索中…");
                else message = tr("BluetoothをONにしてください");
                screen.invalidate();
            } else if (selection < 2 + paired.size()) connectBluetooth(paired.get(selection-2));
            else pairBluetooth(nearby.get(selection-2-paired.size()));
        } else if (page == Page.USB_TRANSFER) {
            if (selection == 1) back();
            else if (!usbBusy) setUsbStorage(!usbStorageActive());
        } else if (page == Page.APK_FILES) {
            if (apkBusy) return;
            if (selection == apkEntries.size()) { back(); return; }
            File chosen = apkEntries.get(selection);
            if (chosen.isDirectory()) { apkDirectory = chosen; refreshApkFiles(); }
            else prepareApk(chosen);
        } else if (page == Page.APK_CONFIRM) {
            if (selection == 0) { deleteFile(ApkUpdates.STAGED_NAME); apkPrepared = null; refreshApkFiles(); }
            else installPreparedApk();
        } else if (page == Page.SHUTDOWN) {
            if (selection == 1) requestShutdown(); else { openSetting(Setting.SHUTDOWN); }
        } else if (page == Page.EQ) {
            if (selection == 6) {
                eqMode = 6;
                persistEq(); applyEq();
                navigate(Page.EQ_BANDS);
            } else {
                eqMode = selection;
                if (eqMode > 0) System.arraycopy(EQ_CURVES[eqMode-1], 0, eqBands, 0, eqBands.length);
                persistEq(); applyEq();
                message = tr(EQ_NAMES[eqMode]) + tr("を適用");
                screen.invalidate();
            }
        } else if (page == Page.EQ_BANDS) {
            editingBand = !editingBand;
            screen.invalidate();
        }
    }
    private void back() {
        if (apkInstalling) return;
        if (page == Page.APK_CONFIRM) {
            deleteFile(ApkUpdates.STAGED_NAME); apkPrepared = null; refreshApkFiles(); return;
        }
        if (page == Page.APK_FILES) {
            ++apkRequest; apkBusy = false;
            if (apkDirectory != null && !apkDirectory.equals(apkRoot)) {
                apkDirectory = apkDirectory.getParentFile(); refreshApkFiles();
            } else openSetting(Setting.APP_UPDATE);
            return;
        }
        if (page == Page.EQ_BANDS && editingBand) { editingBand = false; screen.invalidate(); return; }
        if (page == Page.EQ_BANDS) { navigate(Page.EQ); selection = 6; }
        else if (page == Page.EQ) { navigate(Page.PLAYER); selection = 3; }
        else if (page == Page.USB_TRANSFER && (usbBusy || usbStorageActive())) {
            message = tr(usbBusy ? "切り替え中…" : "PCで取り出してから終了してください");
        }
        else if (page == Page.LANGUAGE || page == Page.OPTIONS || page == Page.BLUETOOTH || page == Page.STORAGE || page == Page.USB_TRANSFER || page == Page.SHUTDOWN) {
            Setting previous = page == Page.OPTIONS ? optionSetting :
                    page == Page.BLUETOOTH ? Setting.BLUETOOTH :
                    page == Page.STORAGE ? Setting.STORAGE :
                    page == Page.USB_TRANSFER ? Setting.USB_TRANSFER :
                    page == Page.LANGUAGE ? Setting.LANGUAGE : Setting.SHUTDOWN;
            openSetting(previous);
        }
        else if (page == Page.ALBUM_SONGS) navigate(Page.ALBUMS);
        else if (page == Page.PLAYER && queueDirectory != null) {
            currentDirectory = new File(queueDirectory);
            directoryFocusPath = playingIndex >= 0 && playingIndex < tracks.size() ? tracks.get(playingIndex).path : null;
            refreshDirectory(); navigate(Page.DIRECTORY);
            if (playingIndex >= 0 && playingIndex < tracks.size()) {
                int song = directorySongs.indexOf(tracks.get(playingIndex));
                if (song >= 0) selection = directoryFolders.size() + song;
            }
        }
        else if (page == Page.DIRECTORY) {
            if (currentDirectory != null && !currentDirectory.equals(directoryRoot)) {
                File child = currentDirectory;
                currentDirectory = currentDirectory.getParentFile();
                directoryFocusPath = child.getAbsolutePath();
                refreshDirectory(); navigate(Page.DIRECTORY);
                selection = directoryFolders.indexOf(child);
                if (selection < 0) selection = 0;
            } else { navigate(Page.HOME); selection = 3; }
        }
        else if (page != Page.HOME) navigate(Page.HOME);
        else message = "";
        screen.invalidate();
    }
    private void move(int amount) {
        if (page == Page.EQ_BANDS && editingBand) { adjustBand(amount); return; }
        if (page == Page.PLAYER) {
            int previous = selection;
            selection = Math.max(0, Math.min(5, selection + amount));
            if (selection != previous) cursorFeedback();
            screen.invalidate();
            return;
        }
        int size = count();
        if (size == 0) return;
        int previous = selection;
        selection = Math.max(0, Math.min(size - 1, selection + amount));
        if (selection != previous) cursorFeedback();
        marqueeStartMs = SystemClock.uptimeMillis();
        screen.invalidate();
    }
    private void cursorFeedback() {
        if (clickSound && cursorSoundReady && cursorSounds != null)
            cursorSounds.play(cursorSoundId, .5f, .5f, 1, 0, 1);
        if (vibrate && motor != null) motor.vibrate(60);
    }
    private void sortTracks() {
        shuffleHistory.clear();
        sortTracksWithoutRefresh();
        refreshDirectory();
    }
    private void sortTracksWithoutRefresh() {
        java.util.Collections.sort(tracks, new java.util.Comparator<Library.Track>() {
            public int compare(Library.Track a, Library.Track b) {
                String left = fileNameFirst ? a.fileName : a.title;
                String right = fileNameFirst ? b.fileName : b.title;
                int order = left.compareToIgnoreCase(right);
                return order != 0 ? order : a.fileName.compareToIgnoreCase(b.fileName);
            }
        });
    }
    private void launchAction(String action) {
        try { startActivity(new Intent(action)); }
        catch (Exception error) { message = tr("画面を開けません"); screen.invalidate(); }
    }
    private boolean usbStorageActive() {
        return Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
    }
    private void setUsbStorage(final boolean enable) {
        if (checkCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS") !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
            message = tr("USB転送にはsystem版が必要です"); screen.invalidate(); return;
        }
        final Object storage = getSystemService(Context.STORAGE_SERVICE);
        if (storage == null) { message = tr("USB切替に失敗しました"); screen.invalidate(); return; }
        try {
            Intent usbState = registerReceiver(null, new IntentFilter("android.hardware.usb.action.USB_STATE"));
            if (enable && (usbState == null || !usbState.getBooleanExtra("connected", false))) {
                message = tr("USBケーブルを接続してください"); screen.invalidate(); return;
            }
        } catch (Exception error) {
            message = tr("USB接続を確認できません"); screen.invalidate(); return;
        }
        if (enable) {
            releasePlayback();
            playingIndex = -1;
            audio.abandonAudioFocus(null);
            if (cover != null) { cover.recycle(); cover = null; }
        }
        usbBusy = true;
        message = tr("切り替え中…");
        screen.invalidate();
        new Thread(new Runnable() {
            public void run() {
                boolean success = false;
                try {
                    if (enable) {
                        DeviceSetupReceiver.setUsbFunction(MainActivity.this, "mass_storage,adb", false);
                        for (int i = 0; i < 20; i++) {
                            if ((Boolean)storage.getClass().getMethod("isUsbMassStorageConnected").invoke(storage)) break;
                            SystemClock.sleep(250);
                        }
                    }
                    storage.getClass().getMethod(enable ? "enableUsbMassStorage" :
                            "disableUsbMassStorage").invoke(storage);
                    for (int i = 0; i < 40; i++) {
                        if (enable == usbStorageActive()) { success = true; break; }
                        SystemClock.sleep(500);
                    }
                    if (!enable && success)
                        DeviceSetupReceiver.setUsbFunction(MainActivity.this, "adb", true);
                } catch (Exception error) {
                    android.util.Log.e("Y1USB", "USB mass storage switch failed", error);
                }
                final boolean done = success;
                runOnUiThread(new Runnable() {
                    public void run() {
                        usbBusy = false;
                        message = tr(done ? enable ? "PCにディスクが表示されます" : "転送完了" :
                                "USB切替に失敗しました");
                        if (done && enable) usbReturnPending = true;
                        if (done && !enable) {
                            refreshLibrary();
                            screen.removeCallbacks(usbReturn);
                            screen.postDelayed(usbReturn, 2500);
                            screen.removeCallbacks(clearUsbReturn);
                            screen.postDelayed(clearUsbReturn, 20000);
                        }
                        screen.invalidate();
                    }
                });
            }
        }, "Y1 USB storage switch").start();
    }
    private void syncClickSound() {
        // This app plays its own tick for every cursor move, independent of
        // the framework's different LEFT/RIGHT versus UP/DOWN sound handling.
        int value = 0;
        if (Settings.System.getInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, -1) != value)
            if (!Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, value))
                message = tr("クリック音を変更できません");
    }
    private boolean airplaneOn() {
        return Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }
    private void toggleAirplane() {
        if (checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
            launchAction(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
            return;
        }
        boolean next = !airplaneOn();
        try {
            if (!Settings.Global.putInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, next ? 1 : 0))
                throw new IllegalStateException("setting rejected");
            Intent changed = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            changed.putExtra("state", next);
            sendBroadcast(changed);
            message = tr(next ? "エアプレーンモードON" : "エアプレーンモードOFF");
        } catch (Exception error) { message = tr("エアプレーンモードを変更できません"); }
        screen.invalidate();
    }
    private void launchComponent(String pkg, String cls) {
        try { startActivity(new Intent().setClassName(pkg, cls)); }
        catch (Exception error) { message = tr("FMラジオを開けません"); screen.invalidate(); }
    }
    private void openApkUpdates() {
        if (usbBusy || usbStorageActive() || !Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            message = tr("USBストレージを終了してください"); screen.invalidate(); return;
        }
        apkRoot = Environment.getExternalStorageDirectory();
        File updates = new File(apkRoot, "Updates");
        apkDirectory = updates.isDirectory() ? updates : apkRoot;
        refreshApkFiles();
    }
    private void refreshApkFiles() {
        final File directory = apkDirectory;
        final int serial = ++apkRequest;
        apkBusy = true; apkEntries.clear();
        navigate(Page.APK_FILES); message = tr("APKを読み込み中…");
        apkWorker.execute(new Runnable() {
            public void run() {
                List<File> found = new ArrayList<File>(); String problem = null;
                try { found = ApkUpdates.list(apkRoot, directory); }
                catch (Exception error) { problem = error instanceof ApkUpdates.Failure ? error.getMessage() : "APKを読み込めません"; }
                final List<File> entries = found; final String failure = problem;
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (serial != apkRequest || isFinishing()) return;
                        apkBusy = false; apkEntries.clear(); apkEntries.addAll(entries);
                        message = tr(failure != null ? failure : entries.isEmpty() ? "APKがありません" : "APKを選択してください");
                        screen.invalidate();
                    }
                });
            }
        });
    }
    private void prepareApk(final File source) {
        final int serial = ++apkRequest;
        apkBusy = true; message = tr("APKを確認しています…"); screen.invalidate();
        apkWorker.execute(new Runnable() {
            public void run() {
                ApkUpdates.Prepared result = null; String problem = null;
                try { result = ApkUpdates.prepare(MainActivity.this, apkRoot, source); }
                catch (Exception error) { problem = error instanceof ApkUpdates.Failure ? error.getMessage() : "APKを読み込めません"; }
                final ApkUpdates.Prepared prepared = result; final String failure = problem;
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (serial != apkRequest || isFinishing()) return;
                        apkBusy = false; apkPrepared = prepared;
                        if (prepared != null) navigate(Page.APK_CONFIRM);
                        else { message = tr(failure == null ? "APKを読み込めません" : failure); screen.invalidate(); }
                    }
                });
            }
        });
    }
    private void installPreparedApk() {
        if (apkPrepared == null || apkInstalling) return;
        if (player != null && player.isPlaying()) player.pause();
        if (vlcPlayer != null && vlcPlayer.isPlaying()) vlcPlayer.pause();
        try {
            getPreferences(0).edit().putBoolean("apkUpdatePending", true).commit();
            apkInstalling = true; navigate(Page.APK_INSTALLING);
            apkInstallObserver = ApkUpdates.install(this, apkPrepared.file, new ApkUpdates.InstallResult() {
                public void finished(final int result) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            apkInstalling = false; apkInstallObserver = null;
                            getPreferences(0).edit().putBoolean("apkUpdatePending", false).commit();
                            deleteFile(ApkUpdates.STAGED_NAME); apkPrepared = null;
                            openSetting(Setting.APP_UPDATE);
                            message = tr(result == 1 ? "アプリを更新しました" : "アプリを更新できません");
                            android.util.Log.i("Y1Update", "install result=" + result);
                            screen.invalidate();
                        }
                    });
                }
            });
        } catch (Exception error) {
            apkInstalling = false;
            getPreferences(0).edit().putBoolean("apkUpdatePending", false).commit();
            openSetting(Setting.APP_UPDATE);
            message = tr(error instanceof ApkUpdates.Failure ? error.getMessage() : "アプリを更新できません");
            android.util.Log.e("Y1Update", "install failed", error); screen.invalidate();
        }
    }
    private void requestShutdown() {
        if (checkCallingOrSelfPermission("android.permission.SHUTDOWN") !=
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openSetting(Setting.SHUTDOWN);
            message = tr("シャットダウンにはsystem版が必要です"); screen.invalidate(); return;
        }
        try {
            Intent intent = new Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN");
            intent.putExtra("android.intent.extra.KEY_CONFIRM", false);
            startActivity(intent);
        } catch (SecurityException error) {
            message = tr("シャットダウンにはsystem版が必要です"); screen.invalidate();
        } catch (Exception error) { message = tr("電源操作に失敗しました"); screen.invalidate(); }
    }
    private void scheduleShutdown() {
        AlarmManager alarms = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ShutdownReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarms.cancel(pending);
        long deadline = getPreferences(0).getLong("shutdownDeadline", 0);
        if (shutdownMinutes > 0 && deadline > System.currentTimeMillis() &&
                checkCallingOrSelfPermission("android.permission.SHUTDOWN") ==
                android.content.pm.PackageManager.PERMISSION_GRANTED)
            alarms.set(AlarmManager.RTC_WAKEUP, deadline, pending);
        else if (shutdownMinutes > 0 && deadline != 0) {
            shutdownMinutes = 0;
            getPreferences(0).edit().putInt("shutdownMinutes", 0).putLong("shutdownDeadline", 0).apply();
        }
    }
    private void refreshPaired() {
        paired.clear();
        if (bluetooth != null && bluetooth.isEnabled()) {
            try { paired.addAll(bluetooth.getBondedDevices()); }
            catch (Exception ignored) {}
        }
        nearby.removeAll(paired);
    }
    private String deviceName(BluetoothDevice device) {
        String name = device.getName();
        return name == null || name.length() == 0 ? device.getAddress() : name;
    }
    private void pairBluetooth(BluetoothDevice device) {
        try {
            if (bluetooth != null) bluetooth.cancelDiscovery();
            java.lang.reflect.Method pair = device.getClass().getMethod("createBond");
            boolean started = (Boolean)pair.invoke(device);
            message = tr(started ? "ペアリングを開始しました" : "ペアリングできません");
        } catch (Exception error) { message = tr("ペアリングを開始できません"); }
        screen.invalidate();
    }
    private void connectBluetooth(BluetoothDevice device) {
        try {
            if (a2dp == null) { message = tr("Bluetooth準備中"); screen.invalidate(); return; }
            java.lang.reflect.Method connect = a2dp.getClass().getMethod("connect", BluetoothDevice.class);
            boolean started = (Boolean)connect.invoke(a2dp, device);
            message = tr(started ? "接続を要求しました" : "接続できません");
        } catch (Exception error) { message = tr("Bluetooth設定から接続してください"); }
        screen.invalidate();
    }
    private void playTrack(int index) {
        if (index < 0 || index >= tracks.size()) return;
        clearAbRepeat();
        releasePlayback();
        if (cover != null) { cover.recycle(); cover = null; }
        int focus = page == Page.PLAYER ? selection : 0;
        playingIndex = index;
        nowPlayingTitle = playbackTitle(tracks.get(index));
        final String path = tracks.get(index).path;
        final int serial = playbackSerial;
        marqueeStartMs = SystemClock.uptimeMillis();
        navigate(Page.PLAYER);
        selection = focus;
        message = tr("読み込み中");
        audio.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (useVlcFirst(path)) {
            startVlc(path, serial);
            loadCover(path);
            screen.invalidate();
            return;
        }
        try {
            final MediaPlayer created = new MediaPlayer();
            player = created;
            created.setAudioStreamType(AudioManager.STREAM_MUSIC);
            created.setDataSource(tracks.get(index).path);
            created.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    if (mp == player && serial == playbackSerial) {
                        message = ""; mp.start(); screen.invalidate();
                    }
                }
            });
            created.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    if (mp == player && serial == playbackSerial) onTrackEnded();
                }
            });
            created.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    if (mp == player && serial == playbackSerial) {
                        player = null;
                        mp.release();
                        startVlc(path, serial);
                    }
                    return true;
                }
            });
            created.prepareAsync();
        } catch (Exception e) {
            if (player != null) { player.release(); player = null; }
            startVlc(path, serial);
        }
        loadCover(path);
        screen.invalidate();
    }
    private String playbackTitle(Library.Track track) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        boolean metadataReadable = false;
        try {
            retriever.setDataSource(track.path);
            metadataReadable = true;
            String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            if (title != null && title.trim().length() > 0 && !"<unknown>".equalsIgnoreCase(title.trim()))
                return title.trim();
        } catch (Exception ignored) {
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
        if (metadataReadable) return track.fileName;
        int dot = track.fileName.lastIndexOf('.');
        String stem = dot > 0 ? track.fileName.substring(0, dot) : track.fileName;
        if (!track.title.equalsIgnoreCase(stem) && !track.title.equals(track.fileName))
            return track.title;
        return track.fileName;
    }
    private String displayedPlayingTitle(Library.Track track) {
        return nowPlayingTitle == null ? track.fileName : nowPlayingTitle;
    }
    private boolean useVlcFirst(String path) {
        if (eqMode != 0 || boostLevel != 0) return true;
        String name = path.toLowerCase(java.util.Locale.US);
        return name.endsWith(".aif") || name.endsWith(".aiff") || name.endsWith(".aifc") ||
                name.endsWith(".wma") || name.endsWith(".ape") || name.endsWith(".opus") ||
                name.endsWith(".wv") || name.endsWith(".mka") || name.endsWith(".caf");
    }
    private void startVlc(String path, final int serial) {
        startVlc(path, serial, -1, false);
    }
    private void startVlc(String path, final int serial, final long seekMs, final boolean resumePlaying) {
        try {
            if (vlc == null) vlc = new org.videolan.libvlc.LibVLC(this,
                    new ArrayList<String>(java.util.Arrays.asList("--no-video", "--no-stats",
                            "--audio-resampler=soxr")));
            final org.videolan.libvlc.MediaPlayer created =
                    new org.videolan.libvlc.MediaPlayer(vlc);
            vlcPlayer = created;
            if (eqMode != 0) created.setEqualizer(makeEqualizer());
            final boolean[] seekPending = {seekMs >= 0};
            created.setEventListener(new org.videolan.libvlc.MediaPlayer.EventListener() {
                public void onEvent(final org.videolan.libvlc.MediaPlayer.Event event) {
                    runOnUiThread(new Runnable() { public void run() {
                        if (serial != playbackSerial || created != vlcPlayer) return;
                        if (event.type == org.videolan.libvlc.MediaPlayer.Event.Playing) {
                            created.setVolume(boostPercent());
                            if (seekPending[0]) {
                                seekPending[0] = false;
                                created.setTime(seekMs);
                                if (!resumePlaying) created.pause();
                            }
                            message = ""; screen.invalidate();
                        } else if (event.type == org.videolan.libvlc.MediaPlayer.Event.EndReached) {
                            onTrackEnded();
                        } else if (event.type == org.videolan.libvlc.MediaPlayer.Event.EncounteredError) {
                            message = tr("再生できません"); screen.invalidate();
                        }
                    }});
                }
            });
            org.videolan.libvlc.Media media = new org.videolan.libvlc.Media(vlc,
                    Uri.fromFile(new File(path)));
            created.setMedia(media);
            media.release();
            created.setVolume(boostPercent());
            created.play();
        } catch (Throwable error) {
            android.util.Log.e("Y1VLC", "LibVLC start failed", error);
            if (vlcPlayer != null) { vlcPlayer.release(); vlcPlayer = null; }
            message = tr("LibVLCで再生できません: ") + error.getClass().getSimpleName();
            screen.invalidate();
        }
    }
    private void releasePlayback() {
        playbackSerial++;
        if (player != null) { player.release(); player = null; }
        if (vlcPlayer != null) { vlcPlayer.stop(); vlcPlayer.release(); vlcPlayer = null; }
    }
    private org.videolan.libvlc.MediaPlayer.Equalizer makeEqualizer() {
        org.videolan.libvlc.MediaPlayer.Equalizer equalizer =
                org.videolan.libvlc.MediaPlayer.Equalizer.create();
        for (int i=0; i<eqBands.length; i++) {
            equalizer.setAmp(i, eqBands[i]);
        }
        // VLC's EQ filter scales the dry signal by 0.25; +12 dB restores unity.
        equalizer.setPreAmp(12.0f);
        return equalizer;
    }
    private void persistEq() {
        StringBuilder saved = new StringBuilder();
        for (int i=0; i<eqBands.length; i++) {
            if (i != 0) saved.append(',');
            saved.append(eqBands[i]);
        }
        getPreferences(0).edit().putInt("eqMode", eqMode)
                .putString("eqBands", saved.toString()).apply();
    }
    private void applyEq() {
        if (vlcPlayer != null) {
            vlcPlayer.setEqualizer(eqMode == 0 ? null : makeEqualizer());
        } else if (player != null && eqMode != 0) switchToVlc();
    }
    private void switchToVlc() { switchToVlc(false); }
    private void switchToVlc(boolean resumeRequested) {
        if (player == null || playingIndex < 0) return;
        long position = 0;
        boolean wasPlaying = false;
        try { position = player.getCurrentPosition(); wasPlaying = player.isPlaying(); }
        catch (IllegalStateException ignored) {}
        player.release(); player = null;
        startVlc(tracks.get(playingIndex).path, playbackSerial,
                position, wasPlaying || resumeRequested);
    }
    private void adjustBand(int delta) {
        if (page != Page.EQ_BANDS || selection < 0 || selection >= eqBands.length) return;
        eqBands[selection] = Math.max(-12, Math.min(12, eqBands[selection] + delta));
        eqMode = 6;
        persistEq(); applyEq(); screen.invalidate();
    }
    private void loadCover(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            byte[] bytes = retriever.getEmbeddedPicture();
            if (bytes != null && bytes.length <= 4000000) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
                int biggest = Math.max(opts.outWidth, opts.outHeight);
                opts.inSampleSize = Math.max(1, biggest / 512);
                opts.inJustDecodeBounds = false;
                cover = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
            }
        } catch (Exception ignored) {
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }
    private void togglePlay() {
        if (player == null && vlcPlayer == null) return;
        try {
            if (player != null) {
                if (player.isPlaying()) player.pause();
                else if (boostLevel > 0) switchToVlc(true);
                else player.start();
            }
            else { if (vlcPlayer.isPlaying()) vlcPlayer.pause(); else vlcPlayer.play(); }
        }
        catch (Exception ignored) {}
        screen.invalidate();
    }
    private boolean isPlaying() {
        try { return player != null ? player.isPlaying() : vlcPlayer != null && vlcPlayer.isPlaying(); }
        catch (Exception ignored) { return false; }
    }
    private void next(int direction) {
        List<Library.Track> queue = playbackQueue();
        if (queue.isEmpty()) return;
        int position = playingIndex >= 0 && playingIndex < tracks.size() ?
                queue.indexOf(tracks.get(playingIndex)) : -1;
        int nextPosition;
        if (playMode == 3 && direction > 0 && queue.size() > 1) {
            shuffleHistory.add(playingIndex);
            nextPosition = position;
            while (nextPosition == position) nextPosition = (int)(Math.random() * queue.size());
        } else if (playMode == 3 && direction < 0 && !shuffleHistory.isEmpty())
            nextPosition = queue.indexOf(tracks.get(shuffleHistory.remove(shuffleHistory.size()-1)));
        else nextPosition = position < 0 ? 0 : (position + direction + queue.size()) % queue.size();
        if (nextPosition < 0) nextPosition = 0;
        playTrack(tracks.indexOf(queue.get(nextPosition)));
    }
    private void onTrackEnded() {
        if (abStartMs >= 0 && abEndMs >= 0) {
            try {
                if (player != null) { player.seekTo((int)abStartMs); player.start(); }
                else if (vlcPlayer != null) { vlcPlayer.setTime(abStartMs); vlcPlayer.play(); }
            } catch (Exception ignored) {}
            return;
        }
        if (playMode == 2) { playTrack(playingIndex); return; }
        List<Library.Track> queue = playbackQueue();
        if (playMode == 0 && !queue.isEmpty() && playingIndex >= 0 &&
                tracks.get(playingIndex) == queue.get(queue.size()-1)) {
            releasePlayback();
            message = tr("再生終了"); screen.invalidate(); return;
        }
        next(1);
    }
    private long playbackPositionMs() {
        try { return player != null ? player.getCurrentPosition() :
                vlcPlayer != null ? vlcPlayer.getTime() : 0; }
        catch (Exception ignored) { return 0; }
    }
    private void clearAbRepeat() {
        if (screen != null) screen.removeCallbacks(abLoop);
        abStartMs = abEndMs = -1;
        lastAbSeekMs = 0;
    }
    private void cycleAbRepeat() {
        if (player == null && vlcPlayer == null) return;
        if (abStartMs < 0) {
            abStartMs = playbackPositionMs();
            abEndMs = -1;
            message = "";
        } else if (abEndMs < 0) {
            long position = playbackPositionMs();
            if (position < abStartMs + 1000) {
                message = tr("BはAより後に設定"); screen.invalidate(); return;
            }
            abEndMs = position;
            screen.removeCallbacks(abLoop);
            screen.post(abLoop);
            message = "";
        } else {
            clearAbRepeat();
            message = "";
        }
        screen.invalidate();
    }
    private void seek(int direction, long heldMs) {
        try {
            long duration = player != null ? player.getDuration() : vlcPlayer != null ? vlcPlayer.getLength() : 0;
            if (duration <= 0) { message = tr("シークできません"); screen.invalidate(); return; }
            long distance = seekDistanceMs(heldMs, duration);
            long target = Math.max(0, Math.min(duration-1000,
                    skipStartPositionMs + direction*distance));
            if (player != null) player.seekTo((int)target); else if (vlcPlayer != null) vlcPlayer.setTime(target);
            message = tr(direction > 0 ? "早送り " : "巻き戻し ") + formatTime((int)target);
            seekMessage = message;
            screen.removeCallbacks(clearSeekMessage);
            screen.postDelayed(clearSeekMessage, 1500);
            screen.invalidate();
        } catch (Exception error) { message = tr("シークできません"); screen.invalidate(); }
    }
    private long seekDistanceMs(long heldMs, long durationMs) {
        long elapsed = Math.max(0, heldMs);
        if (elapsed >= 50000000) return durationMs;
        long blocks = elapsed / 5000;
        long remainder = elapsed % 5000;
        long completed = 5000 * (10 * blocks + 3 * blocks * (blocks - 1) / 2);
        return Math.min(durationMs, completed + remainder * (10 + 3 * blocks));
    }
    private String formatTime(int ms) { int s=Math.max(0,ms/1000); return (s/60)+":"+(s%60<10?"0":"")+(s%60); }
    private String playModeText() {
        return abStartMs < 0 ? tr(PLAY_MODES[playMode]) :
                "A-B " + formatTime((int)abStartMs) +
                (abEndMs < 0 ? "" : "-" + formatTime((int)abEndMs));
    }
    private void adjustVolume(int direction) {
        int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (boostLevel > 0 && current < max) boostLevel = 0;
        if (direction > 0 && current == max) boostLevel = Math.min(3, boostLevel + 1);
        else if (direction < 0 && boostLevel > 0) boostLevel--;
        else audio.setStreamVolume(AudioManager.STREAM_MUSIC,
                Math.max(0, Math.min(max, current + direction)), 0);
        if (vlcPlayer != null) {
            int result = vlcPlayer.setVolume(boostPercent());
            android.util.Log.i("Y1Volume", "boost=" + boostLevel + " volume=" +
                    boostPercent() + " result=" + result + " readback=" + vlcPlayer.getVolume());
        } else if (boostLevel > 0 && player != null) {
            try { if (player.isPlaying()) switchToVlc(); }
            catch (IllegalStateException ignored) {}
        }
        screen.invalidate();
    }
    private int boostPercent() { return 100 + boostLevel * 10; }
    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (apkInstalling) return true;
        if ((code == KeyEvent.KEYCODE_MEDIA_PREVIOUS || code == KeyEvent.KEYCODE_MEDIA_NEXT) && page == Page.PLAYER) {
            if (skipKey != code || event.getRepeatCount() == 0) {
                skipKey = code;
                skipHeld = false;
                skipDownTimeMs = event.getEventTime();
                skipStartPositionMs = playbackPositionMs();
            }
            if (event.getRepeatCount() > 0) {
                skipHeld = true;
                seek(code == KeyEvent.KEYCODE_MEDIA_NEXT ? 1 : -1,
                        event.getEventTime() - skipDownTimeMs);
            }
            return true;
        }
        if (event.getRepeatCount() > 0 && code != KeyEvent.KEYCODE_DPAD_UP &&
                code != KeyEvent.KEYCODE_DPAD_DOWN && code != KeyEvent.KEYCODE_PAGE_UP &&
                code != KeyEvent.KEYCODE_PAGE_DOWN && code != KeyEvent.KEYCODE_VOLUME_UP &&
                code != KeyEvent.KEYCODE_VOLUME_DOWN) return true;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_PAGE_UP:
                move(-1); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_PAGE_DOWN:
                move(1); return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN: adjustVolume(-1); return true;
            case KeyEvent.KEYCODE_VOLUME_UP: adjustVolume(1); return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                move(-1); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                move(1); return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                if (page == Page.EQ_BANDS) adjustBand(-1);
                else move(-1); return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                if (page == Page.EQ_BANDS) adjustBand(1);
                else move(1); return true;
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_CALL:
                if (vibrate && motor != null) motor.vibrate(85);
                activate(); return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_SPACE: togglePlay(); return true;
            case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_MENU:
                back(); return true;
            default: return super.onKeyDown(code, event);
        }
    }
    @Override public boolean onKeyUp(int code, KeyEvent event) {
        if (apkInstalling) return true;
        if (code == skipKey &&
                (code == KeyEvent.KEYCODE_MEDIA_PREVIOUS || code == KeyEvent.KEYCODE_MEDIA_NEXT)) {
            if (page == Page.PLAYER && !event.isCanceled()) {
                long heldMs = Math.max(0, event.getEventTime() - skipDownTimeMs);
                int direction = code == KeyEvent.KEYCODE_MEDIA_NEXT ? 1 : -1;
                if (skipHeld || heldMs >= 500) {
                    seek(direction, heldMs);
                    android.util.Log.d("Y1Seek", "heldMs=" + heldMs + " baseMs=" +
                            skipStartPositionMs + " targetMs=" + playbackPositionMs());
                } else next(direction);
            }
            skipHeld = false; skipKey = 0; return true;
        }
        return super.onKeyUp(code, event);
    }
    @Override public void onBackPressed() { back(); }
    @Override protected void onResume() {
        super.onResume();
        if (!loading && tracks.isEmpty() && Environment.MEDIA_MOUNTED.equals(
                Environment.getExternalStorageState())) refreshLibrary();
        originalSystemSound = Settings.System.getInt(getContentResolver(),
                Settings.System.SOUND_EFFECTS_ENABLED, 1);
        syncClickSound();
        if (screen != null) screen.invalidate();
    }
    @Override protected void onPause() {
        if (usbReturnPending && !usbBusy && !usbStorageActive() && screen != null) {
            screen.removeCallbacks(usbReturn);
            screen.postDelayed(usbReturn, 1000);
        }
        if (screen != null) screen.stopRefresh();
        if (originalSystemSound >= 0) {
            Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED,
                    originalSystemSound);
            originalSystemSound = -1;
        }
        super.onPause();
    }
    @Override protected void onDestroy() {
        if (screen != null) screen.removeCallbacks(clearTransientMessage);
        if (screen != null) screen.removeCallbacks(clearSeekMessage);
        if (screen != null) screen.removeCallbacks(abLoop);
        ++directoryScanSerial;
        ++libraryScanSerial;
        directoryWorker.shutdownNow();
        ++apkRequest; apkWorker.shutdownNow();
        if (cursorSounds != null) { cursorSounds.release(); cursorSounds = null; }
        if (screen != null) screen.removeCallbacks(storageRescan);
        if (screen != null) screen.removeCallbacks(usbReturn);
        if (screen != null) screen.removeCallbacks(clearUsbReturn);
        unregisterReceiver(storageEvents);
        unregisterReceiver(outputChange);
        unregisterReceiver(bluetoothEvents);
        if (bluetooth != null && a2dp != null) bluetooth.closeProfileProxy(BluetoothProfile.A2DP, a2dp);
        releasePlayback();
        if (vlc != null) { vlc.release(); vlc = null; }
        if (cover != null) { cover.recycle(); cover = null; }
        audio.abandonAudioFocus(null);
        super.onDestroy();
    }

    private final class Screen extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final RectF r = new RectF();
        private final Bitmap winampMenuArt, winampPlayerArt;
        private final BdfFont hiMdFont10, hiMdFont12;
        private boolean marqueeActive, refreshScheduled;
        private int marqueeLeft, marqueeTop, marqueeRight, marqueeBottom;
        private long lastFullRefresh;
        private final Runnable refresh = new Runnable() {
            public void run() {
                refreshScheduled = false;
                long now = SystemClock.uptimeMillis();
                if (page == Page.PLAYER && isPlaying() && theme != null &&
                        (theme.layout.equals("winamp") || theme.layout.equals("himd"))) {
                    invalidate();
                } else if (page == Page.PLAYER && isPlaying() && marqueeActive &&
                        now - lastFullRefresh >= 500) {
                    lastFullRefresh = now;
                    invalidate();
                } else if (marqueeActive) {
                    float sx = getWidth()/480f, sy = getHeight()/360f;
                    invalidate((int)(marqueeLeft*sx), (int)(marqueeTop*sy),
                            (int)Math.ceil(marqueeRight*sx), (int)Math.ceil(marqueeBottom*sy));
                } else if (page == Page.PLAYER && isPlaying()) {
                    float sx = getWidth()/480f, sy = getHeight()/360f;
                    invalidate((int)(50*sx), (int)(290*sy),
                            (int)(430*sx), (int)(331*sy));
                }
            }
        };
        Screen(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            winampMenuArt = loadSkin("skins/winamp/menu.png");
            winampPlayerArt = loadSkin("skins/winamp/player.png");
            hiMdFont10 = new BdfFont(context, "fonts/umplus_j10r.bin");
            hiMdFont12 = new BdfFont(context, "fonts/umplus_j12r.bin");
        }
        private Bitmap loadSkin(String path) {
            try {
                java.io.InputStream in = getAssets().open(path);
                try { return BitmapFactory.decodeStream(in); }
                finally { in.close(); }
            } catch (Exception error) { return null; }
        }
        private void skin(Canvas c, Bitmap art) {
            if (art == null) fill(c, 0, 0, 480, 360, theme.background);
            else {
                p.setShader(null); p.setColor(Color.WHITE); p.setFilterBitmap(false);
                c.drawBitmap(art, 0, 0, p);
                p.setFilterBitmap(true);
            }
        }
        void stopRefresh() { removeCallbacks(refresh); refreshScheduled = false; }
        private void fill(Canvas c, float x, float y, float w, float h, int color) {
            p.setShader(null); p.setColor(color); c.drawRect(x, y, x + w, y + h, p);
        }
        private void grad(Canvas c, float x, float y, float w, float h, int top, int bottom) {
            p.setColor(Color.WHITE); p.setShader(new LinearGradient(x, y, x, y+h, top, bottom, Shader.TileMode.CLAMP));
            c.drawRect(x, y, x+w, y+h, p); p.setShader(null);
        }
        private void text(Canvas c, String s, float x, float baseline, float size, int color, boolean bold, float maxWidth) {
            if (s == null) s = "";
            p.setShader(null); p.setColor(color); p.setFakeBoldText(bold); p.setTextSize(size);
            if (maxWidth > 0 && measureText(s) > maxWidth) {
                float limit = Math.max(0, maxWidth - measureText("…"));
                StringBuilder prefix = new StringBuilder();
                float width = 0;
                for (int offset = 0; offset < s.length();) {
                    int codepoint = s.codePointAt(offset);
                    String glyph = new String(Character.toChars(codepoint));
                    float glyphWidth = measureText(glyph);
                    if (width + glyphWidth > limit) break;
                    prefix.append(glyph); width += glyphWidth;
                    offset += Character.charCount(codepoint);
                }
                s = prefix.append('…').toString();
            }
            drawRuns(c, s, x, baseline);
            p.setFakeBoldText(false);
        }
        private void drawRuns(Canvas c, String s, float x, float baseline) {
            float cursor = x;
            float baseSize = p.getTextSize();
            for (int offset = 0; offset < s.length();) {
                boolean latin = theme.usesPrimary(s.codePointAt(offset));
                int end = offset + Character.charCount(s.codePointAt(offset));
                while (end < s.length() && theme.usesPrimary(s.codePointAt(end)) == latin)
                    end += Character.charCount(s.codePointAt(end));
                boolean wasBold = p.isFakeBoldText();
                p.setTypeface(latin ? wasBold && theme.boldTypeface != null ?
                        theme.boldTypeface : theme.typeface : theme.nonLatinTypeface);
                p.setTextSize(baseSize * (latin ? theme.primaryScale : 1f));
                if (latin && theme.boldTypeface != null) p.setFakeBoldText(false);
                if (theme.pixelPrimary && latin) { p.setFakeBoldText(false); p.setAntiAlias(false); }
                String run = s.substring(offset, end);
                c.drawText(run, cursor, baseline, p);
                cursor += p.measureText(run);
                p.setFakeBoldText(wasBold); p.setAntiAlias(true);
                offset = end;
            }
            p.setTextSize(baseSize);
        }
        private void marqueeText(Canvas c, String s, float x, float baseline, float size,
                int color, boolean bold, float maxWidth) {
            p.setShader(null); p.setColor(color); p.setFakeBoldText(bold); p.setTextSize(size);
            float width = measureText(s);
            if (width <= maxWidth) { text(c, s, x, baseline, size, color, bold, maxWidth); return; }
            float distance = width - maxWidth;
            long duration = (long)(distance * 1000 / 32);
            long cycle = 1000 + duration + 1000;
            long phase = (SystemClock.uptimeMillis() - marqueeStartMs) % cycle;
            float shift = phase < 1000 ? 0 : Math.min(distance, (phase - 1000) * 32 / 1000f);
            c.save();
            c.clipRect(x, baseline - size * 1.3f, x + maxWidth, baseline + size * .4f);
            drawRuns(c, s, x - shift, baseline);
            c.restore();
            p.setFakeBoldText(false);
            int left = (int)Math.floor(x-2), top = (int)Math.floor(baseline-size*1.4f);
            int right = (int)Math.ceil(x+maxWidth+2), bottom = (int)Math.ceil(baseline+size*.5f);
            if (!marqueeActive) {
                marqueeLeft=left; marqueeTop=top; marqueeRight=right; marqueeBottom=bottom;
            } else {
                marqueeLeft=Math.min(marqueeLeft,left); marqueeTop=Math.min(marqueeTop,top);
                marqueeRight=Math.max(marqueeRight,right); marqueeBottom=Math.max(marqueeBottom,bottom);
            }
            marqueeActive = true;
        }
        private void centered(Canvas c, String s, float center, float y, float size, int color, boolean bold) {
            p.setFakeBoldText(bold); p.setTextSize(size);
            text(c, s, center - measureText(s)/2, y, size, color, bold, 0);
        }
        private float measureText(String s) {
            float width = 0;
            float baseSize = p.getTextSize();
            for (int offset = 0; offset < s.length();) {
                boolean latin = theme.usesPrimary(s.codePointAt(offset));
                int end = offset + Character.charCount(s.codePointAt(offset));
                while (end < s.length() && theme.usesPrimary(s.codePointAt(end)) == latin)
                    end += Character.charCount(s.codePointAt(end));
                boolean wasBold = p.isFakeBoldText();
                p.setTypeface(latin ? wasBold && theme.boldTypeface != null ?
                        theme.boldTypeface : theme.typeface : theme.nonLatinTypeface);
                p.setTextSize(baseSize * (latin ? theme.primaryScale : 1f));
                if (latin && theme.boldTypeface != null) p.setFakeBoldText(false);
                if (theme.pixelPrimary && latin) p.setFakeBoldText(false);
                width += p.measureText(s, offset, end);
                p.setFakeBoldText(wasBold);
                offset = end;
            }
            p.setTextSize(baseSize);
            return width;
        }
        private void header(Canvas c) {
            boolean mono = theme.layout.equals("mono") || theme.layout.equals("mono3");
            if (mono) fill(c, 0, 0, 480, 38, theme.headerTop);
            else grad(c, 0, 0, 480, 38, theme.headerTop, theme.headerBottom);
            fill(c, 0, 36, 480, mono ? 2 : 1, theme.divider);
            centered(c, title(), 240, 27, theme.pixelPrimary && theme.primaryScale > 1 ? 20 : mono ? 18 : 19, theme.text, true);
            if (page != Page.HOME) text(c, "‹", 12, 29, 28, theme.text, true, 0);
            Intent battery = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = battery == null ? 80 : battery.getIntExtra("level", 80);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f); p.setColor(theme.text);
            c.drawRect(437, 12, 463, 23, p); p.setStyle(Paint.Style.FILL);
            fill(c, 440, 15, Math.max(2, 20 * level / 100), 5, theme.progress);
            fill(c, 465, 15, 2, 5, theme.text);
        }
        private void cover(Canvas c, float x, float y, float size) {
            if (cover != null && !cover.isRecycled()) {
                p.setColor(Color.WHITE); p.setShader(null);
                r.set(x, y, x+size, y+size);
                c.drawBitmap(cover, null, r, p);
            } else {
                c.save();
                c.clipRect(x, y, x + size, y + size);
                grad(c, x, y, size, size, 0xFFE8B58D, 0xFF385C80);
                p.setColor(0xAAFFE0B0); c.drawCircle(x+size*.72f, y+size*.34f, size*.22f, p);
                p.setColor(0xFF2B4868);
                r.set(x-size*.1f, y+size*.7f, x+size*1.1f, y+size*1.18f);
                c.drawOval(r, p);
                text(c, "Y1 MUSIC", x+10, y+25, Math.max(11, size*.08f), Color.WHITE, true, size-20);
                c.restore();
            }
            p.setColor(0x66000000); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1);
            c.drawRect(x, y, x+size, y+size, p); p.setStyle(Paint.Style.FILL);
        }
        private void coverFill(Canvas c, float x, float y, float w, float h) {
            c.save(); c.clipRect(x, y, x+w, y+h);
            if (cover != null && !cover.isRecycled()) {
                float side = Math.min(cover.getWidth(), cover.getHeight());
                float factor = Math.max(w / side, h / side);
                float drawnW = cover.getWidth() * factor, drawnH = cover.getHeight() * factor;
                r.set(x + (w-drawnW)/2, y + (h-drawnH)/2,
                        x + (w+drawnW)/2, y + (h+drawnH)/2);
                p.setColor(Color.WHITE); p.setShader(null);
                c.drawBitmap(cover, null, r, p);
            } else {
                cover(c, x + (w-h)/2, y, h);
            }
            c.restore();
        }
        private void iconMenu(Canvas c) {
            boolean touch = theme.layout.equals("touch");
            if (touch) grad(c,0,38,480,322,0xFF354C65,theme.background);
            else fill(c,0,38,480,322,theme.background);
            int[] colors = touch ? new int[]{0xFF3296C9,0xFFE39B52,0xFF68A6C3,0xFF79A8AA,0xFF8A99A7,0xFF829AC0}
                    : new int[]{0xFF5D9DD0,0xFFDD9650,0xFF62AD7B,0xFF6F9D9D,0xFF7D8AA0,0xFF8796B4};
            for (int i=0; i<HOME.length; i++) {
                int col=i%2, row=i/2;
                float x=24+col*230, y=44+row*104;
                if (i==selection) grad(c,x,y,202,96,theme.selectedTop,theme.selectedBottom);
                else fill(c,x,y,202,96,touch?0x663E536D:theme.headerBottom);
                float ix=x+77, iy=y+5;
                if (touch) {
                    p.setColor(colors[i]); p.setShader(null);
                    r.set(ix,iy,ix+48,iy+48); c.drawRoundRect(r,10,10,p);
                } else fill(c,ix,iy,48,48,colors[i]);
                c.save(); c.translate(ix,iy); c.scale(2f/3f,2f/3f);
                iconGlyph(c,i,0,0); c.restore();
                centered(c,tr(HOME[i]),x+101,y+80,16,
                        i==selection?theme.selectedText:theme.text,true);
            }
        }
        private void iconGlyph(Canvas c, int index, float x, float y) {
            p.setShader(null); p.setColor(Color.WHITE); p.setStyle(Paint.Style.FILL);
            if (index==0) {
                Path triangle=new Path(); triangle.moveTo(x+27,y+19);
                triangle.lineTo(x+27,y+55); triangle.lineTo(x+55,y+37);
                triangle.close(); c.drawPath(triangle,p);
            } else if (index==1) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4);
                c.drawRect(x+19,y+18,x+53,y+53,p);
                c.drawRect(x+26,y+25,x+46,y+46,p);
            } else if (index==2) {
                p.setStrokeWidth(5); c.drawLine(x+42,y+17,x+42,y+48,p);
                c.drawLine(x+42,y+17,x+54,y+21,p);
                p.setStyle(Paint.Style.FILL); c.drawCircle(x+34,y+49,9,p);
            } else if (index==3) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4);
                c.drawRect(x+16,y+28,x+57,y+53,p);
                c.drawLine(x+17,y+28,x+28,y+19,p);
                c.drawLine(x+28,y+19,x+44,y+19,p);
            } else if (index==4) {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4);
                c.drawRect(x+18,y+20,x+49,y+50,p);
                c.drawRect(x+25,y+26,x+56,y+56,p);
            } else {
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4);
                c.drawCircle(x+36,y+36,17,p); c.drawCircle(x+36,y+36,6,p);
                for (int i=0;i<8;i++) {
                    double angle=i*Math.PI/4;
                    c.drawLine(x+36+(float)Math.cos(angle)*20,y+36+(float)Math.sin(angle)*20,
                            x+36+(float)Math.cos(angle)*27,y+36+(float)Math.sin(angle)*27,p);
                }
            }
            p.setStyle(Paint.Style.FILL);
        }
        private void controlIcon(Canvas c, int index, float cx, float cy, int color) {
            p.setShader(null); p.setColor(color); p.setStyle(Paint.Style.FILL);
            p.setStrokeWidth(2.2f); p.setStrokeCap(Paint.Cap.ROUND);
            if (index == 0) {
                if (isPlaying()) {
                    fill(c, cx-7, cy-8, 5, 16, color);
                    fill(c, cx+2, cy-8, 5, 16, color);
                } else {
                    Path triangle = new Path(); triangle.moveTo(cx-6,cy-9);
                    triangle.lineTo(cx-6,cy+9); triangle.lineTo(cx+9,cy);
                    triangle.close(); c.drawPath(triangle,p);
                }
            } else if (index == 1 || index == 2) {
                Path speaker = new Path(); speaker.moveTo(cx-13,cy-4);
                speaker.lineTo(cx-8,cy-4); speaker.lineTo(cx-2,cy-9);
                speaker.lineTo(cx-2,cy+9); speaker.lineTo(cx-8,cy+4);
                speaker.lineTo(cx-13,cy+4); speaker.close(); c.drawPath(speaker,p);
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx+3,cy,cx+13,cy,p);
                if (index == 2) c.drawLine(cx+8,cy-5,cx+8,cy+5,p);
            } else if (index == 3) {
                p.setStyle(Paint.Style.STROKE);
                for (int j=0;j<3;j++) {
                    float x=cx-9+j*9;
                    c.drawLine(x,cy-9,x,cy+9,p);
                }
                p.setStyle(Paint.Style.FILL);
                c.drawCircle(cx-9,cy+2,3,p);
                c.drawCircle(cx,cy-4,3,p);
                c.drawCircle(cx+9,cy+4,3,p);
            } else if (index == 5) {
                text(c, abStartMs < 0 ? "A-B" : abEndMs < 0 ? "A→B" : "A↔B",
                        cx-18, cy+5, 14, color, true, 40);
            } else if (playMode == 0) {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx-12,cy-6,cx+5,cy-6,p);
                c.drawLine(cx-12,cy,cx+5,cy,p);
                c.drawLine(cx-12,cy+6,cx+5,cy+6,p);
                c.drawLine(cx+5,cy-10,cx+12,cy,p);
                c.drawLine(cx+12,cy,cx+5,cy+10,p);
            } else if (playMode == 3) {
                p.setStyle(Paint.Style.STROKE);
                c.drawLine(cx-11,cy-8,cx+10,cy+8,p);
                c.drawLine(cx-11,cy+8,cx+10,cy-8,p);
                c.drawLine(cx+5,cy+8,cx+10,cy+8,p);
                c.drawLine(cx+10,cy+8,cx+10,cy+3,p);
                c.drawLine(cx+5,cy-8,cx+10,cy-8,p);
                c.drawLine(cx+10,cy-8,cx+10,cy-3,p);
            } else {
                p.setStyle(Paint.Style.STROKE);
                r.set(cx-11,cy-8,cx+11,cy+8);
                c.drawArc(r, 210, 240, false, p);
                c.drawLine(cx+9,cy+3,cx+12,cy-2,p);
                c.drawLine(cx+12,cy-2,cx+5,cy-1,p);
                if (playMode == 2) {
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(cx,cy,3,p);
                }
            }
            p.setStyle(Paint.Style.FILL);
        }
        private void sidePanel(Canvas c, boolean colorStyle) {
            fill(c, 0, 38, 480, 322, theme.background);
            int left = colorStyle ? 258 : 228;
            fill(c,left,38,1,322,theme.divider);
            if (!colorStyle) {
                fill(c,left+1,38,480-left-1,322,0xFF263748);
                cover(c,267,60,170);
            } else cover(c,307,77,120);
            if (playingIndex >= 0 && playingIndex < tracks.size()) {
                Library.Track t=tracks.get(playingIndex);
                int color = colorStyle ? theme.text : Color.WHITE;
                int muted = colorStyle ? theme.muted : 0xFFCAD8E3;
                text(c,displayedPlayingTitle(t),left+17,267,17,color,true,480-left-30);
                text(c,metadata(t.artist),left+17,292,13,muted,false,480-left-30);
            } else {
                int color = colorStyle ? theme.text : Color.WHITE;
                centered(c,"Y1 Music",(left+480)/2,272,17,color,true);
                centered(c,tracks.size()+(english ? " songs" : " 曲"),(left+480)/2,297,13,color,false);
            }
        }
        private void hiMdText(Canvas c, BdfFont font, String value, float x, float top,
                float scale, int color, float limit, boolean scroll) {
            if (value == null) value = "";
            float width = font.measure(value, scale);
            float shift = 0;
            if (scroll && limit > 0 && width > limit) {
                float distance = width - limit;
                long duration = (long)(distance * 1000 / 34);
                long cycle = 1000 + duration + 1000;
                long phase = (SystemClock.uptimeMillis() - marqueeStartMs) % cycle;
                shift = phase < 1000 ? 0 : Math.min(distance, (phase - 1000) * 34 / 1000f);
                int left = (int)x, right = (int)(x + limit);
                int y = (int)top, bottom = (int)(top + 13 * scale + 2);
                if (!marqueeActive) {
                    marqueeLeft = left; marqueeTop = y;
                    marqueeRight = right; marqueeBottom = bottom;
                } else {
                    marqueeLeft = Math.min(marqueeLeft, left);
                    marqueeTop = Math.min(marqueeTop, y);
                    marqueeRight = Math.max(marqueeRight, right);
                    marqueeBottom = Math.max(marqueeBottom, bottom);
                }
                marqueeActive = true;
            }
            c.save();
            if (limit > 0) c.clipRect(x, top, x + limit, top + 13 * scale + 2);
            font.draw(c, value, x - shift, top, scale, color);
            c.restore();
        }
        private void hiMdFrame(Canvas c, String heading) {
            fill(c, 0, 0, 480, 360, 0xFF02070D);
            fill(c, 10, 8, 460, 344, 0xFF06111E);
            fill(c, 15, 13, 450, 334, 0xFF02070D);
            p.setShader(null); p.setColor(0xFF23618D);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
            c.drawRect(15, 13, 465, 347, p);
            c.drawLine(23, 47, 149, 47, p);
            c.drawLine(149, 47, 176, 22, p);
            c.drawLine(176, 22, 455, 22, p);
            p.setStyle(Paint.Style.FILL);
            Path playMark = new Path();
            playMark.moveTo(32, 22); playMark.lineTo(32, 39);
            playMark.lineTo(54, 30); playMark.close();
            p.setColor(0xFF49D8FF); c.drawPath(playMark, p);
            hiMdText(c, hiMdFont10, heading, 194, 27, 1.5f,
                    0xFFC4E9FF, 224, false);
            fill(c, 24, 318, 432, 1, 0xFF23618D);
            hiMdText(c, hiMdFont10, "Hi-MD", 27, 323, 1.2f,
                    0xFF49D8FF, 95, false);
            // The battery and line icons are original vector drawings.
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2);
            p.setColor(0xFFC4E9FF); c.drawRect(418, 326, 447, 336, p);
            p.setStyle(Paint.Style.FILL);
            fill(c, 447, 329, 3, 4, 0xFFC4E9FF);
            fill(c, 420, 328, 20, 6, 0xFF49D8FF);
        }
        private void hiMdMenu(Canvas c) {
            hiMdFrame(c, title());
            int total = count(), visible = 7;
            int first = visibleStart(visible, total);
            for (int i = first; i < total && i < first + visible; i++) {
                int y = 53 + (i - first) * 37;
                if (i == selection) fill(c, 25, y - 2, 430, 35, 0xFF0A3151);
                fill(c, 25, y + 34, 430, 1, 0xFF164462);
                if (i == selection) {
                    Path cursor = new Path();
                    cursor.moveTo(30, y + 9); cursor.lineTo(30, y + 24);
                    cursor.lineTo(40, y + 16); cursor.close();
                    p.setColor(0xFF49D8FF); c.drawPath(cursor, p);
                } else fill(c, 33, y + 13, 8, 8, 0xFF287AA8);
                hiMdText(c, hiMdFont12, item(i), 53, y + 5, 1.65f,
                        i == selection ? 0xFFE7FAFF : 0xFFAED4EC,
                        388, i == selection);
            }
            if (page == Page.STORAGE) {
                StatFs volume = new StatFs(Environment.getExternalStorageDirectory().getPath());
                long size = volume.getBlockCount() * (long)volume.getBlockSize();
                long free = volume.getAvailableBlocks() * (long)volume.getBlockSize();
                hiMdText(c, hiMdFont12, tr("総容量") + " " + gigabytes(size) + " GB",
                        38, 91, 1.7f, theme.text, 392, false);
                hiMdText(c, hiMdFont12, tr("空き容量") + " " + gigabytes(free) + " GB",
                        38, 138, 1.7f, theme.text, 392, false);
            } else if (page == Page.SHUTDOWN) {
                hiMdText(c, hiMdFont12, tr("本体の電源を切りますか？"),
                        38, 180, 1.7f, theme.text, 390, false);
            } else if (total == 0) {
                hiMdText(c, hiMdFont12,
                        tr(loading || directoryLoading && page == Page.DIRECTORY ? "音楽を読み込み中…" : "音楽ファイルがありません"),
                        38, 95, 1.7f, theme.muted, 392, false);
            }
            String status = message.length() > 0 ? message :
                    (page == Page.SONGS ? total + (english ? " tracks" : " 曲") : "MZ-DH10P");
            hiMdText(c, hiMdFont10, status, 142, 323, 1.1f,
                    0xFF75A8C7, 250, false);
        }
        private void hiMdPlaying(Canvas c) {
            hiMdFrame(c, "");
            if (playingIndex < 0 || playingIndex >= tracks.size()) {
                hiMdText(c, hiMdFont12, tr("曲を選んでください"), 45, 130,
                        1.8f, theme.text, 390, false);
                return;
            }
            Library.Track track = tracks.get(playingIndex);
            int elapsed = (int)Math.min(Integer.MAX_VALUE, playbackPositionMs());
            int duration = (int)track.duration;
            try {
                if (player != null) duration = player.getDuration();
                else if (vlcPlayer != null && vlcPlayer.getLength() > 0)
                    duration = (int)Math.min(Integer.MAX_VALUE, vlcPlayer.getLength());
            } catch (Exception ignored) {}
            hiMdText(c, hiMdFont12,
                    String.format(java.util.Locale.US, "%02d", playingIndex + 1),
                    239, 62, 2.3f, 0xFFE7FAFF, 65, false);
            int elapsedSeconds = Math.max(0, elapsed / 1000);
            String hiMdTime = String.format(java.util.Locale.US, "%02d:%02d",
                    elapsedSeconds / 60, elapsedSeconds % 60);
            hiMdText(c, hiMdFont12, hiMdTime, 100, 108, 2.3f,
                    0xFFE7FAFF, 198, false);
            fill(c, 317, 55, 135, 135, 0xFF3E6D8B);
            fill(c, 322, 60, 125, 125, 0xFFE7FAFF);
            cover(c, 322, 65, 125);
            fill(c, 323, 181, 123, 4, 0xFFE7FAFF);
            float ratio = duration > 0 ? Math.max(0, Math.min(1, elapsed/(float)duration)) : 0;
            fill(c, 42, 187, 248, 3, 0xFF23618D);
            fill(c, 42, 187, 248 * ratio, 3, 0xFF49D8FF);
            fill(c, 42 + 246 * ratio, 184, 3, 9, 0xFFE7FAFF);
            fill(c, 25, 204, 430, 1, 0xFF23618D);
            fill(c, 25, 251, 430, 1, 0xFF23618D);
            fill(c, 25, 297, 430, 1, 0xFF23618D);
            hiMdText(c, hiMdFont12, "♪", 30, 215, 1.65f, 0xFF49D8FF, 23, false);
            hiMdText(c, hiMdFont12, displayedPlayingTitle(track), 61, 213, 1.75f,
                    0xFFE7FAFF, 382, true);
            hiMdText(c, hiMdFont12, "●", 31, 262, 1.5f, 0xFF49D8FF, 25, false);
            hiMdText(c, hiMdFont12, metadata(track.artist), 61, 260, 1.75f,
                    0xFFAED4EC, 382, true);
            int level = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            int max = Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            String status = time(elapsed) + "/" + time(duration) + "  " +
                    tr("音量") + (level + boostLevel) + "/" + max + "  " + playModeText();
            hiMdText(c, hiMdFont10, status, 129, 324, 1.1f,
                    boostLevel > 0 ? 0xFFFF7777 : 0xFF75A8C7, 273, false);
            fill(c, 0, 338, 480, 22, 0xFF06111E);
            for (int i = 0; i < 6; i++) {
                int x = i * 80;
                if (i == selection) fill(c, x + 2, 339, 76, 20, 0xFF0A3151);
                controlIcon(c, i, x + 40, 348,
                        i == selection ? 0xFF49D8FF : 0xFF75A8C7);
            }
        }
        private void winampMenu(Canvas c) {
            skin(c, winampMenuArt);
            int total = count(), visible = 8;
            int first = visibleStart(visible, total);
            for (int i = first; i < total && i < first + visible; i++) {
                int y = 38 + (i - first) * 32;
                if (i == selection) fill(c, 20, y, 440, 29, 0xFF101B34);
                int color = i == selection ? 0xFF00E900 : 0xFFC9D3C3;
                text(c, String.format(java.util.Locale.US, "%02d.", i + 1),
                        24, y + 21, 15, i == selection ? 0xFFF5CA55 : 0xFFA6B2AA, true, 40);
                String label = item(i);
                if (i == selection) marqueeText(c, label, 75, y + 21, 17, color, true, 365);
                else text(c, label, 75, y + 21, 17, color, false, 365);
            }
            if (page == Page.STORAGE) {
                StatFs volume = new StatFs(Environment.getExternalStorageDirectory().getPath());
                long size = volume.getBlockCount() * (long)volume.getBlockSize();
                long free = volume.getAvailableBlocks() * (long)volume.getBlockSize();
                text(c, tr("総容量") + "  " + gigabytes(size) + " GB", 31, 89, 19, theme.text, false, 425);
                text(c, tr("空き容量") + "  " + gigabytes(free) + " GB", 31, 127, 19, theme.text, false, 425);
            } else if (page == Page.USB_TRANSFER && total == 0) {
                text(c, tr("USBケーブルを接続して開始します"), 31, 92, 17, theme.text, false, 420);
            } else if (page == Page.SHUTDOWN) {
                text(c, tr("本体の電源を切りますか？"), 31, 154, 17, theme.text, false, 420);
            } else if (total == 0) {
                text(c, tr(loading || directoryLoading && page == Page.DIRECTORY ? "音楽を読み込み中…" : "音楽ファイルがありません"),
                        31, 92, 17, theme.muted, false, 420);
            }
            String footer = message.length() > 0 ? message : title();
            winampFooterText(c, footer);
        }
        private void winampFooterText(Canvas c, String value) {
            // The skin's LCD opening is y=308..324. Keep text inside it.
            c.save();
            c.clipRect(229, 308, 390, 325);
            text(c, value, 229, 321, 12, 0xFF00E900, false, 155);
            c.restore();
        }
        private void winampPlaying(Canvas c) {
            skin(c, winampPlayerArt);
            if (playingIndex < 0 || playingIndex >= tracks.size()) {
                text(c, tr("曲を選んでください"), 198, 58, 17, 0xFF00E900, true, 260);
                return;
            }
            Library.Track track = tracks.get(playingIndex);
            int elapsed = (int)Math.min(Integer.MAX_VALUE, playbackPositionMs());
            int duration = (int)track.duration;
            try {
                if (player != null) duration = player.getDuration();
                else if (vlcPlayer != null && vlcPlayer.getLength() > 0)
                    duration = (int)Math.min(Integer.MAX_VALUE, vlcPlayer.getLength());
            } catch (Exception ignored) {}
            marqueeText(c, displayedPlayingTitle(track), 198, 57, 16,
                    0xFF00E900, true, 258);
            text(c, time(elapsed), 61, 85, 38, 0xFF00E900, true, 113);
            int level = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            int max = Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            text(c, tr("音量") + " " + (level + boostLevel) + "/" + max,
                    310, 100, 13, boostLevel > 0 ? 0xFFFF7777 : 0xFF00E900, false, 150);
            text(c, playModeText(), 310, 118, 12, 0xFFB7EE8F, false, 150);
            float ratio = duration > 0 ? Math.max(0, Math.min(1, elapsed/(float)duration)) : 0;
            fill(c, 51, 128, 373 * ratio, 4, 0xFFF5CA55);
            fill(c, 19, 242, 441, 28, 0xFF101B34);
            text(c, "01.", 23, 262, 15, 0xFFF5CA55, true, 40);
            marqueeText(c, displayedPlayingTitle(track), 75, 262, 16,
                    0xFF00E900, true, 335);
            text(c, "02.", 23, 291, 15, 0xFFA6B2AA, true, 40);
            text(c, metadata(track.artist), 75, 291, 15, theme.text, false, 335);
            winampFooterText(c, time(elapsed) + "/" + time(duration));
            // The device's wheel focuses six actions; the original transport
            // sprite is decorative, and this row exposes the actual bindings.
            fill(c, 0, 333, 480, 27, 0xFF29293D);
            for (int i = 0; i < 6; i++) {
                int x = i * 80;
                fill(c, x + 2, 335, 76, 23, i == selection ? 0xFF3E3C60 : 0xFF202033);
                if (i == selection) fill(c, x + 4, 355, 72, 2, 0xFF00E900);
                controlIcon(c, i, x + 40, 345.5f,
                        i == selection ? 0xFF00E900 : 0xFFC9D3C3);
            }
        }
        private void shutdownText(Canvas c, String value, float baseline, float size, int color) {
            if (theme.layout.equals("himd")) {
                float scale = size / 12f;
                float width = hiMdFont12.measure(value, scale);
                hiMdText(c, hiMdFont12, value, 240-width/2, baseline-size,
                        scale, color, width+2, false);
            } else centered(c, value, 240, baseline, size, color, true);
        }
        private void shutdownConfirmation(Canvas c) {
            if (theme.layout.equals("winamp")) skin(c, winampMenuArt);
            else if (theme.layout.equals("himd")) hiMdFrame(c, title());
            else fill(c, 0, 38, 480, 322, theme.background);

            // Separate the question and actions from ordinary list rows.
            boolean update = page == Page.APK_CONFIRM;
            shutdownText(c, tr(update ? "アプリを更新しますか？" : "本体の電源を切りますか？"), update ? 84 : 110, 22, theme.text);
            if (update && apkPrepared != null) {
                shutdownText(c, apkPrepared.name, 115, 15, theme.text);
                shutdownText(c, apkPrepared.currentVersion + " → " + apkPrepared.version, 142, 17, theme.muted);
            }
            for (int i=0; i<2; i++) {
                float top = 157 + i*72;
                int color = i == selection ? theme.selectedText : theme.text;
                if (i == selection)
                    grad(c, 40, top, 400, 54, theme.selectedTop, theme.selectedBottom);
                else {
                    fill(c, 40, top, 400, 54, theme.background);
                    p.setShader(null); p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(1); p.setColor(theme.divider);
                    c.drawRect(40, top, 440, top+54, p);
                    p.setStyle(Paint.Style.FILL);
                }
                shutdownText(c, tr(i == 0 ? "キャンセル" : update ? "インストール" : "電源を切る"), top+35, 22, color);
            }
            if (theme.layout.equals("winamp")) winampFooterText(c, title());
        }
        private void menu(Canvas c) {
            if (page == Page.APK_INSTALLING) {
                fill(c, 0, 38, 480, 322, theme.background);
                shutdownText(c, tr("アプリを更新しています…"), 128, 22, theme.text);
                shutdownText(c, tr("電源を切らずにお待ちください"), 185, 18, theme.muted);
                return;
            }
            if (page == Page.SHUTDOWN || page == Page.APK_CONFIRM) { shutdownConfirmation(c); return; }
            if (theme.layout.equals("winamp")) { winampMenu(c); return; }
            if (theme.layout.equals("himd")) { hiMdMenu(c); return; }
            int total = count();
            if (page == Page.EQ_BANDS) { equalizerBands(c); return; }
            boolean icons = page == Page.HOME && (theme.layout.equals("grid") || theme.layout.equals("touch"));
            if (icons) { iconMenu(c); return; }
            boolean settingsPage = page == Page.SETTINGS || page == Page.LANGUAGE || page == Page.OPTIONS || page == Page.BLUETOOTH ||
                    page == Page.STORAGE || page == Page.USB_TRANSFER || page == Page.SHUTDOWN || page == Page.APK_FILES;
            boolean side = !settingsPage && (theme.layout.equals("split") || theme.layout.equals("color"));
            if (side) sidePanel(c,theme.layout.equals("color"));
            else {
                fill(c,0,38,480,322,theme.background);
                if (theme.layout.equals("artfull") && page == Page.HOME) {
                    coverFill(c,250,38,230,292);
                    fill(c,0,38,248,292,theme.background);
                }
            }
            int row = theme.rowHeight;
            int visible = Math.max(1, 318 / row);
            int first = visibleStart(visible, total);
            for (int i = first; i < total && i < first + visible; i++) {
                float y = 42 + (i-first)*row;
                float width = settingsPage ? 480 : theme.layout.equals("split") ? 228 :
                        theme.layout.equals("color") ? 258 :
                        theme.layout.equals("artfull") && page == Page.HOME ? 248 : 480;
                if (i == selection) {
                    if (theme.layout.equals("mono") || theme.layout.equals("mono3"))
                        fill(c,0,y,width,row,theme.selectedBottom);
                    else grad(c, 0, y, width, row, theme.selectedTop, theme.selectedBottom);
                } else fill(c, 0, y+row-1, width, 1, theme.divider);
                int color = i == selection ? theme.selectedText : theme.text;
                boolean songRow = page == Page.SONGS || page == Page.ALBUM_SONGS ||
                        page == Page.DIRECTORY && i >= directoryFolders.size();
                String detail = songRow ? subtitle(i) : "";
                float baseline = detail.length() > 0 ? y + row*(theme.pixelPrimary && theme.primaryScale > 1 ? .46f : .49f) : y + row*.66f;
                if (i == selection && (songRow || settingsPage || page == Page.DIRECTORY))
                    marqueeText(c, item(i), 13, baseline, theme.fontSize, color, true, width-28);
                else text(c, item(i), 13, baseline, theme.fontSize, color, true, width-28);
                if (detail.length() > 0)
                    text(c, detail, 13, y + row*(theme.pixelPrimary && theme.primaryScale > 1 ? .91f : .86f), Math.max(11, theme.fontSize-5),
                            i == selection ? color : theme.muted, false, width-28);
                if (page == Page.HOME || page == Page.ALBUMS ||
                        page == Page.DIRECTORY && i < directoryFolders.size() ||
                        page == Page.APK_FILES && i < apkEntries.size() && apkEntries.get(i).isDirectory())
                    text(c, "›", width-26, y+row*.67f, 25, color, true, 0);
            }
            if (page == Page.DIRECTORY && total == 0 && !directoryLoading && !loading)
                centered(c,tr("このフォルダに曲はありません"),240,185,18,theme.muted,false);
            if (page == Page.STORAGE) {
                StatFs volume = new StatFs(Environment.getExternalStorageDirectory().getPath());
                long blockSize = volume.getBlockSize();
                long totalBytes = volume.getBlockCount()*blockSize;
                long freeBytes = volume.getAvailableBlocks()*blockSize;
                text(c, tr("音楽領域"), 20, 88, 20, theme.text, true, 440);
                text(c, tr("総容量") + "  " + gigabytes(totalBytes) + " GB", 20, 132, 18, theme.text, false, 440);
                text(c, tr("空き容量") + "  " + gigabytes(freeBytes) + " GB", 20, 172, 18, theme.text, false, 440);
                text(c, Environment.getExternalStorageDirectory().getPath(), 20, 225, 15, theme.muted, false, 440);
            } else if (page == Page.USB_TRANSFER) {
                if (checkCallingOrSelfPermission("android.permission.MOUNT_UNMOUNT_FILESYSTEMS") !=
                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    text(c, tr("USB転送にはsystem版が必要です"), 20, 183, 18, theme.text, true, 440);
                } else if (usbStorageActive()) {
                    text(c, tr("PC側でディスクを取り出してから"), 20, 183, 18, theme.text, true, 440);
                    text(c, tr("USBストレージを終了してください"), 20, 213, 18, theme.text, false, 440);
                } else {
                    text(c, tr("USBケーブルを接続して開始します"), 20, 183, 18, theme.text, true, 440);
                    text(c, tr("PCに音楽領域が表示されます"), 20, 213, 18, theme.text, false, 440);
                }
            } else if (page == Page.SHUTDOWN) {
                text(c, tr("本体の電源を切りますか？"), 18, 85, 19, theme.text, true, 440);
            } else if (total == 0) {
                String empty = tr(loading || directoryLoading && page == Page.DIRECTORY ? "音楽を読み込み中…" : "音楽ファイルがありません");
                centered(c, empty, side ? (theme.layout.equals("split") ? 114 : 129) : 240,
                        180, 17, theme.muted, false);
            }
            if (message.length() > 0) {
                fill(c, 0, 0, 480, 38, theme.headerBottom);
                text(c, message, 10, 26, 14, theme.text, false, 455);
            }
        }
        private void equalizerBands(Canvas c) {
            fill(c, 0, 38, 480, 322, theme.background);
            int first = Math.max(0, Math.min(selection - 2, eqBands.length - 5));
            for (int i=first; i<Math.min(eqBands.length, first+5); i++) {
                int y = 54 + (i-first)*46;
                if (i == selection) grad(c, 9, y-14, 462, 43,
                        theme.selectedTop, theme.selectedBottom);
                int color = i == selection ? theme.selectedText : theme.text;
                float hz = org.videolan.libvlc.MediaPlayer.Equalizer.getBandFrequency(i);
                String frequency = hz >= 1000 ? String.format(java.util.Locale.US, "%.1f kHz", hz/1000f)
                        : String.format(java.util.Locale.US, "%.0f Hz", hz);
                text(c, frequency, 18, y+12, 17, color, true, 100);
                fill(c, 130, y+4, 240, 8, theme.divider);
                fill(c, 249, y, 2, 16, color);
                float amount = eqBands[i] * 10;
                fill(c, amount < 0 ? 250+amount : 250, y+4, Math.abs(amount), 8,
                        theme.progress);
                text(c, String.format(java.util.Locale.US, "%+.0f dB", eqBands[i]),
                        390, y+12, 16, color, true, 80);
            }
            text(c, tr(editingBand ? "回転: ±1 dB  中央: 確定" : "回転: 帯域選択  中央: 調整"),
                    14, 302, 15, theme.text, false, 450);
            text(c, tr("前曲/次曲: ±1 dB    MENU: 戻る"), 14, 329, 14, theme.muted, false, 450);
        }
        private void playing(Canvas c) {
            if (theme.layout.equals("winamp")) { winampPlaying(c); return; }
            if (theme.layout.equals("himd")) { hiMdPlaying(c); return; }
            fill(c, 0, 38, 480, 322, theme.background);
            if (playingIndex < 0 || playingIndex >= tracks.size()) {
                centered(c, tr("曲を選んでください"), 240, 180, 20, theme.text, true); return;
            }
            Library.Track t = tracks.get(playingIndex);
            if (theme.layout.equals("mono") || theme.layout.equals("mono3")) {
                boolean first = theme.layout.equals("mono");
                fill(c,12,50,456,first?2:1,theme.divider);
                p.setTextSize(24); p.setFakeBoldText(true);
                if (measureText(displayedPlayingTitle(t))>420)
                    marqueeText(c,displayedPlayingTitle(t),30,first?111:101,24,theme.text,true,420);
                else centered(c,displayedPlayingTitle(t),240,first?111:101,24,theme.text,true);
                centered(c,metadata(t.artist),240,first?151:145,18,theme.text,false);
                centered(c,metadata(t.album),240,first?183:181,17,theme.muted,false);
                centered(c,queuePosition(),240,223,15,theme.muted,false);
                centered(c,tr(isPlaying() ? "▶ 再生中" : "Ⅱ 一時停止"),
                        240,265,17,theme.text,true);
            } else if (theme.layout.equals("artfull")) {
                coverFill(c,0,38,480,295);
                grad(c,0,38,480,70,0xDD101820,0x00101820);
                grad(c,0,205,480,128,0x00101820,0xEE101820);
                marqueeText(c,displayedPlayingTitle(t),23,246,23,Color.WHITE,true,435);
                text(c,metadata(t.artist),23,272,16,Color.WHITE,false,315);
                text(c,queuePosition(),398,272,13,Color.WHITE,false,70);
            } else if (theme.layout.equals("grid")) {
                cover(c,170,53,140);
                marqueeText(c,displayedPlayingTitle(t),53,222,21,theme.text,true,374);
                centered(c,metadata(t.artist),240,251,16,theme.muted,false);
                centered(c,queuePosition(),240,265,14,theme.muted,false);
            } else if (theme.layout.equals("touch")) {
                grad(c,0,38,480,277,0xFF283D52,theme.background);
                cover(c,42,65,190);
                marqueeText(c,displayedPlayingTitle(t),260,112,21,theme.text,true,200);
                text(c,metadata(t.artist),260,145,16,theme.muted,false,195);
                text(c,metadata(t.album),260,174,14,theme.muted,false,195);
                text(c,tr(isPlaying()?"▶ 再生中":"Ⅱ 一時停止"),
                        260,229,16,theme.progress,true,190);
                text(c,queuePosition(),260,258,14,theme.muted,false,120);
            } else if (theme.layout.equals("split") || theme.layout.equals("color")) {
                cover(c, 28, 63, 214);
                marqueeText(c, displayedPlayingTitle(t), 264, 111, 22, theme.text, true, 195);
                text(c, metadata(t.artist), 264, 143, 17, theme.muted, false, 195);
                text(c, metadata(t.album), 264, 169, 15, theme.muted, false, 195);
                text(c, tr(isPlaying() ? "▶ 再生中" : "Ⅱ 一時停止"),
                        264, 233, 17, theme.progress, true, 190);
                text(c, queuePosition(), 264, 258, 14, theme.muted, false, 190);
            } else {
                cover(c, 72, 70, 153);
                marqueeText(c, displayedPlayingTitle(t), 248, 107, 22, theme.text, true, 215);
                text(c, metadata(t.artist), 248, 139, 17, theme.muted, false, 215);
                text(c, metadata(t.album), 248, 165, 15, theme.muted, false, 215);
                text(c, queuePosition(), 248, 211, 14, theme.muted, false, 190);
                text(c, isPlaying() ? "▶" : "Ⅱ",
                        248, 244, 23, theme.progress, true, 40);
            }
            int elapsed = 0, duration = (int)t.duration;
            try {
                if (player != null) { elapsed = player.getCurrentPosition(); duration = player.getDuration(); }
                else if (vlcPlayer != null) {
                    elapsed = (int)Math.max(0, vlcPlayer.getTime());
                    long length = vlcPlayer.getLength();
                    if (length > 0) duration = (int)Math.min(Integer.MAX_VALUE,length);
                }
            } catch (Exception ignored) {}
            float ratio = duration > 0 ? Math.min(1, elapsed / (float)duration) : 0;
            fill(c, 56, 295, 368, 7, theme.divider);
            fill(c, 56, 295, 368*ratio, 7, theme.progress);
            text(c, time(elapsed), 56, 326, 14, theme.muted, false, 50);
            text(c, time(duration), 387, 326, 14, theme.muted, false, 50);
            int level = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            int max = Math.max(1, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            if (boostLevel > 0 && level < max) {
                boostLevel = 0;
                if (vlcPlayer != null) vlcPlayer.setVolume(100);
            }
            boolean splitMode = theme.layout.equals("split") || theme.layout.equals("color");
            if (message.length() > 0)
                text(c, message, splitMode ? 264 : 12, 283, 13, theme.text, false,
                        splitMode ? 195 : 230);
            boolean leftVolume = theme.layout.equals("mono") || theme.layout.equals("mono3") ||
                    theme.layout.equals("grid");
            int background = theme.background;
            int brightness = (Color.red(background)*3 + Color.green(background)*6 +
                    Color.blue(background))/10;
            int boostColor = brightness < 130 ? 0xFFFF7777 : 0xFFB00020;
            if (message.length() == 0 || !leftVolume && !splitMode)
                text(c, tr("音量") + " " + (level + boostLevel) + "/" + max,
                        leftVolume ? 12 : 263, 283, 14,
                        boostLevel > 0 ? boostColor : theme.muted, boostLevel > 0, 140);
            boolean classicMode = theme.layout.equals("classic");
            boolean artMode = theme.layout.equals("artfull");
            if (artMode) fill(c, 16, 194, 218, 29, 0x88000000);
            if (message.length() == 0 || splitMode || classicMode || artMode || leftVolume)
                text(c, abStartMs < 0 ? tr("再生種類:") + " " + playModeText() : playModeText(),
                        splitMode ? 264 : classicMode ? 248 : artMode ? 23 : leftVolume ? 263 : 12,
                        splitMode || classicMode ? 197 : artMode ? 214 : 283, 14,
                        artMode ? Color.WHITE : theme.text, true, 203);
            for (int i = 0; i < 6; i++) {
                float x = i * 80;
                if (i == selection) grad(c, x, 333, 80, 27, theme.selectedTop, theme.selectedBottom);
                else fill(c, x, 333, 79, 27, theme.headerBottom);
                controlIcon(c, i, x + 40, 346.5f,
                        i == selection ? theme.selectedText : theme.text);
            }
        }
        private String time(int ms) {
            int seconds = Math.max(0, ms / 1000);
            return (seconds/60)+":"+(seconds%60<10?"0":"")+(seconds%60);
        }
        private String gigabytes(long bytes) {
            return String.format(java.util.Locale.JAPAN, "%.2f", bytes / 1000000000.0);
        }
        @Override protected void onDraw(Canvas real) {
            super.onDraw(real);
            if (!message.equals(observedMessage)) {
                observedMessage = message;
                removeCallbacks(clearTransientMessage);
                if (message.length() > 0 && !persistentMessage() && !message.equals(seekMessage))
                    postDelayed(clearTransientMessage, 2500);
            }
            if (theme == null) return;
            marqueeActive = false;
            real.save();
            real.scale(getWidth()/480f, getHeight()/360f);
            if (!theme.layout.equals("winamp") && !theme.layout.equals("himd")) header(real);
            if (page == Page.PLAYER) playing(real); else menu(real);
            if (loading || directoryLoading && page == Page.DIRECTORY) {
                fill(real, 112, 4, 256, 29, 0xEE071726);
                centered(real, loading && !tracks.isEmpty() ?
                        (english ? "Checking music changes..." : "音楽の変更を確認中…") :
                        (english ? "Scanning music..." : "音楽をスキャン中…"),
                        240, 24, 16, 0xFFBCEBFF, true);
            }
            real.restore();
            if (!refreshScheduled && (marqueeActive || page == Page.PLAYER && isPlaying())) {
                refreshScheduled = true;
                postDelayed(refresh, marqueeActive ? 125 : 500);
            }
        }
    }
}
