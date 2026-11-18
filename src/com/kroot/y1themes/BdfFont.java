package com.kroot.y1themes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.SparseArray;
import java.io.DataInputStream;
import java.io.InputStream;

/** Compact bitmap rendering for the U-M+ BDF assets used by the Hi-MD theme. */
final class BdfFont {
    private static final class Glyph {
        int advance;
        short[] rows;
        Bitmap bitmap;
    }
    private final SparseArray<Glyph> glyphs = new SparseArray<Glyph>();
    private final Paint bitmapPaint = new Paint();
    private final Paint fallbackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF destination = new RectF();
    private int width, height;

    BdfFont(Context context, String path) {
        try {
            InputStream stream = context.getAssets().open(path);
            try {
                DataInputStream input = new DataInputStream(stream);
                if (input.readInt() != 0x42444631) return; // BDF1
                width = input.readUnsignedByte();
                height = input.readUnsignedByte();
                int count = input.readInt();
                if (count < 0 || count > 20000 || width > 16 || height > 16) return;
                for (int i = 0; i < count; i++) {
                    int codepoint = input.readInt();
                    Glyph glyph = new Glyph();
                    glyph.advance = input.readUnsignedByte();
                    glyph.rows = new short[height];
                    for (int row = 0; row < height; row++) glyph.rows[row] = input.readShort();
                    glyphs.put(codepoint, glyph);
                }
            } finally { stream.close(); }
        } catch (Exception ignored) { glyphs.clear(); }
        bitmapPaint.setFilterBitmap(false);
        fallbackPaint.setTypeface(Typeface.DEFAULT);
    }

    float measure(String value, float scale) {
        if (value == null) return 0;
        float result = 0;
        for (int offset = 0; offset < value.length();) {
            int cp = value.codePointAt(offset);
            Glyph glyph = glyphs.get(cp);
            result += (glyph == null ? width : glyph.advance) * scale;
            offset += Character.charCount(cp);
        }
        return result;
    }

    void draw(Canvas canvas, String value, float x, float top, float scale, int color) {
        if (value == null) return;
        bitmapPaint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        fallbackPaint.setColor(color);
        fallbackPaint.setTextSize(height * scale);
        for (int offset = 0; offset < value.length();) {
            int cp = value.codePointAt(offset);
            Glyph glyph = glyphs.get(cp);
            if (glyph != null) {
                if (glyph.bitmap == null) glyph.bitmap = createBitmap(glyph);
                destination.set(x, top, x + width * scale, top + height * scale);
                canvas.drawBitmap(glyph.bitmap, null, destination, bitmapPaint);
                x += glyph.advance * scale;
            } else {
                // Missing BDF characters retain a readable Android font fallback.
                String character = new String(Character.toChars(cp));
                canvas.drawText(character, x, top + (height - 2) * scale, fallbackPaint);
                x += width * scale;
            }
            offset += Character.charCount(cp);
        }
        bitmapPaint.setColorFilter(null);
    }

    private Bitmap createBitmap(Glyph glyph) {
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int bits = glyph.rows[y] & 0xffff;
            for (int x = 0; x < width; x++)
                if ((bits & (1 << (15 - x))) != 0) pixels[y * width + x] = Color.WHITE;
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }
}
