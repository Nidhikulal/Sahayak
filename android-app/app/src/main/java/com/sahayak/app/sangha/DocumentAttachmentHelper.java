package com.sahayak.app.sangha;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Helper for encoding/decoding supporting documents (images and PDFs) safely for Firestore storage
 * without requiring paid cloud storage buckets.
 */
public class DocumentAttachmentHelper {

    public static final int MAX_PDF_BYTES = 350 * 1024; // 350 KB per PDF so doc stays well under 1 MB Firestore limit

    public static Map<String, Object> readUriAsDocumentMap(Context context, Uri uri) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        String mimeType = resolver.getType(uri);
        if (mimeType == null) mimeType = "application/octet-stream";

        String fileName = queryFileName(resolver, uri);
        if (fileName == null || fileName.isEmpty()) {
            fileName = mimeType.startsWith("image/") ? "document.jpg" : "document.pdf";
        }

        byte[] rawBytes;
        try (InputStream is = resolver.openInputStream(uri)) {
            if (is == null) throw new IllegalStateException("Unable to read selected file.");
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, n);
            }
            rawBytes = buffer.toByteArray();
        }

        byte[] finalBytes;
        if (mimeType.startsWith("image/")) {
            finalBytes = compressImageBytes(rawBytes);
            mimeType = "image/jpeg";
        } else {
            if (rawBytes.length > MAX_PDF_BYTES) {
                throw new IllegalArgumentException(fileName + " is too large (" + formatSize(rawBytes.length)
                        + "). Please upload a PDF smaller than 350 KB or attach an image.");
            }
            finalBytes = rawBytes;
        }

        String base64 = Base64.encodeToString(finalBytes, Base64.NO_WRAP);

        Map<String, Object> docMap = new HashMap<>();
        docMap.put("fileName", fileName);
        docMap.put("mimeType", mimeType);
        docMap.put("sizeBytes", (long) finalBytes.length);
        docMap.put("base64Data", base64);
        return docMap;
    }

    private static byte[] compressImageBytes(byte[] rawBytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.length);
        if (bitmap == null) return rawBytes;

        int maxDim = 1024;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (w > maxDim || h > maxDim) {
            float ratio = Math.min((float) maxDim / w, (float) maxDim / h);
            bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(w * ratio), Math.round(h * ratio), true);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
        return out.toByteArray();
    }

    public static Bitmap decodeImage(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) return null;
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static File writeTempFile(Context context, String fileName, String base64Data) throws Exception {
        File dir = new File(context.getCacheDir(), "docs");
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        File outFile = new File(dir, safeName);
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(bytes);
        }
        return outFile;
    }

    private static String queryFileName(ContentResolver resolver, Uri uri) {
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return cursor.getString(idx);
            }
        } catch (Exception ignored) {
        }
        return uri.getLastPathSegment();
    }

    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
    }
}
