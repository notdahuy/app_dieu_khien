package com.example.ws2812_controller.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.model.Effect;

import java.util.ArrayList;
import java.util.List;

public class EffectAdapter extends RecyclerView.Adapter<EffectAdapter.EffectViewHolder> {

    private final List<Effect> effects = new ArrayList<>();
    private int selectedPosition = -1;
    private OnEffectClickListener onEffectClickListener;

    // Giao diện callback để fragment dễ bắt sự kiện
    public interface OnEffectClickListener {
        void onEffectClick(Effect effect);
    }

    public EffectAdapter(List<Effect> effects) {
        if (effects != null) {
            this.effects.addAll(effects);
        }
    }

    public void setOnEffectClickListener(OnEffectClickListener listener) {
        this.onEffectClickListener = listener;
    }

    // Cập nhật danh sách (dùng khi đổi tab)
    public void updateList(List<Effect> newList) {
        effects.clear();
        if (newList != null) {
            effects.addAll(newList);
        }
        selectedPosition = -1; // reset chọn
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EffectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_list, parent, false);
        return new EffectViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EffectViewHolder holder, int position) {
        Effect effect = effects.get(position);
        holder.tvEffectName.setText(effect.getName());
        holder.itemView.setTag(effect);

        // Mặc định
        holder.effectIndicator.setVisibility(View.GONE);
        holder.itemRoot.setBackgroundResource(R.drawable.item_effect_bg);

        // Nếu item được chọn
        if (position == selectedPosition) {
            holder.effectIndicator.setVisibility(View.VISIBLE);
            holder.itemRoot.setBackgroundResource(R.drawable.item_effect_active);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // Cập nhật UI cho item cũ và mới
            if (oldPos != RecyclerView.NO_POSITION) notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);

            if (onEffectClickListener != null) {
                onEffectClickListener.onEffectClick(effect);
            }
        });
    }

    @Override
    public int getItemCount() {
        return effects.size();
    }

    static class EffectViewHolder extends RecyclerView.ViewHolder {
        View itemRoot, effectIndicator;
        TextView tvEffectName;

        public EffectViewHolder(@NonNull View itemView) {
            super(itemView);
            itemRoot = itemView.findViewById(R.id.itemRoot);
            effectIndicator = itemView.findViewById(R.id.effectIndicator);
            tvEffectName = itemView.findViewById(R.id.tvEffectName);
        }
    }
}
