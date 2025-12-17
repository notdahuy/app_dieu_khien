package com.example.ws2812_controller.model;

public class WifiNetwork {
    public String ssid;
    public int rssi;
    public String auth;

    public WifiNetwork(String ssid, int rssi, String auth) {
        this.ssid = ssid;
        this.rssi = rssi;
        this.auth = auth;
    }

    public int getSignalStrength() {
        // Convert RSSI to percentage
        if (rssi >= -50) return 100;
        if (rssi >= -60) return 75;
        if (rssi >= -70) return 50;
        if (rssi >= -80) return 25;
        return 10;
    }
}