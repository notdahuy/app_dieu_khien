package com.example.ws2812_controller.fragment;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.example.ws2812_controller.R;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ShareCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ws2812_controller.model.SharedLedViewModel;
import com.example.ws2812_controller.model.TimerSchedule;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TimerFragment extends Fragment {

    private static final String PREFS = "timer_prefs";
    private static final String KEY_SCHEDULE = "schedule_json";

    private CheckBox cbMon, cbTue, cbWed, cbThu, cbFri, cbSat, cbSun;
    private LinearLayout mondayRow, tuesdayRow, wednesdayRow, thursdayRow, fridayRow, saturdayRow, sundayRow;
    private TextView tvSummary, tvTimeOn, tvTimeOff;
    private Button btnSave, btnApply, btnAllDays, btnClearDays;

    private CardView cvTimeOn, cvTimeOff;

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
        setupCheckboxClickListeners();

        cvTimeOn.setOnClickListener(view -> {
            // Inflate Bottom Sheet layout
            View bottomSheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_timer, null);

            // Lấy các view
            NumberPicker hourPicker = bottomSheetView.findViewById(R.id.hourPicker);
            NumberPicker minutePicker = bottomSheetView.findViewById(R.id.minutePicker);
            Spinner effectSpinner = bottomSheetView.findViewById(R.id.effectSpinner);
            Button btnCancel = bottomSheetView.findViewById(R.id.btnCancel);
            Button btnSaveTime = bottomSheetView.findViewById(R.id.btnSaveTime);

            // Setup Spinner
            SharedLedViewModel viewModel = new ViewModelProvider(requireActivity()).get(SharedLedViewModel.class);
            List<String> effectNames = viewModel.getNormalEffects();

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                effectNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            effectSpinner.setAdapter(adapter);

            // Set giá trị đã chọn trước đó
            int position = effectNames.indexOf(schedule.onEffect);
            if (position >= 0) {
                effectSpinner.setSelection(position);
            }

            // Cấu hình NumberPicker
            hourPicker.setMinValue(0);
            hourPicker.setMaxValue(23);
            hourPicker.setValue(schedule.onHour);
            hourPicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));

            minutePicker.setMinValue(0);
            minutePicker.setMaxValue(59);
            minutePicker.setValue(schedule.onMinute);
            minutePicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));

            // Tạo BottomSheetDialog
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show();

            // Button Hủy
            btnCancel.setOnClickListener(cancel -> bottomSheetDialog.dismiss());

            // Button Lưu
            btnSaveTime.setOnClickListener(save -> {
                schedule.onHour = hourPicker.getValue();
                schedule.onMinute = minutePicker.getValue();

                // Lưu effect đã chọn
                Object selected = effectSpinner.getSelectedItem();
                if (selected != null) {
                    schedule.onEffect = selected.toString();
                }

                tvTimeOn.setText(String.format("%02d:%02d",
                    schedule.onHour, schedule.onMinute));


                updateSummary();
                bottomSheetDialog.dismiss();
            });
        });

        cvTimeOff.setOnClickListener(view -> {
            // Inflate Bottom Sheet layout
            View bottomSheetView = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottom_sheet_timer, null);

            // Lấy các view
            TextView tvSheetTitle = bottomSheetView.findViewById(R.id.tvSheetTitle);
            TextView tvSheetEffect = bottomSheetView.findViewById(R.id.tvSheetEffect);
            NumberPicker hourPicker = bottomSheetView.findViewById(R.id.hourPicker);
            NumberPicker minutePicker = bottomSheetView.findViewById(R.id.minutePicker);
            Spinner effectSpinner = bottomSheetView.findViewById(R.id.effectSpinner);
            Button btnCancel = bottomSheetView.findViewById(R.id.btnCancel);
            Button btnSaveTime = bottomSheetView.findViewById(R.id.btnSaveTime);

            tvSheetTitle.setText("Chọn thời gian tắt LED");
            effectSpinner.setVisibility(View.GONE);
            tvSheetEffect.setVisibility(View.GONE);

            // Cấu hình NumberPicker
            hourPicker.setMinValue(0);
            hourPicker.setMaxValue(23);
            hourPicker.setValue(schedule.offHour); // ĐÚNG: offHour
            hourPicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));

            minutePicker.setMinValue(0);
            minutePicker.setMaxValue(59);
            minutePicker.setValue(schedule.offMinute); // ĐÚNG: offMinute
            minutePicker.setFormatter(value -> String.format(Locale.getDefault(), "%02d", value));

            // Tạo BottomSheetDialog
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show();

            // Button Hủy
            btnCancel.setOnClickListener(cancel -> bottomSheetDialog.dismiss());

            // Button Lưu
            btnSaveTime.setOnClickListener(save -> {
                schedule.offHour = hourPicker.getValue();    // ĐÚNG: offHour
                schedule.offMinute = minutePicker.getValue(); // ĐÚNG: offMinute

                tvTimeOff.setText(String.format("%02d:%02d", schedule.offHour, schedule.offMinute));

                updateSummary();
                bottomSheetDialog.dismiss();
            });
        });



        btnSave.setOnClickListener(view -> {
            readViewsToSchedule();
            saveSchedule(requireContext(), schedule);
            Toast.makeText(requireContext(), "Đã lưu cấu hình Timer", Toast.LENGTH_SHORT).show();
            updateSummary();
        });

        btnApply.setOnClickListener(view -> {
            readViewsToSchedule();
            // Ví dụ: sendTimerConfig(schedule);
            Toast.makeText(requireContext(), "Đã áp dụng (gửi cấu hình ra thiết bị)", Toast.LENGTH_SHORT).show();
            updateSummary();
        });

        btnAllDays.setOnClickListener(vv -> setAllDays(true));
        btnClearDays.setOnClickListener(vv -> setAllDays(false));
    }

    private void bindViews(View v) {

        cbMon = v.findViewById(R.id.cbMon);
        cbTue = v.findViewById(R.id.cbTue);
        cbWed = v.findViewById(R.id.cbWed);
        cbThu = v.findViewById(R.id.cbThu);
        cbFri = v.findViewById(R.id.cbFri);
        cbSat = v.findViewById(R.id.cbSat);
        cbSun = v.findViewById(R.id.cbSun);

        // Bind các LinearLayout rows
        mondayRow = v.findViewById(R.id.mondayRow);
        tuesdayRow = v.findViewById(R.id.tuesdayRow);
        wednesdayRow = v.findViewById(R.id.wednesdayRow);
        thursdayRow = v.findViewById(R.id.thursdayRow);
        fridayRow = v.findViewById(R.id.fridayRow);
        saturdayRow = v.findViewById(R.id.saturdayRow);
        sundayRow = v.findViewById(R.id.sundayRow);

        // TimePicker 24h
        tvTimeOn = v.findViewById(R.id.tvTimeOn);
        tvTimeOff = v.findViewById(R.id.tvTimeOff);
        cvTimeOn = v.findViewById(R.id.cvTimeOn);
        cvTimeOff = v.findViewById(R.id.cvTimeOff);
        btnSave = v.findViewById(R.id.btnSave);
        btnApply = v.findViewById(R.id.btnApply);
        btnAllDays = v.findViewById(R.id.btnAllDays);
        btnClearDays = v.findViewById(R.id.btnClearDays);
        tvSummary = v.findViewById(R.id.tvSummary);
    }

    private void setupCheckboxClickListeners() {
        // Click vào row sẽ toggle checkbox
        mondayRow.setOnClickListener(v -> {
            cbMon.setChecked(!cbMon.isChecked());
            updateSummary();
        });

        tuesdayRow.setOnClickListener(v -> {
            cbTue.setChecked(!cbTue.isChecked());
            updateSummary();
        });

        wednesdayRow.setOnClickListener(v -> {
            cbWed.setChecked(!cbWed.isChecked());
            updateSummary();
        });

        thursdayRow.setOnClickListener(v -> {
            cbThu.setChecked(!cbThu.isChecked());
            updateSummary();
        });

        fridayRow.setOnClickListener(v -> {
            cbFri.setChecked(!cbFri.isChecked());
            updateSummary();
        });

        saturdayRow.setOnClickListener(v -> {
            cbSat.setChecked(!cbSat.isChecked());
            updateSummary();
        });

        sundayRow.setOnClickListener(v -> {
            cbSun.setChecked(!cbSun.isChecked());
            updateSummary();
        });

        // Cũng có thể click trực tiếp vào checkbox
        cbMon.setOnCheckedChangeListener((buttonView, isChecked) -> updateSummary());
        cbTue.setOnCheckedChangeListener((buttonView, isChecked) -> updateSummary());
        cbWed.setOnCheckedChangeListener((buttonView, isChecked) -> updateSummary());
        cbThu.setOnCheckedChangeListener((buttonView, isChecked) -> updateSummary());
        cbFri.setOnCheckedChangeListener((buttonView, isChecked) -> updateSummary());
        cbSat.setOnCheckedChangeListener((buttonView, isChecked) -> updateSummary());
        cbSun.setOnCheckedChangeListener((buttonView, isChecked) -> updateSummary());
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
        cbMon.setChecked(t.days[0]);
        cbTue.setChecked(t.days[1]);
        cbWed.setChecked(t.days[2]);
        cbThu.setChecked(t.days[3]);
        cbFri.setChecked(t.days[4]);
        cbSat.setChecked(t.days[5]);
        cbSun.setChecked(t.days[6]);

        // Load thời gian đã lưu
        tvTimeOn.setText(String.format("%02d:%02d", t.onHour, t.onMinute));
        tvTimeOff.setText(String.format("%02d:%02d", t.offHour, t.offMinute));
    }

    private void readViewsToSchedule() {
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