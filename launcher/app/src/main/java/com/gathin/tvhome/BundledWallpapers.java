package com.gathin.tvhome;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

final class BundledWallpapers {
    private BundledWallpapers() {}

    static List<File> copy(Context context) {
        File dir = new File(context.getFilesDir(), "my_wallpapers");
        try {
            String[] names = context.getAssets().list("wallpapers");
            if (names == null) names = new String[0];
            if (!dir.isDirectory()) dir.mkdirs();
            for (String name : names) {
                File out = new File(dir, name);
                if (out.isFile() && out.length() > 0) continue;
                try (InputStream in = context.getAssets().open("wallpapers/" + name);
                     OutputStream os = new FileOutputStream(out)) {
                    byte[] buf = new byte[16384];
                    int n;
                    while ((n = in.read(buf)) > 0) os.write(buf, 0, n);
                }
            }
        } catch (Exception ignored) {
        }
        return WallpaperRotation.images(dir);
    }
}
