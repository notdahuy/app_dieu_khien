package com.example.ws2812_controller.model;

public class Effect {
    private String name;
    private int previewColor;

    public Effect(String name, int previewColor) {
        this.name = name;
        this.previewColor = previewColor;
    }

    public String getName() { return name; }
    public int getPreviewColor() { return previewColor; }
}