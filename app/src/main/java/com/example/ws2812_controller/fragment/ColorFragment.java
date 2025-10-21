package com.example.ws2812_controller.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.model.LedController;
import com.github.antonpopoff.colorwheel.ColorWheel;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ColorFragment extends Fragment {

    private LedController ledController;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private int lastSelectedColor = Color.WHITE;
    private int currentBrightness = 80;
    private boolean isLedOn = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo LedController với Context
        ledController = new LedController(requireContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_color, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MaterialSwitch ledPowerSwitch = view.findViewById(R.id.powerSwitch);
        ColorWheel colorWheel = view.findViewById(R.id.colorWheel);
        Slider brightness = view.findViewById(R.id.brightnessSlider);
        TextView brightnessPercentage = view.findViewById(R.id.brightnessPercentage);

        // Khởi tạo giá trị ban đầu
        brightnessPercentage.setText(currentBrightness + "%");

        // === Power Switch ===
        ledPowerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isLedOn = isChecked;

            executor.execute(() -> {
                try {
                    String result = isChecked ? ledController.turnOn() : ledController.turnOff();
                    showToast(result);
                } catch (IOException e) {
                    showToast(e.getMessage());
                }
            });
        });

        // === Color Wheel ===
        colorWheel.setColorChangeListener(selectedColor -> {
            lastSelectedColor = selectedColor; // chỉ lưu tạm
            return null;
        });

        colorWheel.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (!isLedOn) {
                    showToast("Turn on LED first");
                    return false;
                }
                sendCurrentColor();
            }
            return false;
        });

        // === Preset Colors ===
        int[] colorIds = {
            R.id.colorRed,
            R.id.colorBlue,
            R.id.colorGreen,
            R.id.colorCyan,
            R.id.colorYellow,
            R.id.colorPink,
            R.id.colorOrange,
            R.id.colorWhite
        };

        for (int id : colorIds) {
            View colorView = view.findViewById(id);
            colorView.setOnClickListener(v -> {
                if (!isLedOn) {
                    showToast("Turn on LED first");
                    return;
                }

                int color = v.getBackgroundTintList() != null
                    ? v.getBackgroundTintList().getDefaultColor()
                    : Color.WHITE;

                lastSelectedColor = color;
                colorWheel.setRgb(Color.red(color), Color.green(color), Color.blue(color));
                sendCurrentColor();
            });
        }

        // === Brightness Slider ===
        brightness.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                currentBrightness = (int) value;
                brightnessPercentage.setText(currentBrightness + "%");
            }
        });

        brightness.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                if (!isLedOn) {
                    showToast("Turn on LED first");
                    return;
                }

                currentBrightness = (int) slider.getValue();
                sendCurrentColor();
            }
        });
    }

    private void sendCurrentColor() {
        int r = Color.red(lastSelectedColor);
        int g = Color.green(lastSelectedColor);
        int b = Color.blue(lastSelectedColor);

        executor.execute(() -> {
            try {
                String result = ledController.setColor(r, g, b, currentBrightness);
            } catch (IOException e) {
                showToast("Send failed: " + e.getMessage());
            }
        });
    }

    private void showToast(String message) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() ->
            Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }
}