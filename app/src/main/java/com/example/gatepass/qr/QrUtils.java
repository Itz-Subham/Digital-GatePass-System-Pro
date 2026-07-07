package com.example.gatepass.qr;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.example.gatepass.utils.Constants;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class QrUtils {

    public static Bitmap generateQrCode(String content, int size) {
        if (content == null || content.isEmpty()) return null;

        MultiFormatWriter writer = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            Log.e("QrUtils", "QR generation failed", e);
            return null;
        }
    }

    // handles both the full URL format and the older "ID:xxx|name" format from old QR passes
    public static String extractDocId(String rawValue) {
        if (rawValue == null || rawValue.isEmpty()) return null;

        String result = rawValue.trim();

        if (result.startsWith(Constants.BASE_QR_URL)) {
            String docId = result.substring(Constants.BASE_QR_URL.length()).trim();
            return validateId(docId);
        }

        if (result.contains("http") || result.contains("://")
                || result.contains("/") || result.contains("?")) {
            return null;
        }

        if (result.startsWith("ID:")) {
            int pipeIndex = result.indexOf("|");
            result = (pipeIndex != -1)
                    ? result.substring(3, pipeIndex)
                    : result.substring(3);
        }

        return validateId(result.trim());
    }

    private static String validateId(String id) {
        if (id == null || id.length() < 3) {
            return null;
        }
        return id;
    }

    public static String getWebPassUrl(String docId) {
        if (docId == null) return "";
        return Constants.BASE_QR_URL + docId.trim();
    }
}
