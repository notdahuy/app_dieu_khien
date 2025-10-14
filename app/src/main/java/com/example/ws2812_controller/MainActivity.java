package com.example.ws2812_controller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import android.graphics.Insets;
import android.os.Bundle;
import android.view.MenuItem;
// import android.widget.Toolbar; // Không dùng, có thể bỏ nếu không cần

import com.example.ws2812_controller.fragment.ColorFragment;
import com.example.ws2812_controller.fragment.EffectsFragment;
import com.example.ws2812_controller.fragment.MusicFragment; // <-- THÊM IMPORT NÀY
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    // Toolbar toolbar; // Không dùng, có thể xoá nếu không setActionBar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Nếu layout có view id=main, giữ insets như bạn đang làm
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars()).toPlatformInsets();
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
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
                } else if (id == R.id.menu_music) {              // <-- THÊM CASE MUSIC
                    target = new MusicFragment();
                }
                if (target != null) {
                    switchTo(target);
                    return true;
                }
                return false;
            }
        });

        // Bấm lại cùng tab: không reload
        bottomNavigationView.setOnItemReselectedListener(new NavigationBarView.OnItemReselectedListener() {
            @Override public void onNavigationItemReselected(@NonNull MenuItem item) { /* no-op */ }
        });

        // Mặc định mở tab "Music" để bạn vào giao diện Music luôn (đổi sang menu_color nếu muốn)
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.menu_music); // <-- vào Music ngay
            // Nếu muốn vào Color mặc định: dùng R.id.menu_color
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
