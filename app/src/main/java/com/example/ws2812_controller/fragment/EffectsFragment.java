package com.example.ws2812_controller.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.adapter.EffectAdapter;
import com.example.ws2812_controller.model.Effect;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EffectsFragment extends Fragment {

    private RecyclerView recyclerView;
    private EffectAdapter adapter;
    private List<Effect> effects;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_effects, container, false);

        recyclerView = view.findViewById(R.id.recyclerEffects);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Tạo danh sách hiệu ứng mẫu
        effects = new ArrayList<>();
        effects.add(new Effect("Rainbow", Color.MAGENTA));
        effects.add(new Effect("Breathe", Color.CYAN));
        effects.add(new Effect("Chase", Color.YELLOW));
        effects.add(new Effect("Blink", Color.RED));

        adapter = new EffectAdapter(effects, effect -> {
            Toast.makeText(getContext(), "Chọn: " + effect.getName(), Toast.LENGTH_SHORT).show();

            // TODO: Gửi hiệu ứng tới ESP32 ở đây
            // ledController.setEffect(effect.getName());
        });

        recyclerView.setAdapter(adapter);

        return view;
    }
}
