package com.kroot.y1themes;

import android.graphics.Color;
import android.graphics.Typeface;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Environment;

final class Theme {
    String id, name, nameEn, layout;
    int background, headerTop, headerBottom, text, muted, selectedTop, selectedBottom;
    int selectedText, divider, progress, fontSize, rowHeight;
    Typeface typeface = Typeface.DEFAULT;
    Typeface boldTypeface;
    Typeface nonLatinTypeface = Typeface.DEFAULT;
    boolean pixelPrimary;
    float primaryScale = 1f;
    java.util.HashSet<Integer> primaryGlyphs;
    boolean usesPrimary(int codepoint) {
        return primaryGlyphs == null ? codepoint < 128 : primaryGlyphs.contains(codepoint);
    }

    static List<Theme> loadAll(Context context) {
        List<Theme> result = new ArrayList<Theme>();
        for (String name : new String[]{"first.json", "third.json", "gen4.json",
                "fifth.json", "gen6.json", "nano5.json", "nano6.json", "touch.json",
                "winamp.json", "himd.json"}) {
            try {
                InputStream in = context.getAssets().open("themes/" + name);
                Theme theme = parse(in, null);
                if (theme.id.equals("first") ||
                        theme.id.equals("third") || theme.id.equals("gen4")) {
                    theme.typeface = Typeface.createFromAsset(context.getAssets(), "fonts/chicago/ChicagoKare-Regular.ttf");
                    InputStream coverage = context.getAssets().open("fonts/chicago/cmap.txt");
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    byte[] block = new byte[4096]; int count;
                    try { while ((count = coverage.read(block)) != -1) data.write(block, 0, count); }
                    finally { coverage.close(); }
                    theme.primaryGlyphs = new java.util.HashSet<Integer>();
                    for (String cp : data.toString("UTF-8").trim().split("\\s+"))
                        theme.primaryGlyphs.add(Integer.parseInt(cp));
                    theme.pixelPrimary = true;
                    theme.primaryScale = 1.5f;
                } else if (theme.id.equals("fifth")) {
                    loadScalableFont(context, theme, "source-sans", "SourceSans3-Regular.ttf", "SourceSans3-Bold.ttf", 1.3f);
                } else if (theme.id.equals("gen6") || theme.id.equals("nano5") ||
                        theme.id.equals("nano6") || theme.id.equals("touch")) {
                    loadScalableFont(context, theme, "heros", "texgyreheros-regular.otf", "texgyreheros-bold.otf", 1.2f);
                }
                result.add(theme);
            } catch (Exception ignored) {}
        }
        File local = new File(context.getFilesDir(), "themes");
        addDirectoryThemes(result, local);
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_SYSTEM) == 0)
            addDirectoryThemes(result, new File("/data/local/tmp/y1-themes"));
        File root = new File(Environment.getExternalStorageDirectory(), "Y1Themes");
        addDirectoryThemes(result, root);
        return result;
    }

    private static void loadScalableFont(Context context, Theme theme, String directory,
            String regular, String bold, float scale) throws Exception {
        String root = "fonts/" + directory + "/";
        theme.typeface = Typeface.createFromAsset(context.getAssets(), root + regular);
        theme.boldTypeface = Typeface.createFromAsset(context.getAssets(), root + bold);
        InputStream input = context.getAssets().open(root + "cmap.txt");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096]; int count;
        try { while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count); }
        finally { input.close(); }
        theme.primaryGlyphs = new java.util.HashSet<Integer>();
        for (String codepoint : bytes.toString("UTF-8").trim().split("\\s+"))
            theme.primaryGlyphs.add(Integer.parseInt(codepoint));
        theme.primaryScale = scale;
    }

    private static void addDirectoryThemes(List<Theme> result, File root) {
        File[] dirs = root.listFiles();
        if (dirs != null) {
            for (File dir : dirs) {
                if (!dir.isDirectory()) continue;
                File config = new File(dir, "theme.json");
                if (!config.isFile() || config.length() > 65536) continue;
                try { result.add(parse(new FileInputStream(config), dir)); }
                catch (Exception ignored) {}
            }
        }
    }

    static Theme parse(InputStream input, File dir) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        try {
            while ((n = input.read(buffer)) != -1) {
                if (bytes.size() + n > 65536) throw new IllegalArgumentException("theme too large");
                bytes.write(buffer, 0, n);
            }
        } finally { input.close(); }
        JSONObject json = new JSONObject(bytes.toString("UTF-8"));
        if (json.optInt("schema", 0) != 1) throw new IllegalArgumentException("schema");
        Theme t = new Theme();
        t.id = json.getString("id");
        t.name = json.getString("name");
        t.nameEn = json.optString("nameEn", t.name);
        if (t.name.length() > 40 || !t.id.matches("[a-z0-9_-]{1,40}") ||
                (dir != null && (t.id.equals("first") || t.id.equals("third") ||
                t.id.equals("gen4") || t.id.equals("fifth") || t.id.equals("gen6") ||
                t.id.equals("nano5") || t.id.equals("nano6") || t.id.equals("touch") ||
                t.id.equals("winamp") || t.id.equals("himd") || t.id.equals("chicago"))))
            throw new IllegalArgumentException("name");
        t.layout = json.getString("layout");
        if (!t.layout.equals("mono") && !t.layout.equals("mono3") &&
                !t.layout.equals("classic") && !t.layout.equals("color") &&
                !t.layout.equals("split") && !t.layout.equals("artfull") &&
                !t.layout.equals("grid") && !t.layout.equals("touch") &&
                !t.layout.equals("winamp") && !t.layout.equals("himd"))
            throw new IllegalArgumentException("layout");
        JSONObject c = json.getJSONObject("colors");
        t.background = color(c, "background", "#F3F5F6");
        t.headerTop = color(c, "headerTop", "#F7FAFC");
        t.headerBottom = color(c, "headerBottom", "#B9CEDC");
        t.text = color(c, "text", "#17232E");
        t.muted = color(c, "muted", "#566573");
        t.selectedTop = color(c, "selectedTop", "#54B8E6");
        t.selectedBottom = color(c, "selectedBottom", "#0878B8");
        t.selectedText = color(c, "selectedText", "#FFFFFF");
        t.divider = color(c, "divider", "#BAC9D3");
        t.progress = color(c, "progress", "#1991C8");
        t.fontSize = clamp(json.optInt("fontSize", 18), 14, 26);
        t.rowHeight = clamp(json.optInt("rowHeight", 43), 32, 64);
        if (t.layout.equals("winamp")) t.typeface = Typeface.MONOSPACE;
        if (dir != null) {
            File font = new File(dir, "font.ttf");
            if (font.isFile() && font.length() < 4000000) {
                try { t.typeface = Typeface.createFromFile(font); }
                catch (Exception ignored) {}
            }
            File nonLatinFont = new File(dir, "font-cjk.ttf");
            if (nonLatinFont.isFile() && nonLatinFont.length() < 4000000) {
                try { t.nonLatinTypeface = Typeface.createFromFile(nonLatinFont); }
                catch (Exception ignored) {}
            }
        }
        return t;
    }

    private static int color(JSONObject c, String key, String fallback) {
        try { return Color.parseColor(c.optString(key, fallback)); }
        catch (Exception ignored) { return Color.parseColor(fallback); }
    }
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
