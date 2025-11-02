package com.example.ws2812_controller.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.ws2812_controller.model.TimerSchedule;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class TimerStorage {
    private static final String PREF = "timer_prefs";
    private static final String KEY = "schedules";
    private final SharedPreferences sp;
    private final Gson gson = new Gson();

    public TimerStorage(Context ctx) {
        sp = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public ArrayList<TimerSchedule> load() {
        String json = sp.getString(KEY, "[]");
        Type t = new TypeToken<ArrayList<TimerSchedule>>(){}.getType();
        return gson.fromJson(json, t);
    }

    public void save(ArrayList<TimerSchedule> list) {
        sp.edit().putString(KEY, gson.toJson(list)).apply();
    }
}
