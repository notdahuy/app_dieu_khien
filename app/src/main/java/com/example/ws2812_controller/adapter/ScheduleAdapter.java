package com.example.ws2812_controller.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.model.TimerSchedule;

import java.util.ArrayList;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.VH> {

    public interface Listener {
        void onToggle(String id, boolean enabled);
        void onDelete(String id);
    }

    private ArrayList<TimerSchedule> data;
    private final Listener listener;

    public ScheduleAdapter(ArrayList<TimerSchedule> data, Listener listener) {
        this.data = data;
        this.listener = listener;
    }

    public void update(ArrayList<TimerSchedule> newData) {
        this.data = newData;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTimeRange, tvDays;
        Switch sw;
        ImageButton btnDelete;
        VH(@NonNull View v) {
            super(v);
            tvTimeRange = v.findViewById(R.id.tvTimeRange);
            tvDays = v.findViewById(R.id.tvDays);
            sw = v.findViewById(R.id.swEnabled);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        TimerSchedule it = data.get(pos);
        h.tvTimeRange.setText(String.format("%02d:%02d - %02d:%02d",
                it.onHour, it.onMinute, it.offHour, it.offMinute));
        h.tvDays.setText(it.daysString());
        h.sw.setOnCheckedChangeListener(null);
        h.sw.setChecked(it.enabled);

        h.sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onToggle(String.valueOf(it.id), isChecked);
        });

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(String.valueOf(it.id));
        });
    }

    @Override
    public int getItemCount() { return data.size(); }
}
