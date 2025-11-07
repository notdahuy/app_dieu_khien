package com.example.ws2812_controller.model;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LedController {
    private static final String TAG = "LedController";
    private final OkHttpClient client;
    private final Context context;

    // Form-urlencoded content type
    public static final MediaType FORM_URLENCODED =
        MediaType.parse("application/x-www-form-urlencoded");

    // ESP32 AP mode default IP
    private static final String ESP32_IP = "192.168.71.1";
    private static final int TIMEOUT_SECONDS = 5;

    public LedController(Context context) {
        this.context = context.getApplicationContext();

        // Configure OkHttp with timeouts
        this.client = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();
    }

    // Get current WiFi IP (or use hardcoded ESP32 IP)
    private String getCurrentWifiIP() {
        return ESP32_IP;
    }

    // Send command to ESP32
    private String sendCommand(String formBody) throws IOException {
        String ip = getCurrentWifiIP();
        if (ip == null) {
            throw new IOException("Cannot get WiFi IP");
        }

        String url = "http://" + ip + "/led";

        Log.d(TAG, "Sending to " + url + " body: " + formBody);

        RequestBody body = RequestBody.create(formBody, FORM_URLENCODED);

        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                Log.e(TAG, "Request failed: " + response.code() + " - " + errorBody);
                throw new IOException("HTTP Error: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "{}";
            Log.d(TAG, "Response: " + responseBody);
            return responseBody;
        } catch (IOException e) {
            Log.e(TAG, "Network error: " + e.getMessage());
            throw e;
        }
    }

    // Build form-urlencoded body string
    private String buildFormBody(String mode, Integer r, Integer g, Integer b,
                                 Integer brightness, Integer speed) {
        List<String> params = new ArrayList<>();

        // Convert RGB to hex color (RRGGBB format)
        if (r != null && g != null && b != null) {
            String hexColor = String.format(Locale.US, "%02X%02X%02X", r, g, b);
            params.add("color=" + hexColor);
        }

        if (brightness != null) {
            // Clamp 0-100
            int clamped = Math.max(0, Math.min(100, brightness));
            params.add("brightness=" + clamped);
        }

        if (speed != null) {
            // Clamp 1-255
            int clamped = Math.max(1, Math.min(255, speed));
            params.add("speed=" + clamped);
        }

        if (mode != null && !mode.isEmpty()) {
            params.add("mode=" + mode);
        }

        // Join with &
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            sb.append(params.get(i));
            if (i < params.size() - 1) {
                sb.append("&");
            }
        }

        String body = sb.toString();
        Log.d(TAG, "Form body: " + body);
        return body;
    }

    // ===== PUBLIC API METHODS =====

    // Turn on LED (white, 50% brightness)
    public String turnOn() throws IOException {
        return sendCommand(buildFormBody("static", 255, 255, 255, 50, null));
    }

    // Turn off LED (brightness = 0)
    public String turnOff() throws IOException {
        return sendCommand(buildFormBody(null, null, null, null, 0, null));
    }

    // Set static color
    public String setColor(int r, int g, int b, int brightness) throws IOException {
        return sendCommand(buildFormBody("static", r, g, b, brightness, null));
    }

    public String updateColorOnly(int r, int g, int b) throws IOException {
        return sendCommand(buildFormBody(null, r, g, b, null, null));
    }

    // Rainbow effect
    public String setEffect(String mode, int speed) throws IOException {
        // Gửi null cho color và brightness
        return sendCommand(buildFormBody(mode, null, null, null, null, speed));
    }


    // Set brightness only
    public String setBrightness(int brightness) throws IOException {
        return sendCommand(buildFormBody(null, null, null, null, brightness, null));
    }

    // Set speed only (don't change mode)
    public String setSpeed(int speed) throws IOException {
        return sendCommand(buildFormBody(null, null, null, null, null, speed));
    }

    // Check device status
    public String getStatus() throws IOException {
        String ip = getCurrentWifiIP();
        if (ip == null) {
            throw new IOException("Cannot get WiFi IP");
        }

        String url = "http://" + ip + "/status";

        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP Error: " + response.code());
            }
            return response.body() != null ? response.body().string() : "{}";
        }
    }
}