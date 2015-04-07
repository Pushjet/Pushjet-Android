package io.Pushjet.api;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.zxing.common.BitMatrix;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MiscUtil {

    public static String hash(String s) {
        return hash(s, "MD5");
    }

    public static String hash(String s, String hash) {
        MessageDigest m;

        try {
            m = MessageDigest.getInstance(hash);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }

        m.update(s.getBytes(), 0, s.length());
        return new BigInteger(1, m.digest()).toString(16);
    }

    public static String iconFilename(String url) {
        return String.format("icon_%s", hash(url));
    }

    public static Bitmap matrixToBitmap(BitMatrix matrix) {
        int height = matrix.getHeight();
        int width = matrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int width, int height) {
        int oHeight = bitmap.getHeight();
        int oWidth = bitmap.getWidth();
        int sqr = oHeight > oWidth ? oHeight : oWidth;

        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        Bitmap background = Bitmap.createBitmap(sqr, sqr, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);
        canvas.drawBitmap(bitmap, (sqr - oWidth) / 2, (sqr - oHeight) / 2, paint);
        return Bitmap.createScaledBitmap(background, width, height, false);
    }

    public static void WriteToClipboard(String data, String title, Context ctx) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(data);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(title, data);
            clipboard.setPrimaryClip(clip);
        }
    }
}
