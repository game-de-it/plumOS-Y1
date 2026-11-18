package com.kroot.y1themes;

import android.content.Context;
import java.util.HashMap;

final class UiText {
    private static final HashMap<String,String> EN = new HashMap<String,String>();
    static {
        add("アプリ更新には対応FWが必要です", "Player update needs compatible firmware");
        add("アプリを更新できません", "Could not update player");
        add("アプリを更新しています…", "Updating the player…");
        add("電源を切らずにお待ちください", "Please wait; keep power on");
        add("アプリを更新", "Update player");
        add("親フォルダへ戻る", "Parent folder");
        add("設定へ戻る", "Back to settings");
        add("戻る", "Back");
        add("インストール", "Install");
        add("アプリを更新しますか？", "Update the player?");
        add("APKを読み込み中…", "Loading APK files…");
        add("APKがありません", "No APK files found");
        add("APKを選択してください", "Select an APK file");
        add("APKを確認しています…", "Checking APK…");
        add("APKを読み込めません", "Cannot read APK");
        add("インストーラーを開けません", "Cannot open installer");
        add("アプリを更新しました", "Player updated");
        add("インストールを終了しました", "Installation finished");
        add("更新フォルダを開けません", "Cannot open update folder");
        add("音楽領域を確認してください", "Check music storage");
        add("APKのサイズが不正です", "Invalid APK size");
        add("このアプリの更新APKではありません", "Not a player update APK");
        add("APKの署名が一致しません", "APK signature does not match");
        add("古いバージョンには更新できません", "Cannot install an older version");
        add("再生中", "Now Playing"); add("アルバム", "Albums"); add("曲", "Songs");
        add("ディレクトリ", "Directories");
        add("音楽を再スキャン", "Rescan music");
        add("キャッシュ削除・再構築", "Clear and rebuild cache");
        add("差分を確認しています", "Checking for changes");
        add("キャッシュを再構築しています", "Rebuilding cache");
        add("このフォルダに曲はありません", "No songs in this folder");
        add("テーマ", "Themes"); add("設定", "Settings"); add("ミュージック", "Music");
        add("イコライザー", "Equalizer"); add("オフ", "Off"); add("フラット", "Flat");
        add("低音", "Bass"); add("ボーカル", "Vocal"); add("明瞭", "Clarity");
        add("温かみ", "Warm"); add("カスタム調整", "Custom");
        add("順番", "Sequential"); add("リピート全曲", "Repeat All");
        add("リピート1曲", "Repeat One"); add("シャッフル", "Shuffle");
        add("ファイル名で表示", "File names"); add("タグの曲名で表示", "Tag titles");
        add("シャットダウン", "Shut down"); add("シャットダウンタイマー", "Sleep timer");
        add("画面タイムアウト", "Screen timeout"); add("クリック音", "Click sound");
        add("バイブ", "Vibration"); add("画面輝度", "Brightness");
        add("FMラジオ", "FM radio"); add("Android設定", "Android settings");
        add("日付と時刻", "Date & time"); add("ディスク容量", "Disk space");
        add("言語", "Language"); add("日本語", "Japanese");
        add("エアプレーンモード", "Airplane mode");
        add("USBファイル転送", "USB file transfer");
        add("USBストレージを開始", "Turn on USB storage");
        add("USBストレージを終了", "Turn off USB storage");
        add("USBケーブルを接続してください", "Connect a USB cable");
        add("USB接続を確認できません", "Cannot check USB connection");
        add("USB転送にはsystem版が必要です", "USB transfer needs system version");
        add("USB切替に失敗しました", "USB switch failed");
        add("切り替え中…", "Switching…");
        add("PCにディスクが表示されます", "The disk is available on your computer");
        add("転送完了", "Transfer finished");
        add("PCで取り出してから終了してください", "Eject on your computer before turning off");
        add("PC側でディスクを取り出してから", "Eject the disk on your computer,");
        add("USBストレージを終了してください", "then turn off USB storage");
        add("USBケーブルを接続して開始します", "Connect USB, then turn on storage");
        add("PCに音楽領域が表示されます", "The music disk appears on your computer");
        add("エアプレーンモードON", "Airplane mode on");
        add("エアプレーンモードOFF", "Airplane mode off");
        add("エアプレーンモードを変更できません", "Cannot change airplane mode");
        add("FMにはエアプレーンモードOFFが必要です", "Turn airplane mode off for FM");
        add("FMにはイヤホンが必要な場合があります", "FM may need wired earphones");
        add("バイブに対応していません", "Vibration unavailable");
        add("クリック音を変更できません", "Cannot change click sound");
        add("検索中…", "Searching…"); add("新しい機器を探す", "Search for devices");
        add("接続中", "Connected"); add("ペア済み", "Paired");
        add("ペアリング", "Pair"); add("キャンセル", "Cancel");
        add("電源を切る", "Power off"); add("分割画面", "Split view");
        add("リスト画面", "List view"); add("シャットダウン確認", "Confirm shutdown");
        add("先に曲を選んでください", "Choose a song first");
        add("を適用", " applied"); add("ファイル名順に変更", "Sorted by file name");
        add("曲名タグ順に変更", "Sorted by tag title");
        add("タイマーにはsystem版が必要です", "Sleep timer needs system version");
        add("Bluetooth非搭載", "Bluetooth unavailable");
        add("BluetoothをONにしてください", "Turn Bluetooth on");
        add("画面を開けません", "Cannot open screen");
        add("FMラジオを開けません", "Cannot open FM radio");
        add("シャットダウンにはsystem版が必要です", "Shutdown needs system version");
        add("電源操作に失敗しました", "Power operation failed");
        add("ペアリングを開始しました", "Pairing started");
        add("ペアリングできません", "Pairing failed");
        add("ペアリングを開始できません", "Cannot start pairing");
        add("Bluetooth準備中", "Bluetooth is starting");
        add("接続を要求しました", "Connection requested");
        add("接続できません", "Connection failed");
        add("Bluetooth設定から接続してください", "Connect in Bluetooth settings");
        add("読み込み中", "Loading…"); add("再生できません", "Cannot play file");
        add("LibVLCで再生できません: ", "LibVLC cannot play: ");
        add("再生終了", "Playback finished"); add("シークできません", "Cannot seek");
        add("早送り ", "Forward "); add("巻き戻し ", "Rewind ");
        add("音楽領域", "Music storage"); add("総容量", "Total");
        add("空き容量", "Free"); add("本体の電源を切りますか？", "Power off the player?");
        add("音楽を読み込み中…", "Scanning music…");
        add("音楽ファイルがありません", "No music files found");
        add("曲を選んでください", "Choose a song");
        add("▶ 再生中", "▶ Playing"); add("Ⅱ 一時停止", "Ⅱ Paused");
        add("音量", "Volume"); add("再生種類:", "Play mode:");
        add("回転: ±1 dB  中央: 確定", "Wheel: ±1 dB  Center: done");
        add("回転: 帯域選択  中央: 調整", "Wheel: select band  Center: adjust");
        add("前曲/次曲: ±1 dB    MENU: 戻る", "Prev/Next: ±1 dB   MENU: back");
        add("輝度", "Brightness"); add("不明なアーティスト", "Unknown artist");
        add("不明なアルバム", "Unknown album");
        add("を追加しました", " added"); add("テーマ追加に失敗: ", "Theme install failed: ");
    }
    private static void add(String ja, String en) { EN.put(ja, en); }
    static boolean english(Context context) {
        return context.getSharedPreferences("MainActivity", 0).getBoolean("english",
                false);
    }
    static String get(boolean english, String ja) {
        String result = english ? EN.get(ja) : null;
        return result == null ? ja : result;
    }
}
