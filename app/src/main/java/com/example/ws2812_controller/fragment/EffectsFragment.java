package com.example.ws2812_controller.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.adapter.EffectAdapter;
import com.example.ws2812_controller.model.Effect;

import java.util.ArrayList;
import java.util.List;

public class EffectsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EffectAdapter adapter;
    private List<Effect> effects;

    private SeekBar seekSpeed;
    private TextView tvSpeed;
    private Button btnApply;

    private int speedPercent = 50; // 0..100

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_effects, container, false);

        recyclerView = view.findViewById(R.id.recyclerEffects);
        seekSpeed    = view.findViewById(R.id.seekSpeed);
        tvSpeed      = view.findViewById(R.id.tvSpeed);
        btnApply     = view.findViewById(R.id.btnApply);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Danh sách hiệu ứng mẫu
        effects = new ArrayList<>();
        effects.add(new Effect("Rainbow",  Color.MAGENTA));
        effects.add(new Effect("Breathe",  Color.CYAN));
        effects.add(new Effect("Chase",    Color.YELLOW));
        effects.add(new Effect("Blink",    Color.RED));
        effects.add(new Effect("Flashing", Color.GREEN));
        effects.add(new Effect("Fading",   Color.BLUE));
        effects.add(new Effect("Cycling",  Color.rgb(255,165,0)));
        effects.add(new Effect("Jumping",  Color.LTGRAY));

        adapter = new EffectAdapter(
                effects,
                new EffectAdapter.OnEffectClickListener() {
                    @Override public void onEffectClick(Effect effect) {
                        if (getContext() != null) {
                            Toast.makeText(getContext(),
                                    "Chọn: " + effect.getName(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new EffectAdapter.OnSelectionChanged() {
                    @Override public void onSelection(List<Effect> selectedInOrder) {
                        // Bạn có thể cập nhật UI phụ ở đây (nếu muốn)
                    }
                }
        );

        recyclerView.setAdapter(adapter);

        // SeekBar speed
        tvSpeed.setText("Speed: " + speedPercent + "%");
        seekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speedPercent = progress;
                tvSpeed.setText("Speed: " + speedPercent + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        // Apply
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                applySelectedEffects();
            }
        });

        return view;
    }

    private void applySelectedEffects() {
        List<Effect> selected = adapter.getSelectedOrdered(); // ĐÚNG THỨ TỰ
        if (selected.isEmpty()) {
            Toast.makeText(getContext(), "Hãy chọn ít nhất 1 hiệu ứng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build CSV theo thứ tự: "RAINBOW,FLASHING,..."
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selected.size(); i++) {
            sb.append(selected.get(i).getName().toUpperCase());
            if (i < selected.size() - 1) sb.append(",");
        }
        String effectsCsv = sb.toString();

        int speed = mapSpeedToDevice(speedPercent);

        // TODO: Gửi lệnh tới ESP32 (HTTP/MQTT/Socket) theo API của bạn
        // Ví dụ HTTP:
        // String url = "http://<esp32-ip>/set_effects?list=" + URLEncoder.encode(effectsCsv, "UTF-8")
        //            + "&speed=" + speed;

        Toast.makeText(getContext(),
                "Apply: [" + effectsCsv + "] speed=" + speed,
                Toast.LENGTH_SHORT).show();
    }

    // Ví dụ map speed (0..100%) -> 20..255 (tùy firmware)
    private int mapSpeedToDevice(int percent) {
        int min = 20, max = 255;
        return min + (percent * (max - min) / 100);
    }
}
