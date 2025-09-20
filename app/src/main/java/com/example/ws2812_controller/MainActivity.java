package com.example.ws2812_controller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import android.graphics.Insets;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toolbar;

import com.example.ws2812_controller.fragment.ColorFragment;
import com.example.ws2812_controller.fragment.EffectsFragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    Toolbar toolbar;

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

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;

                int id = item.getItemId();
                if (id == R.id.menu_color) {
                    selectedFragment = new ColorFragment();
                } else if (id == R.id.menu_effects) {
                    selectedFragment = new EffectsFragment();
                } else if (id == R.id.menu_timer) {
//                    selectedFragment = new TimerFragment();
                } else if (id == R.id.menu_settings) {
//                    selectedFragment = new SettingsFragment();
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                }

                return true;
            }
        });

        // Mặc định mở tab "Color"
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.menu_color);
        }

    }

}
