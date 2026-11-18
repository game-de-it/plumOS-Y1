package com.kroot.y1themes;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.util.HashMap;
import java.util.List;

final class LibraryCache {
    static final class Entry {
        Library.Track track;
        long size, modified;
    }
    private static SQLiteDatabase open(Context context) {
        SQLiteDatabase db = context.openOrCreateDatabase("music-cache.db", 0, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS tracks (path TEXT PRIMARY KEY, size INTEGER, modified INTEGER, title TEXT, artist TEXT, album TEXT, duration INTEGER)");
        return db;
    }
    static HashMap<String, Entry> read(Context context) {
        HashMap<String, Entry> result = new HashMap<String, Entry>();
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = open(context);
            cursor = db.rawQuery("SELECT path,size,modified,title,artist,album,duration FROM tracks LIMIT 20000", null);
            while (cursor.moveToNext()) {
                Entry entry = new Entry();
                entry.size = cursor.getLong(1); entry.modified = cursor.getLong(2);
                entry.track = new Library.Track(cursor.getString(0), cursor.getString(3),
                        cursor.getString(4), cursor.getString(5), cursor.getLong(6));
                result.put(entry.track.path, entry);
            }
        } catch (Exception error) {
            android.util.Log.w("Y1Scan", "cache read failed; rebuilding", error);
            result.clear();
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }
        return result;
    }
    static void write(Context context, List<Library.Track> tracks,
            HashMap<String, Entry> entries) {
        SQLiteDatabase db = null;
        try {
            db = open(context); db.beginTransaction();
            db.delete("tracks", null, null);
            for (Library.Track track : tracks) {
                Entry entry = entries.get(track.path);
                if (entry == null) continue;
                ContentValues values = new ContentValues();
                values.put("path", track.path); values.put("size", entry.size);
                values.put("modified", entry.modified); values.put("title", track.title);
                values.put("artist", track.artist); values.put("album", track.album);
                values.put("duration", track.duration);
                db.insertOrThrow("tracks", null, values);
            }
            db.setTransactionSuccessful();
        } catch (Exception error) {
            android.util.Log.w("Y1Scan", "cache write failed", error);
        } finally {
            if (db != null) { if (db.inTransaction()) db.endTransaction(); db.close(); }
        }
    }
    static void clear(Context context) { context.deleteDatabase("music-cache.db"); }
}
