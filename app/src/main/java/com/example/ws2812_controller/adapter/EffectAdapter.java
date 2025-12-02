package com.example.ws2812_controller.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.model.Effect;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EffectAdapter extends RecyclerView.Adapter<EffectAdapter.EffectViewHolder> {

    private final List<Effect> effects = new ArrayList<>();
    private final Set<String> favoriteEffects = new HashSet<>();
    private String selectedEffectName = "static";
    private OnEffectClickListener onEffectClickListener;
    private OnFavoriteClickListener onFavoriteClickListener;

    // Giao diện callback để fragment dễ bắt sự kiện
    public interface OnEffectClickListener {
        void onEffectClick(Effect effect);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Effect effect, boolean isFavorite);
    }

    public EffectAdapter(List<Effect> effects) {
        if (effects != null) {
            this.effects.addAll(effects);
        }
    }

    public void setOnEffectClickListener(OnEffectClickListener listener) {
        this.onEffectClickListener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.onFavoriteClickListener = listener;
    }

    // Cập nhật danh sách yêu thích
    public void setFavoriteEffects(Set<String> favorites) {
        favoriteEffects.clear();
        if (favorites != null) {
            favoriteEffects.addAll(favorites);
        }
        notifyDataSetChanged();
    }

    // Thêm/xóa yêu thích
    public void toggleFavorite(String effectName) {
        if (favoriteEffects.contains(effectName)) {
            favoriteEffects.remove(effectName);
        } else {
            favoriteEffects.add(effectName);
        }

        // Cập nhật UI cho item này
        int index = findIndexOfEffect(effectName);
        if (index != -1) {
            notifyItemChanged(index);
        }
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

        // Mặc định
        holder.effectIndicator.setVisibility(View.GONE);
        holder.itemRoot.setBackgroundResource(R.drawable.item_effect_bg);

        String effectApiName = effect.getName().toLowerCase().replace(" ", "");

        // Nếu item được chọn
        if (effectApiName.equals(selectedEffectName)) {
            holder.effectIndicator.setVisibility(View.VISIBLE);
            holder.itemRoot.setBackgroundResource(R.drawable.item_effect_active);
        } else {
            holder.effectIndicator.setVisibility(View.GONE);
            holder.itemRoot.setBackgroundResource(R.drawable.item_effect_bg);
        }

        // Cập nhật icon yêu thích
        if (favoriteEffects.contains(effectApiName)) {
            holder.btnFavorite.setImageResource(android.R.drawable.star_big_on);
        } else {
            holder.btnFavorite.setImageResource(android.R.drawable.star_big_off);
        }

        // Click vào item
        holder.itemView.setOnClickListener(v -> {
            if (onEffectClickListener != null) {
                onEffectClickListener.onEffectClick(effect);
            }
        });

        // Click vào nút yêu thích
        holder.btnFavorite.setOnClickListener(v -> {
            boolean isFavorite = favoriteEffects.contains(effectApiName);

            if (onFavoriteClickListener != null) {
                onFavoriteClickListener.onFavoriteClick(effect, !isFavorite);
            }

            // Toggle trạng thái
            toggleFavorite(effectApiName);
        });
    }

    @Override
    public int getItemCount() {
        return effects.size();
    }

    public void setSelectedEffect(String effectName) {
        if (effectName == null || effectName.equals(selectedEffectName)) {
            return;
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
        return -1;
    }

    public Set<String> getFavoriteEffects() {
        return new HashSet<>(favoriteEffects);
    }

    static class EffectViewHolder extends RecyclerView.ViewHolder {
        View itemRoot, effectIndicator;
        TextView tvEffectName;
        ImageView btnFavorite;

        public EffectViewHolder(@NonNull View itemView) {
            super(itemView);
            itemRoot = itemView.findViewById(R.id.itemRoot);
            effectIndicator = itemView.findViewById(R.id.effectIndicator);
            tvEffectName = itemView.findViewById(R.id.tvEffectName);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}