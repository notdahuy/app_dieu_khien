package com.example.ws2812_controller.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.example.ws2812_controller.R;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ws2812_controller.model.TimerSchedule;

import org.json.JSONObject;

public class TimerFragment extends Fragment {

    private static final String PREFS = "timer_prefs";
    private static final String KEY_SCHEDULE = "schedule_json";

    private Switch swEnable;
    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;
    private TimePicker tpOn, tpOff;
    private TextView tvSummary;
    private Button btnSave, btnApply, btnAllDays, btnClearDays;

    private TimerSchedule schedule;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_timer, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        bindViews(v);
        schedule = loadSchedule(requireContext());
        applyScheduleToViews(schedule);
        updateSummary();

        btnSave.setOnClickListener(view -> {
            readViewsToSchedule();
            saveSchedule(requireContext(), schedule);
            Toast.makeText(requireContext(), "Đã lưu cấu hình Timer", Toast.LENGTH_SHORT).show();
            updateSummary();
        });

        btnApply.setOnClickListener(view -> {
            readViewsToSchedule();
            // TODO: Gửi lệnh sang ESP32/Controller (MQTT/HTTP/Socket) theo giao thức của bạn
            // Ví dụ: sendTimerConfig(schedule);
            Toast.makeText(requireContext(), "Đã áp dụng (gửi cấu hình ra thiết bị)", Toast.LENGTH_SHORT).show();
            updateSummary();
        });

        btnAllDays.setOnClickListener(vv -> setAllDays(true));
        btnClearDays.setOnClickListener(vv -> setAllDays(false));
    }

    private void bindViews(View v) {
        swEnable = v.findViewById(R.id.swEnable);

        cbMon = v.findViewById(R.id.cbMon);
        cbTue = v.findViewById(R.id.cbTue);
        cbWed = v.findViewById(R.id.cbWed);
        cbThu = v.findViewById(R.id.cbThu);
        cbFri = v.findViewById(R.id.cbFri);
        cbSat = v.findViewById(R.id.cbSat);
        cbSun = v.findViewById(R.id.cbSun);

        tpOn = v.findViewById(R.id.tpOn);
        tpOff = v.findViewById(R.id.tpOff);

        // TimePicker 24h
        if (Build.VERSION.SDK_INT >= 23) {
            tpOn.setIs24HourView(true);
            tpOff.setIs24HourView(true);
        }

        btnSave = v.findViewById(R.id.btnSave);
        btnApply = v.findViewById(R.id.btnApply);
        btnAllDays = v.findViewById(R.id.btnAllDays);
        btnClearDays = v.findViewById(R.id.btnClearDays);
        tvSummary = v.findViewById(R.id.tvSummary);
    }

    private void setAllDays(boolean checked) {
        cbMon.setChecked(checked);
        cbTue.setChecked(checked);
        cbWed.setChecked(checked);
        cbThu.setChecked(checked);
        cbFri.setChecked(checked);
        cbSat.setChecked(checked);
        cbSun.setChecked(checked);
        updateSummary();
    }

    private void applyScheduleToViews(TimerSchedule t) {
        swEnable.setChecked(t.enabled);

        if (Build.VERSION.SDK_INT >= 23) {
            tpOn.setHour(t.onHour);
            tpOn.setMinute(t.onMinute);
            tpOff.setHour(t.offHour);
            tpOff.setMinute(t.offMinute);
        } else {
            tpOn.setCurrentHour(t.onHour);
            tpOn.setCurrentMinute(t.onMinute);
            tpOff.setCurrentHour(t.offHour);
            tpOff.setCurrentMinute(t.offMinute);
        }

        cbMon.setChecked(t.days[0]);
        cbTue.setChecked(t.days[1]);
        cbWed.setChecked(t.days[2]);
        cbThu.setChecked(t.days[3]);
        cbFri.setChecked(t.days[4]);
        cbSat.setChecked(t.days[5]);
        cbSun.setChecked(t.days[6]);
    }

    private void readViewsToSchedule() {
        if (schedule == null) schedule = TimerSchedule.defaultOf();
        schedule.enabled = swEnable.isChecked();

        if (Build.VERSION.SDK_INT >= 23) {
            schedule.onHour = tpOn.getHour();
            schedule.onMinute = tpOn.getMinute();
            schedule.offHour = tpOff.getHour();
            schedule.offMinute = tpOff.getMinute();
        } else {
            schedule.onHour = tpOn.getCurrentHour();
            schedule.onMinute = tpOn.getCurrentMinute();
            schedule.offHour = tpOff.getCurrentHour();
            schedule.offMinute = tpOff.getCurrentMinute();
        }

        schedule.days[0] = cbMon.isChecked();
        schedule.days[1] = cbTue.isChecked();
        schedule.days[2] = cbWed.isChecked();
        schedule.days[3] = cbThu.isChecked();
        schedule.days[4] = cbFri.isChecked();
        schedule.days[5] = cbSat.isChecked();
        schedule.days[6] = cbSun.isChecked();
    }

    private void updateSummary() {
        readViewsToSchedule();
        tvSummary.setText(schedule.summary());
    }

    private TimerSchedule loadSchedule(Context ctx) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String js = sp.getString(KEY_SCHEDULE, "");
            if (js == null || js.isEmpty()) return TimerSchedule.defaultOf();
            return TimerSchedule.fromJson(new JSONObject(js));
        } catch (Exception e) {
            return TimerSchedule.defaultOf();
        }
    }

    private void saveSchedule(Context ctx, TimerSchedule t) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY_SCHEDULE, t.toJson().toString()).apply();
    }
}
