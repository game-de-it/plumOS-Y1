package com.kroot.y1themes;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.provider.MediaStore;
import android.os.Environment;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

final class Library {
    static final class Track {
        final String path, fileName, title, artist, album;
        final long duration;
        Track(String path, String title, String artist, String album, long duration) {
            this.path = path; this.fileName = new File(path).getName();
            this.title = clean(title, filename(path));
            this.artist = clean(artist, "不明なアーティスト");
            this.album = clean(album, "不明なアルバム"); this.duration = duration;
        }
        private static String filename(String p) {
            String f = new File(p).getName();
            int dot = f.lastIndexOf('.');
            return dot > 0 ? f.substring(0, dot) : f;
        }
        private static String clean(String s, String fallback) {
            return s == null || s.length() == 0 || s.equals("<unknown>") ? fallback : s;
        }
    }

    interface CachedListener { void ready(List<Track> tracks); }
    static synchronized void clearCache(Context context) { LibraryCache.clear(context); }

    static synchronized List<Track> load(Context context, CachedListener listener) {
        long started = android.os.SystemClock.uptimeMillis();
        java.util.HashMap<String, LibraryCache.Entry> cached = LibraryCache.read(context);
        if (!cached.isEmpty() && listener != null) {
            ArrayList<Track> initial = new ArrayList<Track>();
            for (LibraryCache.Entry entry : cached.values()) initial.add(entry.track);
            listener.ready(initial);
            android.util.Log.i("Y1Scan", "cache tracks=" + initial.size() + " elapsedMs=" +
                    (android.os.SystemClock.uptimeMillis()-started));
        }
        ArrayList<Track> tracks = new ArrayList<Track>();
        java.util.HashMap<String, LibraryCache.Entry> updated = new java.util.HashMap<String, LibraryCache.Entry>();
        int[] stats = new int[2]; // reused, metadata reads
        boolean[] complete = {true};
        scan(Environment.getExternalStorageDirectory(), tracks, cached, updated, stats, complete, 0);
        File secondary = new File("/storage/sdcard1");
        if (secondary.isDirectory()) scan(secondary, tracks, cached, updated, stats, complete, 0);
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) == 0)
            scan(new File("/data/local/tmp/y1-bgmset"), tracks, cached, updated, stats, complete, 0);
        if (complete[0] && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
            LibraryCache.write(context, tracks, updated);
        else {
            // An unreadable or disconnected volume is not proof of deletion.
            for (LibraryCache.Entry entry : cached.values())
                if (!updated.containsKey(entry.track.path)) tracks.add(entry.track);
        }
        android.util.Log.i("Y1Scan", "incremental tracks=" + tracks.size() + " reused=" + stats[0] +
                " metadataReads=" + stats[1] + " complete=" + complete[0] + " elapsedMs=" +
                (android.os.SystemClock.uptimeMillis()-started));
        return tracks;
    }

    private static void scan(File dir, List<Track> tracks,
            java.util.HashMap<String, LibraryCache.Entry> cached,
            java.util.HashMap<String, LibraryCache.Entry> updated,
            int[] stats, boolean[] complete, int depth) {
        if (depth > 32 || tracks.size() >= 20000) { complete[0] = false; return; }
        File[] files = dir.listFiles();
        if (files == null) { complete[0] = false; return; }
        for (File f : files) {
            if (tracks.size() >= 20000) { complete[0] = false; break; }
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) { scan(f, tracks, cached, updated, stats, complete, depth + 1); continue; }
            if (isAudioFile(f)) {
                String path = f.getAbsolutePath();
                if (updated.containsKey(path)) continue;
                long size = f.length(), modified = f.lastModified();
                LibraryCache.Entry old = cached.get(path);
                LibraryCache.Entry entry = new LibraryCache.Entry();
                entry.size = size; entry.modified = modified;
                if (old != null && old.size == size && old.modified == modified) {
                    entry.track = old.track; stats[0]++;
                } else { entry.track = readMetadata(path); stats[1]++; }
                tracks.add(entry.track); updated.put(path, entry);
            }
        }
    }

    static boolean isAudioFile(File file) {
        String name = file.getName().toLowerCase(java.util.Locale.US);
        return file.isFile() && (name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".aac") ||
                name.endsWith(".flac") || name.endsWith(".ogg") || name.endsWith(".wav") ||
                name.endsWith(".aif") || name.endsWith(".aiff") || name.endsWith(".aifc") ||
                name.endsWith(".wma") || name.endsWith(".ape") || name.endsWith(".opus") ||
                name.endsWith(".wv") || name.endsWith(".mka") || name.endsWith(".alac") || name.endsWith(".caf"));
    }

    static Track readMetadata(String path) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            long ms = 0;
            try { if (duration != null) ms = Long.parseLong(duration); }
            catch (NumberFormatException ignored) {}
            return new Track(path,
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM), ms);
        } catch (Exception ignored) {
            return new Track(path, null, null, null, 0);
        } finally {
            try { retriever.release(); } catch (Exception ignored) {}
        }
    }
}
