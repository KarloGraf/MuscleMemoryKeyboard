package com.example.musclememorykeyboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class CustomKeyboardOverlay extends KeyboardView {
    private Paint paint;

    public CustomKeyboardOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomKeyboardOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.BLACK); // Black color for lines
        paint.setStrokeWidth(3); // Line thickness
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void onDraw(Canvas canvas) {
        init();
        super.onDraw(canvas);
        Log.d("ONDRAWCALLED", "ondrawcalled");

        // Example: Drawing lines for each key row
        int width = getWidth();
        int height = getHeight();

        // Calculate positions using percentages
        int rowHeight = height / 4;  // Assuming 4 rows

        //Top and corners
        canvas.drawLine(0, 2, width, 2, paint);
        canvas.drawLine(2, 0, 2, rowHeight, paint);
        canvas.drawLine(width-2, 0, width-2, rowHeight, paint);

        // First row
        canvas.drawLine(0, rowHeight, (int)(width*0.05), rowHeight, paint);
        canvas.drawLine((int)(width*0.95), rowHeight, width, rowHeight, paint);
        canvas.drawLine((int)(width*0.05), rowHeight, (int)(width*0.05), rowHeight*2, paint);
        canvas.drawLine((int)(width*0.95), rowHeight, (int)(width*0.95), rowHeight*2, paint);

        //Second row
        canvas.drawLine((int)(width*0.05), rowHeight*2, (int)(width*0.15), rowHeight*2, paint);
        canvas.drawLine((int)(width*0.85), rowHeight*2, (int)(width*0.95), rowHeight*2, paint);
        canvas.drawLine((int)(width*0.15), rowHeight*2, (int)(width*0.15), rowHeight*3, paint);
        canvas.drawLine((int)(width*0.85), rowHeight*2, (int)(width*0.85), rowHeight*3, paint);

        //Third row
        canvas.drawLine((int)(width*0.15), rowHeight*3, (int)(width*0.85), rowHeight*3, paint);
    }
}
