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
    private String selectedEffectName = "static";
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
//        holder.itemView.setTag(effect);

        // Mặc định
        holder.effectIndicator.setVisibility(View.GONE);
        holder.itemRoot.setBackgroundResource(R.drawable.item_effect_bg);

        String effectApiName = effect.getName().toLowerCase().replace(" ", "");

        // Nếu item được chọn
        // So sánh với tên hiệu ứng đã lưu
        if (effectApiName.equals(selectedEffectName)) {
            // Đây là item được chọn
            holder.effectIndicator.setVisibility(View.VISIBLE);
            holder.itemRoot.setBackgroundResource(R.drawable.item_effect_active);
        } else {
            // Item bình thường
            holder.effectIndicator.setVisibility(View.GONE);
            holder.itemRoot.setBackgroundResource(R.drawable.item_effect_bg);
        }

        holder.itemView.setOnClickListener(v -> {
            // THAY ĐỔI: Adapter không tự cập nhật UI nữa
            // Nó chỉ báo cho Fragment biết
            if (onEffectClickListener != null) {
                onEffectClickListener.onEffectClick(effect);
            }
        });
    }

    @Override
    public int getItemCount() {
        return effects.size();
    }

    public void setSelectedEffect(String effectName) {
        if (effectName == null || effectName.equals(selectedEffectName)) {
            return; // Không có gì thay đổi
        }

        String oldSelectedName = selectedEffectName;
        selectedEffectName = effectName;

        // Tìm vị trí của 2 mục (cũ và mới) để cập nhật
        int oldIndex = findIndexOfEffect(oldSelectedName);
        int newIndex = findIndexOfEffect(selectedEffectName);

        if (oldIndex != -1) {
            notifyItemChanged(oldIndex);
        }
        if (newIndex != -1) {
            notifyItemChanged(newIndex);
        }
    }

    private int findIndexOfEffect(String effectApiName) {
        for (int i = 0; i < effects.size(); i++) {
            String apiName = effects.get(i).getName().toLowerCase().replace(" ", "");
            if (apiName.equals(effectApiName)) {
                return i;
            }
        }
        return -1; // Không tìm thấy
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
