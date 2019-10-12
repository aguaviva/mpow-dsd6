package com.example.dsd6;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Date;

class DrawChart extends View {

    Paint paintGray = new Paint();
    private Paint paintRed = new Paint();
    private Paint paintWhiteText = new Paint();

    private float[] lines = new float[1024*2];

    ArrayList<DbHandler.BandActivity> mData;
    long mIniTime, mFinTime;

    public DrawChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintRed.setColor(Color.RED);
        paintRed.setStrokeWidth(8);

        paintGray.setColor(Color.GRAY);
        paintGray.setStrokeWidth(2);

        paintWhiteText.setColor(Color.WHITE);
        paintWhiteText.setStyle(Paint.Style.FILL);
        paintWhiteText.setTextSize(20);
    }

    void AddPoints(java.util.Date ini, java.util.Date fin, ArrayList<DbHandler.BandActivity> data)
    {
        mIniTime = ini.getTime()/1000;
        mFinTime = fin.getTime()/1000;

        mData = data;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int step = 60;

        int maxY = (200);
        if (canvas!=null ) {

            // draw hour ticks
            for(int i=0;i<24;i++)
            {
                float x = (i * width) / 24;
                canvas.drawLine(x,0, x, height, paintGray);
                canvas.drawText(""+i, x, 0+paintWhiteText.getTextSize(), paintWhiteText);
            }

            for(int i=0;i<4;i++)
            {
                float y = (i * height) / 4;
                canvas.drawLine(0,y, width, y, paintGray);
            }

            for(int i=50;i<100;i+=10)
            {
                float y = height - ((i * height) / maxY);
                canvas.drawLine(0, y, width, y, paintGray);
            }

            if (mData!=null)
            {
                for (DbHandler.BandActivity data : mData) {
                    if (data.type == 1) {
                        int index = (int) (((data.timestamp - mIniTime) * width) / (mFinTime - mIniTime));

                        int x1 = index - 1;
                        int x2 = index + 1;
                        int y = (data.value * height) / maxY;

                        canvas.drawRect(x1, height - 0, x2, height - y, paintRed);
                    }
                }
            }

        }

    }
}
