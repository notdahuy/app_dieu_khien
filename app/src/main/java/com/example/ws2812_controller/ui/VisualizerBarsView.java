package com.example.ws2812_controller.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class VisualizerBarsView extends View {

    private final Paint paintBass = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintMid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTreble = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float levelBass = 0f, levelMid = 0f, levelTreble = 0f; // 0..1
    private float smoothBass = 0f, smoothMid = 0f, smoothTreble = 0f;
    private float smoothing = 0.15f; // độ mượt

    public VisualizerBarsView(Context c) { super(c); init(); }
    public VisualizerBarsView(Context c, AttributeSet a) { super(c, a); init(); }
    public VisualizerBarsView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        paintBass.setColor(0xFFFF5252);   // đỏ cam
        paintMid.setColor(0xFF40C4FF);    // xanh dương
        paintTreble.setColor(0xFF69F0AE); // xanh lục
    }

    /** Nhận giá trị 0..255, tự scale về 0..1 */
    public void setLevels255(int bass, int mid, int treble) {
        setLevels(bass / 255f, mid / 255f, treble / 255f);
    }

    /** Nhận trực tiếp 0..1 */
    public void setLevels(float bass, float mid, float treble) {
        levelBass = clamp01(bass);
        levelMid = clamp01(mid);
        levelTreble = clamp01(treble);
        invalidate();
    }

    private float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // smoothing
        smoothBass   += (levelBass   - smoothBass)   * smoothing;
        smoothMid    += (levelMid    - smoothMid)    * smoothing;
        smoothTreble += (levelTreble - smoothTreble) * smoothing;

        int gap = dp(12);
        int barWidth = (w - gap * 4) / 3;
        int x1 = gap, x2 = x1 + barWidth + gap, x3 = x2 + barWidth + gap;

        // Vẽ từ đáy lên
        canvas.drawRect(x1, h - h * smoothBass,   x1 + barWidth, h, paintBass);
        canvas.drawRect(x2, h - h * smoothMid,    x2 + barWidth, h, paintMid);
        canvas.drawRect(x3, h - h * smoothTreble, x3 + barWidth, h, paintTreble);

        // tự animate nhẹ nhàng
        if (Math.abs(levelBass - smoothBass) > 0.001f
                || Math.abs(levelMid - smoothMid) > 0.001f
                || Math.abs(levelTreble - smoothTreble) > 0.001f) {
            postInvalidateOnAnimation();
        }
    }

    private int dp(int d) {
        float den = getResources().getDisplayMetrics().density;
        return (int) (d * den + 0.5f);
    }
}
