package com.sahayak.app.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP client that calls the Sahayak Node.js + Nodemailer backend.
 * Default URL is http://10.0.2.2:3000 (Android Emulator -> host machine).
 * On a physical device, Admin can set the host PC's LAN IP (e.g. http://192.168.1.10:3000).
 */
public class EmailServiceClient {

    public static final String DEFAULT_BASE_URL = "http://10.0.2.2:3000";
    private static final String PREFS_NAME = "sahayak_email_prefs";
    private static final String KEY_BASE_URL = "nodemailer_base_url";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface EmailCallback {
        void onComplete(boolean emailSent, String message);
    }

    public static String getBaseUrl(Context context) {
        if (context == null) return DEFAULT_BASE_URL;
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String url = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
        if (url == null || url.trim().isEmpty()) return DEFAULT_BASE_URL;
        return url.trim().replaceAll("/+$", "");
    }

    public static void setBaseUrl(Context context, String url) {
        if (context == null) return;
        String clean = (url == null || url.trim().isEmpty()) ? DEFAULT_BASE_URL : url.trim().replaceAll("/+$", "");
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
                payload.put("address", address);
                payload.put("description", description);
                payload.put("documentCount", documentCount);
                payload.put("reviewToken", reviewToken);
                payload.put("isResubmission", isResubmission);
                if (resubmissionNote != null && !resubmissionNote.isEmpty()) {
                    payload.put("resubmissionNote", resubmissionNote);
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
                MAIN_HANDLER.post(() -> callback.onComplete(true, "Email sent via Nodemailer."));
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
