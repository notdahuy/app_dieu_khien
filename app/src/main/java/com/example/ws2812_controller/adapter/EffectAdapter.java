package com.example.ws2812_controller.adapter;

import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ws2812_controller.R;
import com.example.ws2812_controller.model.Effect;

import java.util.ArrayList;
import java.util.List;

public class EffectAdapter extends RecyclerView.Adapter<EffectAdapter.ViewHolder> {

    public interface OnEffectClickListener {
        void onEffectClick(Effect effect);
    }
    public interface OnSelectionChanged {
        void onSelection(List<Effect> selectedInOrder);
    }

    private final List<Effect> effects;
    // Danh sách theo thứ tự chọn, loại bỏ lỗi “bắt đầu từ 2”
    private final List<Effect> order = new ArrayList<>();

    private final OnEffectClickListener clickListener;   // có thể null
    private final OnSelectionChanged selectionCallback;  // có thể null

    // Constructor 2 tham số (click)
    public EffectAdapter(List<Effect> effects, OnEffectClickListener clickListener) {
        this.effects = effects;
        this.clickListener = clickListener;
        this.selectionCallback = null;
        resetSelection();
    }

    // Constructor 3 tham số (click + callback danh sách chọn)
    public EffectAdapter(List<Effect> effects,
                         OnEffectClickListener clickListener,
                         OnSelectionChanged selectionCallback) {
        this.effects = effects;
        this.clickListener = clickListener;
        this.selectionCallback = selectionCallback;
        resetSelection();
    }

    private void resetSelection() {
        order.clear();
        for (Effect x : effects) {
            x.setSelected(false);
            x.setSelectedOrder(0);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_effect, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder h, int position) {
        final Effect e = effects.get(position);

        // Tên + dải màu preview
        h.tvName.setText(e.getName());
        if (h.colorLine.getBackground() != null) {
            h.colorLine.getBackground().setColorFilter(e.getPreviewColor(), PorterDuff.Mode.SRC_IN);
        } else {
            h.colorLine.setBackgroundColor(e.getPreviewColor());
        }

        // Highlight theo state selected (nếu item_effect.xml dùng @drawable/bg_item_effect)
        h.itemView.setSelected(e.isSelected());
        h.itemView.setAlpha(e.isSelected() ? 1.0f : 0.95f);

        // Badge thứ tự (1,2,3,...) nếu có view
        if (h.tvOrderBadge != null) {
            if (e.isSelected() && e.getSelectedOrder() > 0) {
                h.tvOrderBadge.setVisibility(View.VISIBLE);
                h.tvOrderBadge.setText(String.valueOf(e.getSelectedOrder()));
            } else {
                h.tvOrderBadge.setVisibility(View.GONE);
                h.tvOrderBadge.setText("");
            }
        }

        // Click: toggle chọn + cập nhật order + callback
        h.itemView.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                toggleSelection(e);
                int pos = h.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) notifyItemChanged(pos);

                if (selectionCallback != null) selectionCallback.onSelection(getSelectedOrdered());
                if (clickListener != null) clickListener.onEffectClick(e);
            }
        });
    }

    @Override public int getItemCount() { return effects.size(); }

    private void toggleSelection(Effect e) {
        if (!e.isSelected()) {
            // CHỌN: thêm vào cuối order
            e.setSelected(true);
            order.add(e);
            e.setSelectedOrder(order.size()); // 1..N
        } else {
            // BỎ CHỌN: xoá khỏi order và đánh số lại
            e.setSelected(false);
            e.setSelectedOrder(0);
            order.remove(e);
            for (int i = 0; i < order.size(); i++) {
                order.get(i).setSelectedOrder(i + 1);
            }
        }
        // cập nhật toàn bộ để badge các item sau cũng thay đổi
        notifyDataSetChanged();
    }

    /** Trả về danh sách đã chọn THEO THỨ TỰ chọn */
    public List<Effect> getSelectedOrdered() {
        return new ArrayList<>(order);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        View colorLine;
        TextView tvOrderBadge; // có thể null nếu layout không có

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvEffectName);
            colorLine = itemView.findViewById(R.id.viewColorLine);
            tvOrderBadge = itemView.findViewById(R.id.tvOrderBadge);
        }
    }
}
