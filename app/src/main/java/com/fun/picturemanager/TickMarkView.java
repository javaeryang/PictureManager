package com.fun.picturemanager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class TickMarkView extends View {
    private Paint paint;
    private int totalFrames = 0;
    private double fps = 0;

    public TickMarkView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.LTGRAY);
        paint.setStrokeWidth(3f);
        paint.setStyle(Paint.Style.STROKE);
    }

    public void setVideoData(int totalFrames, double fps) {
        this.totalFrames = totalFrames;
        this.fps = fps;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (totalFrames <= 0 || fps <= 0) return;

        int width = getWidth();
        int height = getHeight();
        
        // Adjust for SeekBar padding (approximate, standard SeekBar has some padding)
        float padding = 32f; // Default thumb offset/padding
        float drawWidth = width - 2 * padding;

        for (int frame = 0; frame < totalFrames; frame++) {
            // Only draw for full seconds
            if (frame % (int) fps == 0) {
                float x = padding + (frame * drawWidth / (totalFrames - 1));
                canvas.drawLine(x, 0, x, height, paint);
            }
        }
    }
}
