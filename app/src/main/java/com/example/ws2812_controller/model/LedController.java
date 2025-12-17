package com.example.ws2812_controller.model;

import android.content.Context;
import android.content.SharedPreferences;
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

    // SharedPreferences
    private static final String PREFS_NAME = "ESP32_Prefs";
    private static final String KEY_CURRENT_IP = "current_esp_ip";

    // Form-urlencoded content type
    public static final MediaType FORM_URLENCODED =
        MediaType.parse("application/x-www-form-urlencoded");

    // Default IPs
    private static final String ESP32_AP_IP = "192.168.71.1";
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

    // Get current ESP32 IP from SharedPreferences
    public String getCurrentEspIp() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedIp = prefs.getString(KEY_CURRENT_IP, "");

        if (savedIp.isEmpty()) {
            // Nếu chưa có IP lưu, trả về AP IP
            Log.d(TAG, "No saved IP, using AP IP: " + ESP32_AP_IP);
            return ESP32_AP_IP;
        }

        Log.d(TAG, "Using saved IP: " + savedIp);
        return savedIp;
    }

    // Method để update IP từ SettingFragment
    public void updateEspIp(String newIp) {
        if (newIp != null && !newIp.isEmpty()) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_CURRENT_IP, newIp);
            editor.apply();
            Log.d(TAG, "Updated ESP IP to: " + newIp);
        }
    }

    // Kiểm tra kết nối đến IP hiện tại
    public boolean testConnection() {
        String ip = getCurrentEspIp();
        String url = "http://" + ip + "/status";

        OkHttpClient testClient = new OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build();

        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = testClient.newCall(request).execute()) {
            boolean isSuccessful = response.isSuccessful();
            Log.d(TAG, "Connection test to " + ip + ": " + (isSuccessful ? "SUCCESS" : "FAILED"));
            return isSuccessful;
        } catch (IOException e) {
            Log.d(TAG, "Connection test to " + ip + " failed: " + e.getMessage());
            return false;
        }
    }

    // Tự động fallback nếu IP hiện tại không hoạt động
    private String getActiveEspIp() throws IOException {
        String currentIp = getCurrentEspIp();

        // Thử IP hiện tại trước
        if (testIpConnection(currentIp)) {
            return currentIp;
        }

        // Nếu IP hiện tại không hoạt động, thử AP IP
        Log.w(TAG, "Current IP " + currentIp + " not responding, trying AP IP");
        if (testIpConnection(ESP32_AP_IP)) {
            // Lưu AP IP để lần sau dùng
            updateEspIp(ESP32_AP_IP);
            return ESP32_AP_IP;
        }

        // Cả hai đều không hoạt động
        throw new IOException("Cannot connect to ESP32. Please check connection.");
    }

    private boolean testIpConnection(String ip) {
        String url = "http://" + ip + "/status";

        OkHttpClient testClient = new OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build();

        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();

        try (Response response = testClient.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException e) {
            return false;
        }
    }

    // Send command to ESP32
    private String sendCommand(String formBody) throws IOException {
        String ip = getActiveEspIp();
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

                // Nếu lỗi, thử lại với AP IP
                if (!ip.equals(ESP32_AP_IP)) {
                    Log.d(TAG, "Retrying with AP IP");
                    updateEspIp(ESP32_AP_IP);
                    throw new IOException("Connection lost, switched to AP mode");
                }
                throw new IOException("HTTP Error: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "{}";
            Log.d(TAG, "Response: " + responseBody);
            return responseBody;
        } catch (IOException e) {
            Log.e(TAG, "Network error: " + e.getMessage());

            // Thử lại với AP IP nếu chưa phải
            if (!ip.equals(ESP32_AP_IP)) {
                Log.d(TAG, "Retrying with AP IP");
                updateEspIp(ESP32_AP_IP);
                // Có thể thử gửi lại command với AP IP ở đây nếu muốn
            }
            throw new IOException("Network error: " + e.getMessage());
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
        if (params.isEmpty()) {
            return ""; // Không có param nào
        }

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
        String ip = getActiveEspIp();
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

    // Phương thức helper để kiểm tra xem có đang kết nối WiFi không
    public boolean isConnectedToWifi() {
        String ip = getCurrentEspIp();
        return ip != null && !ip.isEmpty() && !ip.equals(ESP32_AP_IP);
    }

    // Reset về AP mode
    public void resetToApMode() {
        updateEspIp(ESP32_AP_IP);
        Log.d(TAG, "Reset to AP mode");
    }
}