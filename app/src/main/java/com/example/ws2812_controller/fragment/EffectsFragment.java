package com.example.ws2812_controller.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.adapter.EffectAdapter;
import com.example.ws2812_controller.model.Effect;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class EffectsFragment extends Fragment {

    private RecyclerView listEffects;
    private TabLayout tabLayout;
    private SeekBar seekBarSpeed;
    private EffectAdapter adapter;

    private List<Effect> normalEffects;
    private List<Effect> soundEffects;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_effects, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        listEffects = view.findViewById(R.id.listEffects);
        seekBarSpeed = view.findViewById(R.id.seekBarSpeed);

        setupTabs();
        setupRecyclerView();
        setupSeekBar();

        return view;
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("NORMAL MODE"));
        tabLayout.addTab(tabLayout.newTab().setText("SOUND REACTIVE"));

        // Danh sách hiệu ứng thường
        normalEffects = new ArrayList<>();
        normalEffects.add(new Effect("Static"));
        normalEffects.add(new Effect("Rainbow"));
        normalEffects.add(new Effect("Wave"));
        normalEffects.add(new Effect("Pulse"));
        normalEffects.add(new Effect("Sparkle"));
        normalEffects.add(new Effect("Firefly"));
        normalEffects.add(new Effect("Fade"));

        // Danh sách hiệu ứng âm thanh
        soundEffects = new ArrayList<>();
        soundEffects.add(new Effect("VU Meter"));
        soundEffects.add(new Effect("Bass Pulse"));
        soundEffects.add(new Effect("Beat Glow"));
        soundEffects.add(new Effect("Audio Wave"));
        soundEffects.add(new Effect("Music Spectrum"));

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
        adapter = new EffectAdapter(normalEffects);
        listEffects.setLayoutManager(new LinearLayoutManager(getContext()));
        listEffects.setAdapter(adapter);

        adapter.setOnEffectClickListener(effect -> {
            Toast.makeText(getContext(), "Đã chọn: " + effect.getName(), Toast.LENGTH_SHORT).show();
            // TODO: gửi lệnh điều khiển hiệu ứng sang ESP32 tại đây
        });
    }

    private void setupSeekBar() {
        seekBarSpeed.setMax(100);
        seekBarSpeed.setProgress(50);

        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // TODO: gửi giá trị tốc độ đến ESP32
                    // Ví dụ:
                    // sendSpeedToESP32(progress);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}
