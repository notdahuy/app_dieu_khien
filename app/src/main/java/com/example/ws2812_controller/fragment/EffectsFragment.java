package com.example.ws2812_controller.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.adapter.EffectAdapter;
import com.example.ws2812_controller.model.Effect;
import com.example.ws2812_controller.model.LedController; // Import
import com.example.ws2812_controller.model.SharedLedViewModel;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService; // Import
import java.util.concurrent.Executors; // Import

public class EffectsFragment extends Fragment {

    private static final String TAG = "EffectsFragment";

    // === THÊM CÁC HẰNG SỐ ===
    private static final String TOAST_ERROR_MSG = "Connection Failed (Check Wi-Fi)";
    private static final String TOAST_TURN_ON_MSG = "TURN ON FIRST";
    // ========================

    // Thêm các biến điều khiển
    private LedController ledController;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private RecyclerView listEffects;
    private TabLayout tabLayout;
    private Slider speedSlider;
    private TextView speedValueText;
    private EffectAdapter adapter;
    private List<Effect> normalEffects;
    private List<Effect> soundEffects; // Sẽ dùng sau

    private SharedLedViewModel viewModel; // Lưu tốc độ hiện tại

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo LedController
        ledController = new LedController(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(SharedLedViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_effects, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        listEffects = view.findViewById(R.id.listEffects);
        speedSlider = view.findViewById(R.id.speedSlider);
        speedValueText = view.findViewById(R.id.speedValue);

        setupTabs();
        setupRecyclerView();
        setupSpeedSlider();

        viewModel.getCurrentEffect().observe(getViewLifecycleOwner(), effectName -> {
            if (adapter != null) {
                adapter.setSelectedEffect(effectName);
            }
        });

        // Lắng nghe thay đổi tốc độ
        viewModel.getCurrentSpeed().observe(getViewLifecycleOwner(), speed -> {
            // === THAY ĐỔI: Kiểm tra trước khi set ===
            if ((int) speedSlider.getValue() != speed) {
                speedSlider.setValue(speed);
            }
            speedValueText.setText(String.valueOf(speed));
        });

        return view;
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("NORMAL MODE"));
        tabLayout.addTab(tabLayout.newTab().setText("SOUND REACTIVE")); // Tạm thời ẩn

        // === THAY ĐỔI: Cập nhật danh sách khớp với firmware ===
        normalEffects = new ArrayList<>();
        // "Static" đã ở bên ColorFragment, không cần ở đây
        normalEffects.add(new Effect("Static"));
        normalEffects.add(new Effect("Rainbow"));
        normalEffects.add(new Effect("Breathe"));
        normalEffects.add(new Effect("Color Wipe"));
        normalEffects.add(new Effect("Comet"));
        normalEffects.add(new Effect("Scanner"));
        normalEffects.add(new Effect("Theater Chase"));
        normalEffects.add(new Effect("Bounce"));

        // Tạm thời vô hiệu hóa
        tabLayout.setVisibility(View.GONE);

        // Logic tab cũ (khi nào có audio thì mở lại)
        soundEffects = new ArrayList<>();
        soundEffects.add(new Effect("VU Meter"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    adapter.updateList(normalEffects);
                } else {
                    adapter.updateList(soundEffects);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

    }

    private void setupRecyclerView() {
        adapter = new EffectAdapter(normalEffects); // Mặc định là list normal
        listEffects.setLayoutManager(new LinearLayoutManager(getContext()));
        listEffects.setAdapter(adapter);

        // === THAY ĐỔI: Gửi lệnh khi click ===
        adapter.setOnEffectClickListener(effect -> {

            // === THÊM KIỂM TRA "IS ON" ===
            Boolean isOn = viewModel.getIsLedOn().getValue();
            if (isOn == null || !isOn) {
                showToast(TOAST_TURN_ON_MSG);
                return; // Dừng lại, không làm gì cả
            }
            // =============================

            String effectName = effect.getName();
            showToast("Đã chọn: " + effectName);

            // Chuyển "Color Wipe" -> "colorwipe"
            String modeApiName = effectName.toLowerCase().replace(" ", "");

            viewModel.setCurrentEffect(modeApiName);

            // Gửi lệnh qua thread riêng
            sendEffectCommand(modeApiName);
        });
    }

    // === THAY ĐỔI: Xử lý speed slider ===
    private void setupSpeedSlider() {
        // 1. Đặt giá trị ban đầu cho cả Slider và Text
        Integer speedValue = viewModel.getCurrentSpeed().getValue();
        int initialSpeed = (speedValue != null) ? speedValue : 128;

        speedSlider.setValue(initialSpeed);
        speedValueText.setText(String.valueOf(initialSpeed));

        // 2. THÊM LẠI BỘ LẮNG NGHE KHI "ĐANG KÉO"
        speedSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {

                // === THÊM KIỂM TRA "IS ON" ===
                Boolean isOn = viewModel.getIsLedOn().getValue();
                if (isOn == null || !isOn) {
                    // Ngăn không cho trượt, trả về giá trị cũ
                    Integer oldSpeed = viewModel.getCurrentSpeed().getValue();
                    if (oldSpeed != null) {
                        slider.setValue(oldSpeed);
                    }
                    return; // Dừng, không cập nhật ViewModel
                }
                // =============================

                viewModel.setCurrentSpeed((int) value);
            }
        });

        // 3. XỬ LÝ KHI "THẢ TAY" (để gửi lệnh)
        speedSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(@NonNull Slider slider) {

                // === THÊM KIỂM TRA "IS ON" (ĐỂ HIỆN TOAST) ===
                Boolean isOn = viewModel.getIsLedOn().getValue();
                if (isOn == null || !isOn) {
                    showToast(TOAST_TURN_ON_MSG);
                }
                // ===========================================
            }

            @Override public void onStopTrackingTouch(@NonNull Slider slider) {

                // === THÊM KIỂM TRA "IS ON" (ĐỂ GỬI LỆNH) ===
                Boolean isOn = viewModel.getIsLedOn().getValue();
                if (isOn != null && isOn) {
                    sendSpeedCommand();
                }
                // ========================================
            }
        });
    }

    // === CÁC HÀM GỬI LỆNH MỚI ===

    private void sendEffectCommand(String modeName) {
        executor.execute(() -> {
            try {
                // Đọc tốc độ hiện tại từ ViewModel
                Integer speedValue = viewModel.getCurrentSpeed().getValue();
                int speed = (speedValue != null) ? speedValue : 128;
                String result = ledController.setEffect(modeName, speed);
            }
            catch (IOException e) {
                Log.e(TAG, "Failed to set effect", e);
                showToast(TOAST_ERROR_MSG); // <-- Dùng Toast mới
            }
        });
    }

    private void sendSpeedCommand() {
        executor.execute(() -> {
            try {
                // Dùng hàm setSpeed() đã có
                Integer speedValue = viewModel.getCurrentSpeed().getValue();
                int speed = (speedValue != null) ? speedValue : 128;
                String result = ledController.setSpeed(speed);
            } catch (IOException e) {
                Log.e(TAG, "Failed to set speed", e);
                showToast(TOAST_ERROR_MSG); // <-- Dùng Toast mới
            }
        });
    }

    // === HÀM HỖ TRỢ ===

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
        executor.shutdownNow(); // Đóng executor
    }
}