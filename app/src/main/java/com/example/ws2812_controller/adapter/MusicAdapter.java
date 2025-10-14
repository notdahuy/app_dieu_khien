package com.example.ws2812_controller.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ws2812_controller.model.Music;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter cho ListView dùng layout sẵn có: android.R.layout.simple_list_item_2
 * - text1: title
 * - text2: artist + duration
 */
public class MusicAdapter extends ArrayAdapter<Music> {

    public interface OnMusicClickListener {
        void onMusicClick(Music item, int position);
        // Bạn có thể bổ sung onLongClick nếu cần
    }

    private final LayoutInflater inflater;
    private final List<Music> data = new ArrayList<>();
    private OnMusicClickListener listener;
    private int selectedIndex = -1; // để highlight bài đang phát

    public MusicAdapter(@NonNull Context ctx, @NonNull List<Music> items) {
        super(ctx, android.R.layout.simple_list_item_2, items);
        inflater = LayoutInflater.from(ctx);
        data.addAll(items);
    }

    public void setOnMusicClickListener(@Nullable OnMusicClickListener l) {
        this.listener = l;
    }

    /** Cập nhật danh sách mới */
    public void setItems(@NonNull List<Music> items) {
        data.clear();
        data.addAll(items);
        notifyDataSetChanged();
    }

    /** Thêm 1 bài và refresh */
    public void addItem(@NonNull Music item) {
        data.add(item);
        notifyDataSetChanged();
    }

    /** Đánh dấu bài đang phát (highlight) */
    public void setSelectedIndex(int index) {
        // clear cờ cũ
        if (selectedIndex >= 0 && selectedIndex < data.size()) {
            data.get(selectedIndex).setSelected(false);
        }
        selectedIndex = index;
        if (selectedIndex >= 0 && selectedIndex < data.size()) {
            data.get(selectedIndex).setSelected(true);
        }
        notifyDataSetChanged();
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    @Override public int getCount() { return data.size(); }
    @Nullable @Override public Music getItem(int position) { return data.get(position); }
    @Override public long getItemId(int position) { return position; }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        ViewHolder h;
        if (v == null) {
            v = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
            h = new ViewHolder();
            h.text1 = v.findViewById(android.R.id.text1);
            h.text2 = v.findViewById(android.R.id.text2);
            v.setTag(h);
        } else {
            h = (ViewHolder) v.getTag();
        }

        final Music item = data.get(position);

        // Dòng 1: Title
        h.text1.setText(item.getTitle() != null ? item.getTitle() : "(untitled)");
        h.text1.setTypeface(null, item.isSelected() ? Typeface.BOLD : Typeface.NORMAL);

        // Dòng 2: Artist • mm:ss (nếu có)
        String artist = item.getArtist();
        String dur = item.getDurationMs() > 0 ? item.getDurationText() : null;
        StringBuilder sub = new StringBuilder();
        if (artist != null && artist.trim().length() > 0) sub.append(artist.trim());
        if (dur != null) {
            if (sub.length() > 0) sub.append(" • ");
            sub.append(dur);
        }
        h.text2.setText(sub.length() == 0 ? "" : sub.toString());

        // Highlight nhẹ cho bài đang phát
        v.setAlpha(item.isSelected() ? 1.0f : 0.95f);

        // Click item
        v.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (listener != null) listener.onMusicClick(item, position);
            }
        });

        return v;
    }

    static class ViewHolder {
        TextView text1;  // title
        TextView text2;  // artist + duration
    }
}
