package com.example.ws2812_controller.model;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.atomic.AtomicInteger;

public class TimerSchedule {
    private static final AtomicInteger ID_GEN = new AtomicInteger(1);

    public int id;                 // id tự tăng
    public boolean enabled = false;
    public int onHour = 18, onMinute = 0;
    public int offHour = 22, offMinute = 0;
    // days[0]=Mon ... days[6]=Sun
    public boolean[] days = new boolean[7];

    public TimerSchedule() {
        // gán id tự tăng nếu chưa có
        this.id = ID_GEN.getAndIncrement();
    }

    public static TimerSchedule defaultOf() {
        TimerSchedule t = new TimerSchedule();
        for (int i = 0; i < 7; i++) t.days[i] = true; // mặc định cả tuần
        return t;
    }

    public JSONObject toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("enabled", enabled);
            o.put("onHour", onHour);
            o.put("onMinute", onMinute);
            o.put("offHour", offHour);
            o.put("offMinute", offMinute);
            JSONArray arr = new JSONArray();
            for (boolean d : days) arr.put(d);
            o.put("days", arr);
            return o;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static TimerSchedule fromJson(JSONObject o) {
        TimerSchedule t = new TimerSchedule();
        if (o == null) return t;
        t.id = o.optInt("id", t.id); // nếu JSON chưa có id, giữ id đã gán ở ctor
        t.enabled = o.optBoolean("enabled", false);
        t.onHour = o.optInt("onHour", 18);
        t.onMinute = o.optInt("onMinute", 0);
        t.offHour = o.optInt("offHour", 22);
        t.offMinute = o.optInt("offMinute", 0);
        JSONArray arr = o.optJSONArray("days");
        if (arr != null && arr.length() == 7) {
            for (int i = 0; i < 7; i++) t.days[i] = arr.optBoolean(i, false);
        }
        return t;
    }

    // tiện ích hiển thị
    public String timeRangeStr() {
        return String.format("%02d:%02d - %02d:%02d", onHour, onMinute, offHour, offMinute);
    }

    public String daysShortStr() {
        String[] names = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < 7; i++) {
            if (days[i]) {
                if (!first) sb.append(",");
                sb.append(names[i]);
                first = false;
            }
        }
        return first ? "(none)" : sb.toString();
    }

    public String daysString() {
        // nếu nơi khác đang gọi daysString() thì dùng bản rút gọn này
        return daysShortStr();
    }
    public String summary() {
        return String.format(
                "%s • ON %02d:%02d • OFF %02d:%02d • Days: %s",
                enabled ? "ENABLED" : "DISABLED",
                onHour, onMinute, offHour, offMinute,
                daysShortStr()  // dùng chuỗi ngày rút gọn Mon,Tue,...
        );
    }
}


