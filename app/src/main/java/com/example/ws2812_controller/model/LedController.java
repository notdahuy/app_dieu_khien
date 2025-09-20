package com.example.ws2812_controller.model;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LedController {
    private static final String BASE_URL = "http://192.168.71.1";
    private final OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public String sendCommand(String path) throws IOException {
        RequestBody body = RequestBody.create("", MediaType.parse("text/plain"));

        Request request = new Request.Builder()
            .url(BASE_URL + path)
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Error: " + response.code();
            }
            return response.body().string();
        }
    }

    public String turnOn() throws IOException {
        return sendCommand("/on");
    }

    public String turnOff() throws IOException {
        return sendCommand("/off");
    }

    public String setColor(int r, int g, int b) throws IOException {
        String json = "{ \"r\": " + r + ", \"g\": " + g + ", \"b\": " + b + " }";
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
            .url(BASE_URL + "/set")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            return response.body() != null ? response.body().string() : "No response";
        }
    }

    public String setBrightness(int percent) throws IOException {
        // Validate brightness percentage (0-100)
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Brightness percent must be between 0 and 100");
        }

        String json = "{ \"percent\": " + percent + " }";
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
            .url(BASE_URL + "/brightness")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return "Error: " + response.code();
            }
            return response.body() != null ? response.body().string() : "No response";
        }
    }
}
