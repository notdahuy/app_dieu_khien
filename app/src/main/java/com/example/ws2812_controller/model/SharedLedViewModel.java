package com.example.ws2812_controller.model;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SharedLedViewModel extends ViewModel {

    // 1. Trạng thái Bật/Tắt
    private final MutableLiveData<Boolean> isLedOn = new MutableLiveData<>(false);
    public LiveData<Boolean> getIsLedOn() { return isLedOn; }
    public void setIsLedOn(boolean isOn) { isLedOn.setValue(isOn); }

    // 2. Trạng thái Màu
    private final MutableLiveData<Integer> currentColor = new MutableLiveData<>(Color.WHITE);
    public LiveData<Integer> getCurrentColor() { return currentColor; }
    public void setCurrentColor(int color) { currentColor.setValue(color); }

    // 3. Trạng thái Độ sáng
    private final MutableLiveData<Integer> currentBrightness = new MutableLiveData<>(50);
    public LiveData<Integer> getCurrentBrightness() { return currentBrightness; }
    public void setCurrentBrightness(int brightness) { currentBrightness.setValue(brightness); }

    // 4. Trạng thái Tốc độ
    private final MutableLiveData<Integer> currentSpeed = new MutableLiveData<>(128);
    public LiveData<Integer> getCurrentSpeed() { return currentSpeed; }
    public void setCurrentSpeed(int speed) { currentSpeed.setValue(speed); }

    // 5. Trạng thái Hiệu ứng (dùng tên API, vd: "static", "rainbow")
    private final MutableLiveData<String> currentEffect = new MutableLiveData<>("static");
    public LiveData<String> getCurrentEffect() { return currentEffect; }
    public void setCurrentEffect(String effectName) { currentEffect.setValue(effectName); }

    // 6. List hiệu ứng
    private static final List<String> NORMAL_EFFECTS = Arrays.asList(
        "Static", "Rainbow", "Breathe", "Color Wipe",
        "Comet", "Scanner", "Theater Chase", "Bounce"
    );

    public List<String> getNormalEffects() {
        return new ArrayList<>(NORMAL_EFFECTS);
    }
}