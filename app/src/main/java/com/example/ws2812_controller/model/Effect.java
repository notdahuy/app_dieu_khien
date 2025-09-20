package com.example.ws2812_controller.model;

public class Effect {
    private final String name;
    private final int previewColor;

    // multi-select + thứ tự
    private boolean selected = false;
    private int selectedOrder = 0; // 0 = chưa chọn; 1..N = thứ tự đã chọn

    public Effect(String name, int previewColor) {
        this.name = name;
        this.previewColor = previewColor;
    }

    public String getName() { return name; }
    public int getPreviewColor() { return previewColor; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public int getSelectedOrder() { return selectedOrder; }
    public void setSelectedOrder(int selectedOrder) { this.selectedOrder = selectedOrder; }
}
