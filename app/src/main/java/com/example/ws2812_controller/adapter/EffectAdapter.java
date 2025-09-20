package com.example.ws2812_controller.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.model.Effect;

import java.util.List;

public class EffectAdapter extends RecyclerView.Adapter<EffectAdapter.ViewHolder> {

    private List<Effect> effects;
    private OnEffectClickListener listener;

    public interface OnEffectClickListener {
        void onEffectClick(Effect effect);
    }

    public EffectAdapter(List<Effect> effects, OnEffectClickListener listener) {
        this.effects = effects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_effect, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Effect effect = effects.get(position);
        holder.tvName.setText(effect.getName());
        holder.colorLine.setBackgroundColor(effect.getPreviewColor());

        holder.itemView.setOnClickListener(v -> listener.onEffectClick(effect));
    }

    @Override
    public int getItemCount() {
        return effects.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        View colorLine;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEffectName);
            colorLine = itemView.findViewById(R.id.viewColorLine);
        }
    }
}
