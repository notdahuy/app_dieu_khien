package com.example.ws2812_controller.model;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LedController {
    private final OkHttpClient client = new OkHttpClient();
    private final Context context;

    public LedController(Context context) {
        this.context = context;
    }

    // 🧠 Lấy IP của WiFi hiện tại
    private String getCurrentWifiIP() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) return null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipInt = wifiInfo.getIpAddress();
        String deviceIp = Formatter.formatIpAddress(ipInt); // ví dụ: 192.168.4.2
        // Giả định ESP32 có IP .1 trong cùng subnet
        if (deviceIp.contains(".")) {
            String base = deviceIp.substring(0, deviceIp.lastIndexOf('.') + 1);
            return base + "1"; // ví dụ -> 192.168.4.1
        }
        return "192.168.4.1";
    }

    // 🧩 Hàm gửi POST /led?...
    private String sendCommand(String query) throws IOException {
        String ip = getCurrentWifiIP();
        if (ip == null) return "Error: cannot get WiFi IP";

        String url = "http://" + ip + "/led?" + query;
        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(new byte[0], null))
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Error: " + response.code();
            }
            return response.body() != null ? response.body().string() : "No response";
        }
    }

    // Helper: Build query string từ các tham số
    private String buildQuery(String mode, Integer r, Integer g, Integer b, Integer brightness, Integer speed) {
        List<String> params = new ArrayList<>();

        // Thứ tự không quan trọng vì Rust parse hết trước
        // Nhưng để dễ đọc, ta giữ theo thứ tự: color -> brightness -> speed -> mode
        if (r != null && g != null && b != null) {
            params.add("r=" + r);
            params.add("g=" + g);
            params.add("b=" + b);
        }

        if (brightness != null) {
            params.add("brightness=" + brightness);
        }

        if (speed != null) {
            params.add("speed=" + speed);
        }

        if (mode != null && !mode.isEmpty()) {
            params.add("mode=" + mode);
        }

        return String.join("&", params);
    }

    // Bật LED (mode=on với brightness)
    public String turnOn() throws IOException {
        return sendCommand(buildQuery("on", null, null, null, 150, null));
    }

    // Tắt LED
    public String turnOff() throws IOException {
        return sendCommand(buildQuery("off", null, null, null, null, null));
    }

    // Đặt màu (static mode với color)
    public String setColor(int r, int g, int b, int brightness) throws IOException {
        // Gửi cả color, brightness và mode trong 1 request
        return sendCommand(buildQuery("static", r, g, b, brightness, null));
    }

    // Hiệu ứng rainbow
    public String setRainbow(int brightness) throws IOException {
        return sendCommand(buildQuery("rainbow", null, null, null, brightness, null));
    }

    // Hiệu ứng blink
    public String setBlink(int brightness, int speed) throws IOException {
        return sendCommand(buildQuery("blink", null, null, null, brightness, speed));
    }

    // Hiệu ứng aurora
    public String setAurora(int brightness, int speed) throws IOException {
        return sendCommand(buildQuery("aurora", null, null, null, brightness, speed));
    }

    // Hiệu ứng meteor
    public String setMeteor(int brightness, int speed) throws IOException {
        return sendCommand(buildQuery("meteor", null, null, null, brightness, speed));
    }

    // Chỉnh độ sáng (không đổi mode)
    public String setBrightness(int brightness) throws IOException {
        return sendCommand(buildQuery(null, null, null, null, brightness, null));
    }

    // Chỉnh speed (không đổi mode)
    public String setSpeed(int speed) throws IOException {
        return sendCommand(buildQuery(null, null, null, null, null, speed));
    }

    // Advanced: Tùy chỉnh đầy đủ
    public String setCustom(String mode, Integer r, Integer g, Integer b, Integer brightness, Integer speed) throws IOException {
        return sendCommand(buildQuery(mode, r, g, b, brightness, speed));
    }
}