package com.example.ws2812_controller.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.ws2812_controller.model.LedController;
import com.example.ws2812_controller.model.SharedLedViewModel;
import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EffectsFragment extends Fragment {

    private static final String TAG = "EffectsFragment";
    private static final String PREFS_NAME = "EffectsPrefs";
    private static final String KEY_FAVORITES = "favorites";

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
    private TextInputEditText searchEditText;
    private TextView emptyFavoriteText;
    private EffectAdapter adapter;
    private List<Effect> normalEffects;
    private List<Effect> soundEffects;
    private List<Effect> favoriteEffects;
    private List<Effect> allEffects; // Danh sách tất cả effects để tìm kiếm
    private int currentTabPosition = 0; // Lưu tab hiện tại
    private Set<String> favoriteSet = new HashSet<>(); // Lưu tên effect yêu thích

    private SharedLedViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo LedController
        ledController = new LedController(requireContext());
        viewModel = new ViewModelProvider(requireActivity()).get(SharedLedViewModel.class);

        // Load danh sách yêu thích từ SharedPreferences
        loadFavorites();
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
        searchEditText = view.findViewById(R.id.searchEditText);
        emptyFavoriteText = view.findViewById(R.id.emptyFavoriteText);

        setupTabs();
        setupRecyclerView();
        setupSpeedSlider();
        setupSearch();

        viewModel.getCurrentEffect().observe(getViewLifecycleOwner(), effectName -> {
            if (adapter != null) {
                adapter.setSelectedEffect(effectName);
            }
        });

        // Lắng nghe thay đổi tốc độ
        viewModel.getCurrentSpeed().observe(getViewLifecycleOwner(), speed -> {
            if ((int) speedSlider.getValue() != speed) {
                speedSlider.setValue(speed);
            }
            speedValueText.setText(String.valueOf(speed));
        });

        return view;
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Thường"));
        tabLayout.addTab(tabLayout.newTab().setText("Âm thanh"));
        tabLayout.addTab(tabLayout.newTab().setText("Yêu thích"));

        // Danh sách chế độ thường
        normalEffects = new ArrayList<>();
        normalEffects.add(new Effect("Static"));
        normalEffects.add(new Effect("Rainbow"));
        normalEffects.add(new Effect("Breathe"));
        normalEffects.add(new Effect("Color Wipe"));
        normalEffects.add(new Effect("Comet"));
        normalEffects.add(new Effect("Scanner"));
        normalEffects.add(new Effect("Theater Chase"));
        normalEffects.add(new Effect("Bounce"));

        // Danh sách chế độ âm thanh
        soundEffects = new ArrayList<>();
        soundEffects.add(new Effect("VU"));
        soundEffects.add(new Effect("Volume Bar"));

        // === THAY ĐỔI: Tạo allEffects TRƯỚC KHI gọi updateFavoriteList() ===
        allEffects = new ArrayList<>();
        allEffects.addAll(normalEffects);
        allEffects.addAll(soundEffects);

        // Danh sách yêu thích - tạo từ favoriteSet
        favoriteEffects = new ArrayList<>();
        updateFavoriteList(); // BÂY GIỜ MỚI GỌI HÀM NÀY

        // Hiển thị TabLayout
        tabLayout.setVisibility(View.VISIBLE);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTabPosition = tab.getPosition();
                // Xóa text tìm kiếm khi chuyển tab
                if (searchEditText != null) {
                    searchEditText.setText("");
                }
                updateEffectList();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEffects(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterEffects(String query) {
        if (query.isEmpty()) {
            // Nếu không có text tìm kiếm, hiển thị list theo tab hiện tại
            updateEffectList();
            return;
        }

        // Tìm kiếm trong tất cả effects
        List<Effect> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (Effect effect : allEffects) {
            if (effect.getName().toLowerCase().contains(lowerQuery)) {
                filteredList.add(effect);
            }
        }

        adapter.updateList(filteredList);
        // Ẩn empty text khi đang tìm kiếm
        emptyFavoriteText.setVisibility(View.GONE);
        listEffects.setVisibility(View.VISIBLE);
    }

    private void updateEffectList() {
        switch (currentTabPosition) {
            case 0: // NORMAL
                adapter.updateList(normalEffects);
                emptyFavoriteText.setVisibility(View.GONE);
                listEffects.setVisibility(View.VISIBLE);
                break;
            case 1: // SOUND
                adapter.updateList(soundEffects);
                emptyFavoriteText.setVisibility(View.GONE);
                listEffects.setVisibility(View.VISIBLE);
                break;
            case 2: // FAVORITE
                adapter.updateList(favoriteEffects);
                // Hiển thị text nếu danh sách yêu thích trống
                if (favoriteEffects.isEmpty()) {
                    emptyFavoriteText.setVisibility(View.VISIBLE);
                    listEffects.setVisibility(View.GONE);
                } else {
                    emptyFavoriteText.setVisibility(View.GONE);
                    listEffects.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    private void setupRecyclerView() {
        adapter = new EffectAdapter(normalEffects); // Mặc định là list normal
        listEffects.setLayoutManager(new LinearLayoutManager(getContext()));
        listEffects.setAdapter(adapter);

        // Set danh sách yêu thích vào adapter
        adapter.setFavoriteEffects(favoriteSet);

        // Xử lý click vào effect
        adapter.setOnEffectClickListener(effect -> {

            // Kiểm tra trạng thái bật/tắt
            Boolean isOn = viewModel.getIsLedOn().getValue();
            if (isOn == null || !isOn) {
                showToast(TOAST_TURN_ON_MSG);
                return;
            }

            String effectName = effect.getName();
            showToast("Đã chọn: " + effectName);

            // Chuyển đổi tên effect
            String modeApiName = effectName.toLowerCase().replace(" ", "");

            viewModel.setCurrentEffect(modeApiName);

            // Gửi lệnh qua thread riêng
            sendEffectCommand(modeApiName);
        });

        // Xử lý click vào nút yêu thích
        adapter.setOnFavoriteClickListener((effect, isFavorite) -> {
            String effectApiName = effect.getName().toLowerCase().replace(" ", "");

            if (isFavorite) {
                // Thêm vào yêu thích
                favoriteSet.add(effectApiName);
                showToast("Đã thêm vào yêu thích");
            } else {
                // Xóa khỏi yêu thích
                favoriteSet.remove(effectApiName);
                showToast("Đã xóa khỏi yêu thích");
            }

            // Lưu vào SharedPreferences
            saveFavorites();

            // Cập nhật danh sách yêu thích
            updateFavoriteList();

            // Nếu đang ở tab yêu thích, cập nhật lại danh sách
            if (currentTabPosition == 2) {
                adapter.updateList(favoriteEffects);
                // Cập nhật visibility của empty text
                if (favoriteEffects.isEmpty()) {
                    emptyFavoriteText.setVisibility(View.VISIBLE);
                    listEffects.setVisibility(View.GONE);
                } else {
                    emptyFavoriteText.setVisibility(View.GONE);
                    listEffects.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setupSpeedSlider() {
        // Đặt giá trị ban đầu
        Integer speedValue = viewModel.getCurrentSpeed().getValue();
        int initialSpeed = (speedValue != null) ? speedValue : 128;

        speedSlider.setValue(initialSpeed);
        speedValueText.setText(String.valueOf(initialSpeed));

        // Lắng nghe khi đang kéo
        speedSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {

                Boolean isOn = viewModel.getIsLedOn().getValue();
                if (isOn == null || !isOn) {
                    Integer oldSpeed = viewModel.getCurrentSpeed().getValue();
                    if (oldSpeed != null) {
                        slider.setValue(oldSpeed);
                    }
                    return;
                }

                viewModel.setCurrentSpeed((int) value);
            }
        });

        // Xử lý khi thả tay
        speedSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override public void onStartTrackingTouch(@NonNull Slider slider) {
                Boolean isOn = viewModel.getIsLedOn().getValue();
                if (isOn == null || !isOn) {
                    showToast(TOAST_TURN_ON_MSG);
                }
            }

            @Override public void onStopTrackingTouch(@NonNull Slider slider) {
                Boolean isOn = viewModel.getIsLedOn().getValue();
                if (isOn != null && isOn) {
                    sendSpeedCommand();
                }
            }
        });
    }

    // === XỬ LÝ YÊU THÍCH ===

    private void loadFavorites() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        favoriteSet = prefs.getStringSet(KEY_FAVORITES, new HashSet<>());
        // Tạo bản sao mới để tránh lỗi khi modify
        favoriteSet = new HashSet<>(favoriteSet);
    }

    private void saveFavorites() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putStringSet(KEY_FAVORITES, favoriteSet).apply();
    }

    private void updateFavoriteList() {
        favoriteEffects.clear();

        // Duyệt qua tất cả effects và thêm vào favoriteEffects nếu có trong favoriteSet
        for (Effect effect : allEffects) {
            String effectApiName = effect.getName().toLowerCase().replace(" ", "");
            if (favoriteSet.contains(effectApiName)) {
                favoriteEffects.add(effect);
            }
        }
    }

    // === CÁC HÀM GỬI LỆNH ===

    private void sendEffectCommand(String modeName) {
        executor.execute(() -> {
            try {
                Integer speedValue = viewModel.getCurrentSpeed().getValue();
                int speed = (speedValue != null) ? speedValue : 128;
                String result = ledController.setEffect(modeName, speed);
            }
            catch (IOException e) {
                Log.e(TAG, "Failed to set effect", e);
                showToast(TOAST_ERROR_MSG);
            }
        });
    }

    private void sendSpeedCommand() {
        executor.execute(() -> {
            try {
                Integer speedValue = viewModel.getCurrentSpeed().getValue();
                int speed = (speedValue != null) ? speedValue : 128;
                String result = ledController.setSpeed(speed);
            } catch (IOException e) {
                Log.e(TAG, "Failed to set speed", e);
                showToast(TOAST_ERROR_MSG);
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
        executor.shutdownNow();
    }
}