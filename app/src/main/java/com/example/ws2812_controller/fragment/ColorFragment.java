package com.example.ws2812_controller.fragment;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.model.LedController;
import com.example.ws2812_controller.model.SharedLedViewModel;
import com.github.antonpopoff.colorwheel.ColorWheel;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ColorFragment extends Fragment {
    private static final String TAG = "ColorFragment";

    // === THAY ĐỔI: Đổi tên hằng số cho nhất quán ===
    private static final String TOAST_ERROR_MSG = "Connection Failed (Check Wi-Fi)";
    private static final String TOAST_TURN_ON_MSG = "TURN ON FIRST";
    // ==============================================

    private LedController ledController;
    private SharedLedViewModel viewModel;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());



    private final AtomicBoolean isSending = new AtomicBoolean(false);
    private Runnable pendingColorUpdate;
    private Runnable pendingBrightnessUpdate;

    // UI components
    private MaterialSwitch ledPowerSwitch;
    private TextView brightnessPercentage;
    private Slider brightnessSlider;
    private ColorWheel colorWheel;


    private final List<View> presetColorViews = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ledController = new LedController(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(SharedLedViewModel.class);
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


        ledPowerSwitch = view.findViewById(R.id.powerSwitch);
        colorWheel = view.findViewById(R.id.colorWheel);
        brightnessSlider = view.findViewById(R.id.brightnessSlider);
        brightnessPercentage = view.findViewById(R.id.brightnessPercentage);

        // Khởi tạo danh sách các nút màu
        presetColorViews.clear();
        int[] colorIds = {
            R.id.colorRed, R.id.colorBlue, R.id.colorGreen, R.id.colorCyan,
            R.id.colorYellow, R.id.colorPink, R.id.colorOrange, R.id.colorWhite
        };
        for (int id : colorIds) {
            View colorView = view.findViewById(id);
            if (colorView != null) {
                presetColorViews.add(colorView);
            }
        }


        Integer colorValue = viewModel.getCurrentColor().getValue();
        int initialColor = (colorValue != null) ? colorValue : Color.WHITE;

        Integer brightnessValue = viewModel.getCurrentBrightness().getValue();
        int initialBrightness = (brightnessValue != null) ? brightnessValue : 50;

        Boolean isOnValue = viewModel.getIsLedOn().getValue();
        boolean initialIsOn = (isOnValue != null) ? isOnValue : false;


        brightnessPercentage.setText(initialBrightness + "%");
        brightnessSlider.setValue(initialBrightness);
        ledPowerSwitch.setChecked(initialIsOn);
        colorWheel.setRgb(Color.red(initialColor), Color.green(initialColor), Color.blue(initialColor));



        // Lắng nghe trạng thái ON OFF
        viewModel.getIsLedOn().observe(getViewLifecycleOwner(), isOn -> {
            ledPowerSwitch.setChecked(isOn);
        });

        // Lắng nghe trạng thái Độ sáng
        viewModel.getCurrentBrightness().observe(getViewLifecycleOwner(), brightness -> {
            if ((int) brightnessSlider.getValue() != brightness) {
                brightnessSlider.setValue(brightness);
            }
            brightnessPercentage.setText(brightness + "%");
        });

        // Lắng nghe trạng thái Màu
        viewModel.getCurrentColor().observe(getViewLifecycleOwner(), color -> {
            if (colorWheel.getRgb() != color) {
                colorWheel.setRgb(Color.red(color), Color.green(color), Color.blue(color));
            }
        });

        // POWER SWITCH
        ledPowerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            viewModel.setIsLedOn(isChecked); // GHI VÀO VIEWMODEL

            // === THAY ĐỔI 1: Khi BẬT, luôn set về STATIC ===
            if (isChecked) {

                viewModel.setCurrentEffect("static");
                sendCurrentColor();
            } else {
                sendTurnOff();
            }
            // ============================================
        });

        // COLOR WHEEL
        colorWheel.setColorChangeListener(selectedColor -> {

            Integer currentColor = viewModel.getCurrentColor().getValue();
            if (currentColor == null || currentColor != selectedColor) {
                viewModel.setCurrentColor(selectedColor);
            }
            return null;
        });

        colorWheel.setOnTouchListener((v, event) -> {
            Boolean isOn = viewModel.getIsLedOn().getValue();


            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (isOn == null || !isOn) {
                    showToast(TOAST_TURN_ON_MSG);
                    return true;
                }
            }

            // === THAY ĐỔI 2: Khi ĐỔI MÀU, chỉ cập nhật màu ===
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // debouncedSendColor(200); // Dòng cũ
                debouncedSendColorOnly(); // Dòng MỚI: Gửi lệnh chỉ màu (Req 2)
            }
            // =============================================
            return false;
        });

        // PRESET COLORS
        for (View colorView : presetColorViews) {
            colorView.setOnClickListener(v -> {

                Boolean isOn = viewModel.getIsLedOn().getValue();
                if (isOn == null || !isOn) {
                    showToast(TOAST_TURN_ON_MSG);
                    return; // Không làm gì cả
                }


                int color = v.getBackgroundTintList() != null
                    ? v.getBackgroundTintList().getDefaultColor()
                    : Color.WHITE;

                viewModel.setCurrentColor(color);

                // === THAY ĐỔI 3: Khi ĐỔI MÀU, chỉ cập nhật màu ===
                // sendCurrentColor(); // Dòng cũ
                sendColorOnlyCommand(); // Dòng MỚI: Gửi lệnh chỉ màu (Req 2)
                // =============================================
            });
        }

        // BRIGHTNESS SLIDER (Giữ nguyên, logic đã đúng)
        brightnessSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {

                Boolean isOn = viewModel.getIsLedOn().getValue();
                if (isOn == null || !isOn) {
                    Integer oldBrightness = viewModel.getCurrentBrightness().getValue();
                    if (oldBrightness != null) {
                        slider.setValue(oldBrightness);
                    }
                    return;
                }

                viewModel.setCurrentBrightness((int) value);
            }
        });

        brightnessSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                Boolean isOn = viewModel.getIsLedOn().getValue();
                if (isOn == null || !isOn) {
                    showToast(TOAST_TURN_ON_MSG);
                }
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                Boolean isOn = viewModel.getIsLedOn().getValue();
                if (isOn != null && isOn) {
                    debouncedSendBrightness();
                }
            }
        });
    }

    /**
     * Gửi lệnh MÀU TĨNH (mode=static, color, brightness)
     * Dùng khi bật công tắc
     */
    private void sendCurrentColor() {
        if (isSending.get()) {
            Log.d(TAG, "Already sending, skipping...");
            return;
        }

        Integer colorValue = viewModel.getCurrentColor().getValue();
        Integer brightnessValue = viewModel.getCurrentBrightness().getValue();

        int color = (colorValue != null) ? colorValue : Color.WHITE;
        int brightness = (brightnessValue != null) ? brightnessValue : 50;

        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);


        Log.d(TAG, String.format("Sending STATIC color: #%02X%02X%02X @ %d%%", r, g, b, brightness));

        isSending.set(true);

        executor.execute(() -> {
            try {
                // Hàm này gọi mode=static trong LedController
                String result = ledController.setColor(r, g, b, brightness);
                Log.d(TAG, "Static color set success: " + result);

            } catch (IOException e) {
                Log.e(TAG, "Failed to set color", e);
                showToast(TOAST_ERROR_MSG);

                // Turn off switch on failure
                mainHandler.post(() -> {
                    viewModel.setIsLedOn(false);
                });

            } finally {
                isSending.set(false);
            }
        });
    }

    // === THÊM HÀM MỚI (Dùng cho Req 2) ===
    /**
     * Gửi lệnh CẬP NHẬT MÀU (KHÔNG đổi mode/brightness)
     * Dùng khi chọn màu từ bánh xe / preset
     */
    private void sendColorOnlyCommand() {
        if (isSending.get()) {
            Log.d(TAG, "Already sending, skipping color update...");
            return;
        }

        Integer colorValue = viewModel.getCurrentColor().getValue();
        int color = (colorValue != null) ? colorValue : Color.WHITE;

        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        Log.d(TAG, String.format("Updating color: #%02X%02X%02X (No Mode/Brightness Change)", r, g, b));

        isSending.set(true);

        executor.execute(() -> {
            try {
                // Hàm này KHÔNG gửi mode/brightness trong LedController
                String result = ledController.updateColorOnly(r, g, b);
                Log.d(TAG, "Color update success: " + result);

            } catch (IOException e) {
                Log.e(TAG, "Failed to update color", e);
                showToast(TOAST_ERROR_MSG);
                // Không cần tắt switch ở đây, vì đây chỉ là cập nhật
            } finally {
                isSending.set(false);
            }
        });
    }
    // ======================================

    /**
     * Send brightness only command (brightness=...)
     */
    private void sendCurrentBrightness() {
        if (isSending.get()) {
            Log.d(TAG, "Already sending, skipping...");
            return;
        }

        Integer brightnessValue = viewModel.getCurrentBrightness().getValue();
        int brightness = (brightnessValue != null) ? brightnessValue : 50;

        isSending.set(true);

        executor.execute(() -> {
            try {
                String result = ledController.setBrightness(brightness);

            } catch (IOException e) {
                Log.e(TAG, "Failed to set brightness", e);
                showToast(TOAST_ERROR_MSG);

            } finally {
                isSending.set(false);
            }
        });
    }

    /**
     * Turn off LED (brightness = 0)
     */
    private void sendTurnOff() {
        Log.d(TAG, "Turning off LED");

        executor.execute(() -> {
            try {
                String result = ledController.turnOff();
                Log.d(TAG, "Turn off success: " + result);

            } catch (IOException e) {
                Log.e(TAG, "Failed to turn off", e);
                // === THAY ĐỔI: Dùng hằng số ===
                showToast(TOAST_ERROR_MSG);
                // ==============================

                // Revert switch on failure
                mainHandler.post(() -> {
                    viewModel.setIsLedOn(true);
                });
            }
        });
    }

    // === THAY ĐỔI: Đổi tên hàm debounce (Dùng cho Req 2) ===
    /**
     * Debounce cho lệnh CẬP NHẬT MÀU
     */
    private void debouncedSendColorOnly() {
        if (pendingColorUpdate != null) {
            mainHandler.removeCallbacks(pendingColorUpdate);
        }

        pendingColorUpdate = this::sendColorOnlyCommand; // Trỏ đến hàm MỚI
        mainHandler.postDelayed(pendingColorUpdate, 200);
    }
    // ==============================================

    /**
     * Debounce brightness updates to prevent spam
     */
    private void debouncedSendBrightness() {
        if (pendingBrightnessUpdate != null) {
            mainHandler.removeCallbacks(pendingBrightnessUpdate);
        }

        pendingBrightnessUpdate = this::sendCurrentBrightness;
        mainHandler.postDelayed(pendingBrightnessUpdate, 100);
    }

    // =========================================================================
    // UTILITIES
    // =========================================================================

    private void showToast(String message) {
        if (getActivity() == null) return;

        mainHandler.post(() -> {
            if (getActivity() != null && isAdded()) {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Cancel pending updates
        if (pendingColorUpdate != null) {
            mainHandler.removeCallbacks(pendingColorUpdate);
        }
        if (pendingBrightnessUpdate != null) {
            mainHandler.removeCallbacks(pendingBrightnessUpdate);
        }

        // Shutdown executor
        executor.shutdownNow();

        // Clear view references
        presetColorViews.clear();
    }
}