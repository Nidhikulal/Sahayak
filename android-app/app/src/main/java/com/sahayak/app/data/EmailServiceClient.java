package com.sahayak.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP client that calls the Sahayak Node.js + Nodemailer backend.
 * Uses http://10.0.2.2:3000 for Android Emulator and your LAN IP for Physical Phone.
 */
public class EmailServiceClient {

    public static final String EMULATOR_BASE_URL = "http://10.0.2.2:3000";
    public static final String DEVICE_BASE_URL = "http://10.124.39.155:3000";
    public static final String DEFAULT_BASE_URL = "http://10.124.39.155:3000";

    private static final String PREFS_NAME = "sahayak_email_prefs";
    private static final String KEY_BASE_URL = "nodemailer_base_url";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface EmailCallback {
        void onComplete(boolean emailSent, String message);
    }

    private static boolean isEmulator() {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT));
    }

    public static String getBaseUrl(Context context) {
        String smartDefault = isEmulator() ? EMULATOR_BASE_URL : DEVICE_BASE_URL;
        if (context == null) return smartDefault;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String url = prefs.getString(KEY_BASE_URL, smartDefault);
        if (url == null || url.trim().isEmpty() || url.contains("192.168.1.101")) return smartDefault;
        return url.trim().replaceAll("/+$", "");
    }

    public static void setBaseUrl(Context context, String url) {
        if (context == null) return;
        String smartDefault = isEmulator() ? EMULATOR_BASE_URL : DEVICE_BASE_URL;
        String clean = (url == null || url.trim().isEmpty()) ? smartDefault : url.trim().replaceAll("/+$", "");
        context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BASE_URL, clean)
                .apply();
    }

    public static void notifyAdminNewApplication(
            Context context,
            String applicationId,
            String organizationName,
            String registrationNo,
            String email,
            String mobile,
            String contactPerson,
            String address,
            String description,
            int documentCount,
            String reviewToken,
            boolean isResubmission,
            String resubmissionNote,
            EmailCallback callback
    ) {
        notifyAdminNewApplication(
                context, applicationId, organizationName, registrationNo, email, mobile,
                contactPerson, "", "", "", "", "", 0.0, 0.0, address, description, "", null,
                documentCount, reviewToken, isResubmission, resubmissionNote, callback
        );
    }

    public static void notifyAdminNewApplication(
            Context context,
            String applicationId,
            String organizationName,
            String registrationNo,
            String email,
            String mobile,
            String contactPerson,
            String district,
            String locality,
            String city,
            String pincode,
            String state,
            double latitude,
            double longitude,
            String address,
            String description,
            String username,
            Object documents,
            int documentCount,
            String reviewToken,
            boolean isResubmission,
            String resubmissionNote,
            EmailCallback callback
    ) {
        String baseUrl = getBaseUrl(context);
        EXECUTOR.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("applicationId", applicationId);
                payload.put("organizationName", organizationName);
                payload.put("registrationNo", registrationNo);
                payload.put("email", email);
                payload.put("mobile", mobile);
                payload.put("contactPerson", contactPerson);
                payload.put("district", district != null ? district : "");
                payload.put("locality", locality != null ? locality : "");
                payload.put("city", city != null ? city : "");
                payload.put("pincode", pincode != null ? pincode : "");
                payload.put("state", state != null ? state : "");
                payload.put("latitude", latitude);
                payload.put("longitude", longitude);
                payload.put("address", address);
                payload.put("description", description);
                payload.put("username", username != null ? username : "");
                payload.put("documentCount", documentCount);
                payload.put("reviewToken", reviewToken);
                payload.put("isResubmission", isResubmission);
                if (resubmissionNote != null && !resubmissionNote.isEmpty()) {
                    payload.put("resubmissionNote", resubmissionNote);
                }

                if (documents instanceof List<?>) {
                    JSONArray docsArray = new JSONArray();
                    for (Object item : (List<?>) documents) {
                        if (item instanceof Map<?, ?>) {
                            JSONObject docObj = new JSONObject();
                            Map<?, ?> map = (Map<?, ?>) item;
                            docObj.put("fileName", String.valueOf(map.get("fileName")));
                            docObj.put("mimeType", String.valueOf(map.get("mimeType")));
                            docObj.put("base64Data", String.valueOf(map.get("base64Data")));
                            docObj.put("sizeBytes", map.get("sizeBytes"));
                            docsArray.put(docObj);
                        }
                    }
                    payload.put("documents", docsArray);
                } else if (documents instanceof JSONArray) {
                    payload.put("documents", documents);
                }

                postJson(baseUrl + "/api/notify-admin", payload, callback);
            } catch (Exception e) {
                MAIN_HANDLER.post(() -> callback.onComplete(false, e.getMessage()));
            }
        });
    }

    public static void notifySanghaDecision(
            Context context,
            String applicationId,
            String organizationName,
            String toEmail,
            String decision,
            String remarks,
            String registrationToken,
            String expiresAtFormatted,
            EmailCallback callback
    ) {
        String baseUrl = getBaseUrl(context);
        EXECUTOR.execute(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("applicationId", applicationId);
                payload.put("organizationName", organizationName);
                payload.put("toEmail", toEmail);
                payload.put("decision", decision);
                payload.put("remarks", remarks != null ? remarks : "");
                payload.put("registrationToken", registrationToken != null ? registrationToken : "");
                payload.put("expiresAtFormatted", expiresAtFormatted != null ? expiresAtFormatted : "");

                postJson(baseUrl + "/api/notify-sangha-decision", payload, callback);
            } catch (Exception e) {
                MAIN_HANDLER.post(() -> callback.onComplete(false, e.getMessage()));
            }
        });
    }

    private static void postJson(String endpoint, JSONObject body, EmailCallback callback) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bytes);
            }

            int code = conn.getResponseCode();
            InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            StringBuilder sb = new StringBuilder();
            if (stream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }
            }

            String respText = sb.toString();
            if (code >= 200 && code < 300) {
                boolean isSuccess = true;
                String msg = "Email sent via Nodemailer.";
                try {
                    JSONObject json = new JSONObject(respText);
                    if (json.has("success")) {
                        isSuccess = json.getBoolean("success");
                    }
                    if (json.has("message")) {
                        msg = json.getString("message");
                    }
                } catch (Exception ignored) {}
                final boolean finalSuccess = isSuccess;
                final String finalMsg = msg;
                MAIN_HANDLER.post(() -> callback.onComplete(finalSuccess, finalMsg));
            } else {
                String err = respText;
                try {
                    JSONObject json = new JSONObject(respText);
                    if (json.has("error")) err = json.getString("error");
                } catch (Exception ignored) {
                }
                final String finalErr = err;
                MAIN_HANDLER.post(() -> callback.onComplete(false, finalErr));
            }
        } catch (Exception e) {
            final String msg = "Email server unreachable (" + e.getMessage() + ")";
            MAIN_HANDLER.post(() -> callback.onComplete(false, msg));
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
