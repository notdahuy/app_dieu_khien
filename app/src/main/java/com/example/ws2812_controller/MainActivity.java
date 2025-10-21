package com.example.ws2812_controller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import android.graphics.Insets;
import android.os.Bundle;
import android.view.MenuItem;

import com.example.ws2812_controller.fragment.ColorFragment;
import com.example.ws2812_controller.fragment.EffectsFragment;
import com.example.ws2812_controller.fragment.MusicFragment;
import com.example.ws2812_controller.fragment.SettingFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;

    private final Fragment colorFragment = new ColorFragment();
    private final Fragment effectsFragment = new EffectsFragment();
    private final Fragment musicFragment = new MusicFragment();
    private final Fragment settingFragment = new SettingFragment();

    private Fragment activeFragment = colorFragment; // fragment mặc định

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).toPlatformInsets();
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottom_nav);

        // Khởi tạo tất cả fragment, chỉ hiển thị 1
        getSupportFragmentManager().beginTransaction()
            .add(R.id.fragment_container, settingFragment, "settings").hide(settingFragment)
            .add(R.id.fragment_container, effectsFragment, "effects").hide(effectsFragment)
            .add(R.id.fragment_container, colorFragment, "color").hide(colorFragment)
            .add(R.id.fragment_container, musicFragment, "music")
            .commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_color) {
                switchFragment(colorFragment);
                return true;
            } else if (id == R.id.menu_effects) {
                switchFragment(effectsFragment);
                return true;
            } else if (id == R.id.menu_music) {
                switchFragment(musicFragment);
                return true;
            } else if (id == R.id.menu_settings) {
                switchFragment(settingFragment);
                return true;
            }

            return false;
        });

        // chọn tab mặc định
        bottomNavigationView.setSelectedItemId(R.id.menu_color);
    }

    private void switchFragment(Fragment target) {
        if (activeFragment == target) return;

        getSupportFragmentManager()
            .beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commit();

        activeFragment = target;
    }
}
