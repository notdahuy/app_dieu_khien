package com.example.ws2812_controller.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class TimerSchedule {
    public int id;
    public int onHour = 7, onMinute = 0;
    public int offHour = 22, offMinute = 0;
    public String onEffect = "Static"; // Thêm effect cho lúc bật (tên đầy đủ, không lowercase)
    // days[0]=Mon ... days[6]=Sun
    public boolean[] days = new boolean[7];

    // Constructor mặc định
    public TimerSchedule() {
        this.id = (int) System.currentTimeMillis(); // Dùng timestamp thay vì AtomicInteger
    }

    public static TimerSchedule defaultOf() {
        TimerSchedule t = new TimerSchedule();
        for (int i = 0; i < 7; i++) t.days[i] = false; // mặc định không chọn ngày nào
        return t;
    }

    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("onHour", onHour);
            o.put("onMinute", onMinute);
            o.put("offHour", offHour);
            o.put("offMinute", offMinute);
            o.put("onEffect", onEffect);
            JSONArray arr = new JSONArray();
            for (boolean d : days) arr.put(d);
            o.put("days", arr);
            return o;
        } catch (Exception e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    public static TimerSchedule fromJson(JSONObject o) {
        TimerSchedule t = new TimerSchedule();
        if (o == null) return t;
        t.id = o.optInt("id", t.id);
        t.onHour = o.optInt("onHour", 7);
        t.onMinute = o.optInt("onMinute", 0);
        t.offHour = o.optInt("offHour", 22);
        t.offMinute = o.optInt("offMinute", 0);
        t.onEffect = o.optString("onEffect", "Static");
        JSONArray arr = o.optJSONArray("days");
        if (arr != null && arr.length() == 7) {
            for (int i = 0; i < 7; i++) t.days[i] = arr.optBoolean(i, false);
        }
        return t;
    }

    // Validate thời gian
    public boolean isValidTime() {
        return onHour >= 0 && onHour <= 23 && onMinute >= 0 && onMinute <= 59
            && offHour >= 0 && offHour <= 23 && offMinute >= 0 && offMinute <= 59;
    }

    // Kiểm tra có ít nhất 1 ngày được chọn
    public boolean hasAtLeastOneDay() {
        for (boolean day : days) {
            if (day) return true;
        }
        return false;
    }

    // Tiện ích hiển thị
    public String timeRangeStr() {
        return String.format("%02d:%02d - %02d:%02d", onHour, onMinute, offHour, offMinute);
    }

    public String daysShortStr() {
        String[] names = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < 7; i++) {
            if (days[i]) {
                if (!first) sb.append(", ");
                sb.append(names[i]);
                first = false;
            }
        }
        return first ? "(Không có ngày nào)" : sb.toString();
    }

    public String daysFullStr() {
        String[] names = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < 7; i++) {
            if (days[i]) {
                if (!first) sb.append(", ");
                sb.append(names[i]);
                first = false;
            }
        }
        return first ? "Không có ngày nào" : sb.toString();
    }

    public String summary() {
        if (!hasAtLeastOneDay()) {
            return "⚠️ Chưa chọn ngày nào";
        }

        return String.format(
            "BẬT %02d:%02d (%s) • TẮT %02d:%02d • %s",
            onHour, onMinute, onEffect,
            offHour, offMinute,
            daysShortStr()
        );
    }

    // Chuyển đổi effect name sang API format
    public String getOnEffectApiName() {
        return onEffect.toLowerCase().replace(" ", "");
    }

    @Override
    public String toString() {
        return summary();
    }
}