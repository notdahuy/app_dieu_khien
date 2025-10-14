package com.example.ws2812_controller.model;

import android.net.Uri;

public class Music {
    private Uri uri;            // đường dẫn SAF
    private String title;       // tên hiển thị
    private String artist;      // nghệ sĩ (có thể null/"")
    private long durationMs;    // thời lượng (ms), có thể 0 nếu chưa biết

    private boolean selected;   // để highlight item đang phát

    public Music(Uri uri, String title) {
        this(uri, title, "", 0L);
    }

    public Music(Uri uri, String title, String artist, long durationMs) {
        this.uri = uri;
        this.title = title != null ? title : "";
        this.artist = artist != null ? artist : "";
        this.durationMs = Math.max(0, durationMs);
        this.selected = false;
    }

    public Uri getUri() { return uri; }
    public void setUri(Uri uri) { this.uri = uri; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title != null ? title : ""; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist != null ? artist : ""; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = Math.max(0, durationMs); }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    /** mm:ss dạng 03:27 */
    public String getDurationText() {
        long totalSec = durationMs / 1000L;
        long m = totalSec / 60L;
        long s = totalSec % 60L;
        return String.format("%02d:%02d", m, s);
    }
}
