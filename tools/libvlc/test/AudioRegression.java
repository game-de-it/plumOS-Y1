package com.kroot.y1themes.audiotest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.File;
import java.io.FileWriter;
import org.json.JSONArray;
import org.json.JSONObject;

/** On-device tests of the real player's native decode/EQ/volume paths. */
public class AudioRegression extends Instrumentation {
    volatile Activity activity;
    JSONObject results = new JSONObject();
    JSONArray cases = new JSONArray();
    String directory;
    String mode;
    boolean requireBt;
    Object field(String name) throws Exception { Field f = activity.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(activity); }
    void field(String name, Object value) throws Exception { Field f = activity.getClass().getDeclaredField(name); f.setAccessible(true); f.set(activity, value); }
    Object invoke(String name, Class<?>[] types, Object... args) throws Exception { Method m = activity.getClass().getDeclaredMethod(name, types); m.setAccessible(true); return m.invoke(activity, args); }
    void main(final Runnable task) { runOnMainSync(task); }
    void start(final String path, final int eq) throws Exception {
        final Throwable[] error = new Throwable[1];
        main(new Runnable() { public void run() { try {
            invoke("releasePlayback", new Class<?>[0]);
            field("boostLevel", 0); field("eqMode", eq);
            float[] bands = (float[])field("eqBands");
            for (int i=0; i<bands.length; i++) bands[i] = eq == 2 && i < 3 ? 4f : 0f;
            invoke("startVlc", new Class<?>[] {String.class, int.class}, path, ((Integer)field("playbackSerial")).intValue());
        } catch (Throwable e) { error[0] = e; } }});
        if (error[0] != null) throw new RuntimeException(error[0]);
    }
    Object vlc(String method) throws Exception { Object p = field("vlcPlayer"); return p.getClass().getMethod(method).invoke(p); }
    @Override public void callActivityOnCreate(Activity created, Bundle state) {
        super.callActivityOnCreate(created, state);
        if (created.getClass().getName().equals("com.kroot.y1themes.MainActivity")) activity = created;
    }
    @Override public void callActivityOnResume(Activity resumed) {
        super.callActivityOnResume(resumed);
        if (resumed.getClass().getName().equals("com.kroot.y1themes.MainActivity")) activity = resumed;
    }
    public void onCreate(Bundle args) { directory = args.getString("directory"); mode = args.getString("mode"); requireBt = "true".equals(args.getString("requireA2dp")); start(); }
    public void onStart() {
        Bundle status = new Bundle();
        try {
            Intent intent = getTargetContext().getPackageManager().getLaunchIntentForPackage(getTargetContext().getPackageName());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            getTargetContext().startActivity(intent);
            for (int i=0; activity == null && i<100; i++) SystemClock.sleep(100);
            if (activity == null) throw new Exception("Target activity did not start");
            waitForIdleSync();
            runOnMainSync(new Runnable() { public void run() { activity.getWindow().addFlags(128); } });
            SystemClock.sleep(1500);
            final AudioManager audio = (AudioManager)activity.getSystemService("audio");
            final int originalVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            final int originalEq = (Integer)field("eqMode"), originalBoost = (Integer)field("boostLevel");
            final float[] originalBands = ((float[])field("eqBands")).clone();
            results.put("version", getTargetContext().getPackageManager().getPackageInfo(getTargetContext().getPackageName(), 0).versionName);
            results.put("cases", cases);
            results.put("originalEq", originalEq); results.put("originalBoost", originalBoost); results.put("originalVolume", originalVolume);
            if (requireBt) {
                for (int i=0; i<40; i++) {
                    android.bluetooth.BluetoothProfile proxy = (android.bluetooth.BluetoothProfile)field("a2dp");
                    if (audio.isBluetoothA2dpOn() && proxy != null && !proxy.getConnectedDevices().isEmpty()) break;
                    SystemClock.sleep(250);
                }
                android.bluetooth.BluetoothProfile proxy = (android.bluetooth.BluetoothProfile)field("a2dp");
                if (!audio.isBluetoothA2dpOn() || proxy == null || proxy.getConnectedDevices().isEmpty())
                    throw new Exception("Connect the headset from native Android Bluetooth settings before this test");
            }
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, "releaseSmoke".equals(mode) ? 0 : 2, 0);
            try {
                File[] files = new File(directory).listFiles();
                if ("probe".equals(mode) || "btProbe".equals(mode) || "releaseSmoke".equals(mode)) files = new File[] {new File(directory, "tone.wav")};
                if (files == null) throw new Exception("Missing audio fixtures");
                java.util.Arrays.sort(files);
                for (File file : files) {
                    if (file.getName().startsWith(".")) continue;
                    for (int eq : new int[] {0, 1, 2}) {
                        start(file.getAbsolutePath(), eq);
                        SystemClock.sleep(1200);
                        long first = ((Long)vlc("getTime")).longValue();
                        SystemClock.sleep(1400);
                        long second = ((Long)vlc("getTime")).longValue();
                        boolean playing = (Boolean)vlc("isPlaying");
                        JSONObject row = new JSONObject();
                        row.put("file", file.getName()); row.put("eq", eq);
                        row.put("startMs", first); row.put("endMs", second); row.put("isPlaying", playing);
                        Object equalizer = invoke("makeEqualizer", new Class<?>[0]);
                        Object nativePlayer = field("vlcPlayer");
                        boolean eqAccepted = (Boolean)nativePlayer.getClass().getMethod("setEqualizer", equalizer.getClass()).invoke(nativePlayer, eq == 0 ? null : equalizer);
                        row.put("equalizerAccepted", eqAccepted);
                        row.put("a2dp", audio.isBluetoothA2dpOn());
                        row.put("pass", playing && second > first + 500 && eqAccepted && (!requireBt || audio.isBluetoothA2dpOn()));
                        row.put("volume", vlc("getVolume"));
                        if (!results.has("resolvedNativeLibraries")) {
                            java.util.Set<String> paths = new java.util.TreeSet<String>();
                            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader("/proc/self/maps"));
                            for (String line; (line = reader.readLine()) != null;) {
                                for (String name : new String[] {"libvlc.so", "libvlcjni.so", "libc++_shared.so"})
                                    if (line.endsWith("/"+name)) paths.add(line.substring(line.indexOf('/')));
                            }
                            reader.close();
                            JSONArray libraries = new JSONArray();
                            for (String path : paths) {
                                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                                java.io.FileInputStream input = new java.io.FileInputStream(path);
                                byte[] buffer = new byte[65536]; for (int count; (count=input.read(buffer)) != -1;) digest.update(buffer, 0, count); input.close();
                                StringBuilder hex = new StringBuilder(); for (byte b : digest.digest()) hex.append(String.format(java.util.Locale.US, "%02x", b & 255));
                                JSONObject library = new JSONObject(); library.put("path", path); library.put("sha256", hex.toString()); libraries.put(library);
                            }
                            results.put("resolvedNativeLibraries", libraries);
                        }
                        cases.put(row);
                        android.util.Log.i("Y1AudioTest", row.toString());
                    }
                }
                // Exercise the application's normal Android-first and EQ->LibVLC paths too.
                java.util.List<?> tracks = (java.util.List<?>)field("tracks");
                JSONArray normal = new JSONArray();
                for (String extension : (("probe".equals(mode) || "releaseSmoke".equals(mode)) ? new String[0] : new String[] {".mp3", ".m4a"})) {
                    int selected = -1;
                    for (int i=0; i<tracks.size(); i++) {
                        Field pathField = tracks.get(i).getClass().getDeclaredField("path"); pathField.setAccessible(true);
                        if (((String)pathField.get(tracks.get(i))).toLowerCase(java.util.Locale.US).endsWith(extension)) { selected = i; break; }
                    }
                    if (selected < 0) continue;
                    final int index = selected;
                    for (final int eq : new int[] {0, 1}) {
                        main(new Runnable() { public void run() { try {
                            field("eqMode", eq); field("boostLevel", 0);
                            java.util.Arrays.fill((float[])field("eqBands"), 0f);
                            invoke("playTrack", new Class<?>[] {int.class}, index);
                        } catch (Exception e) { throw new RuntimeException(e); } }});
                        SystemClock.sleep(1500);
                        long first = ((Long)invoke("playbackPositionMs", new Class<?>[0])).longValue();
                        SystemClock.sleep(1400);
                        long last = ((Long)invoke("playbackPositionMs", new Class<?>[0])).longValue();
                        JSONObject row = new JSONObject(); row.put("format", extension); row.put("eq", eq);
                        row.put("engine", field("vlcPlayer") == null ? "Android" : "LibVLC");
                        row.put("startMs", first); row.put("endMs", last); row.put("pass", last > first + 500);
                        normal.put(row); android.util.Log.i("Y1AudioTest", row.toString());
                    }
                }
                results.put("normalPlayback", normal);
                if (!"releaseSmoke".equals(mode)) {
                start(new File(directory, "tone.wav").getAbsolutePath(), 1);
                SystemClock.sleep(800);
                final JSONArray volumes = new JSONArray();
                main(new Runnable() { public void run() { try {
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                    field("boostLevel", 0);
                    for (int i=0; i<4; i++) {
                        if (i > 0) invoke("adjustVolume", new Class<?>[] {int.class}, 1);
                        JSONObject row = new JSONObject(); row.put("boost", field("boostLevel"));
                        row.put("percent", invoke("boostPercent", new Class<?>[0])); row.put("readback", vlc("getVolume"));
                        volumes.put(row);
                    }
                    for (int i=0; i<4; i++) invoke("adjustVolume", new Class<?>[] {int.class}, -1);
                    results.put("volumeSteps", volumes);
                    results.put("loweredVolume", audio.getStreamVolume(AudioManager.STREAM_MUSIC));
                    results.put("loweredBoost", field("boostLevel"));
                    results.put("a2dp", audio.isBluetoothA2dpOn());
                    android.bluetooth.BluetoothProfile proxy = (android.bluetooth.BluetoothProfile)field("a2dp");
                    results.put("a2dpConnectedDevices", proxy == null ? 0 : proxy.getConnectedDevices().size());
                    if (requireBt && (!audio.isBluetoothA2dpOn() || proxy == null || proxy.getConnectedDevices().isEmpty())) throw new Exception("A2DP disconnected during test");
                } catch (Exception e) { throw new RuntimeException(e); } }});
                }
            } finally {
                main(new Runnable() { public void run() { try {
                    invoke("releasePlayback", new Class<?>[0]);
                    field("eqMode", originalEq); field("boostLevel", originalBoost); field("eqBands", originalBands);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
                } catch (Exception e) { throw new RuntimeException(e); } }});
            }
            FileWriter writer = new FileWriter(new File(getTargetContext().getFilesDir(), "audio-regression.json")); writer.write(results.toString(2)); writer.close();
            status.putString("results", results.toString()); finish(Activity.RESULT_OK, status);
        } catch (Throwable error) {
            android.util.Log.e("Y1AudioTest", "Regression failed", error);
            status.putString("error", android.util.Log.getStackTraceString(error)); finish(Activity.RESULT_CANCELED, status);
        }
    }
}
