package com.example.ws2812_controller.fragment;

import android.annotation.SuppressLint;
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
import java.util.concurrent.Executors;

public class ColorFragment extends Fragment {

    private final LedController ledController = new LedController();
    private int lastSelectedColor;
    private View selectedColorView = null;

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

        // Power Switch
        ledPowerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    String result = isChecked ? ledController.turnOn() : ledController.turnOff();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show()
                        );
                    }
                } catch (IOException e) {
//                    if (getActivity() != null) {
//                        getActivity().runOnUiThread(() ->
//                            Toast.makeText(getActivity(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
//                        );
//                    }
                }
            });
        });

        // Color Wheel
        colorWheel.setColorChangeListener((selectedColor) -> {
            // Lưu tạm màu cuối cùng mà người dùng chọn
            lastSelectedColor = selectedColor;
            return null;
        });

        // Lắng nghe sự kiện chạm/thả tay
        colorWheel.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Khi người dùng thả tay -> lấy màu cuối cùng
                int r = Color.red(lastSelectedColor);
                int g = Color.green(lastSelectedColor);
                int b = Color.blue(lastSelectedColor);

                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        String result = ledController.setColor(r, g, b);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (IOException e) {
//                        if (getActivity() != null) {
//                            getActivity().runOnUiThread(() ->
//                                Toast.makeText(getActivity(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
//                            );
//                        }
                    }
                });
            }
            return false;
        });

        // Mảng lưu trữ id màu preset
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


        // Xử lý sự kiện cho màu preset
        for (int id : colorIds) {
            View colorView = view.findViewById(id);
            colorView.setOnClickListener(v -> {
                v.setAlpha(1.0f);

                // Highlight View mới
                for (int otherId : colorIds) {
                    View otherView = view.findViewById(otherId);
                    if (otherView != v) { // Nếu View đó không phải là View vừa được chọn
                        otherView.setAlpha(0.5f);
                    }
                }

                // Lưu View mới đã được chọn
                selectedColorView = v;

                // Lấy màu từ view
                int color = 0;
                if (v.getBackgroundTintList() != null) {
                    color = v.getBackgroundTintList().getDefaultColor();
                }

                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);

                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        String result = ledController.setColor(r, g, b);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Color set: " + result, Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (IOException e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
            });
        }

        // Slider value change listener to update percentage
        brightness.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                int brightnessValue = (int) value;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                        brightnessPercentage.setText(brightnessValue + "%")
                    );
                }
            }
        });

        brightness.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                // Called when user starts touching the slider
                slider.setCustomThumbDrawable(R.drawable.custom_thumb);
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                // Called when user stops touching the slider
                int brightnessValue = (int) slider.getValue();

                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        String result = ledController.setBrightness(brightnessValue);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Brightness: " + brightnessValue + "%", Toast.LENGTH_SHORT).show()
                            );
                        }
                    } catch (IOException e) {
//                        if (getActivity() != null) {
//                            getActivity().runOnUiThread(() ->
//                                Toast.makeText(getActivity(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
//                            );
//                        }
                    } catch (IllegalArgumentException e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                Toast.makeText(getActivity(), "Invalid brightness: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                        }
                    }
                });
            }
        });
    }
}