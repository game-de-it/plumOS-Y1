package com.kroot.y1themes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.widget.Toast;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;

public final class ThemeInstallReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (!"com.kroot.y1themes.INSTALL_THEME".equals(intent.getAction())) return;
        try {
            String encoded = intent.getStringExtra("data");
            if (encoded == null || encoded.length() > 90000) throw new IllegalArgumentException("size");
            byte[] json = Base64.decode(encoded, Base64.NO_WRAP);
            if (json.length > 65536) throw new IllegalArgumentException("size");
            Theme theme = Theme.parse(new ByteArrayInputStream(json), null);
            if (theme.id.equals("gen4") || theme.id.equals("gen6"))
                throw new IllegalArgumentException("reserved id");
            File folder = new File(new File(context.getFilesDir(), "themes"), theme.id);
            if (!folder.isDirectory() && !folder.mkdirs()) throw new IllegalStateException("mkdir");
            File output = new File(folder, "theme.json");
            File temp = new File(folder, "theme.json.tmp");
            FileOutputStream stream = new FileOutputStream(temp);
            try { stream.write(json); stream.getFD().sync(); }
            finally { stream.close(); }
            if (!temp.renameTo(output)) throw new IllegalStateException("rename");
            boolean english = UiText.english(context);
            Toast.makeText(context, (english ? theme.nameEn : theme.name) +
                    UiText.get(english, "を追加しました"), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, UiText.get(UiText.english(context), "テーマ追加に失敗: ") +
                    e.getClass().getSimpleName(),
                    Toast.LENGTH_LONG).show();
        }
    }
}
