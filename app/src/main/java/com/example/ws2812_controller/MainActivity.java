package com.example.ws2812_controller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
// import android.widget.Toolbar; // Không dùng, có thể bỏ nếu không cần

import com.example.ws2812_controller.fragment.ColorFragment;
import com.example.ws2812_controller.fragment.EffectsFragment;
import com.example.ws2812_controller.fragment.SettingFragment;
import com.example.ws2812_controller.fragment.TimerFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Nếu layout có view id=main, giữ insets như bạn đang làm
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).toPlatformInsets();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            }
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottom_nav);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment target = null;
                int id = item.getItemId();

                if (id == R.id.menu_color) {
                    target = new ColorFragment();
                } else if (id == R.id.menu_effects) {
                    target = new EffectsFragment();
                } else if (id == R.id.menu_timer) {
                    target = new TimerFragment();
                } else if (id == R.id.menu_settings) {
                    target = new SettingFragment();
                }

                if (target != null) {
                    switchTo(target);
                    return true;
                }
                return false;
            }
        });


        bottomNavigationView.setOnItemReselectedListener(new NavigationBarView.OnItemReselectedListener() {
            @Override public void onNavigationItemReselected(@NonNull MenuItem item) { /* no-op */ }
        });

        if (savedInstanceState == null) {
            // Tải ColorFragment
            switchTo(new ColorFragment());
            // Đặt icon "Color" ở bottom nav thành "selected"
            bottomNavigationView.setSelectedItemId(R.id.menu_color);
        }
    }

    private void switchTo(Fragment target) {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current != null && current.getClass() == target.getClass()) return; // đang ở đúng tab

        getSupportFragmentManager()
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, target)
                .commit();
    }
}
