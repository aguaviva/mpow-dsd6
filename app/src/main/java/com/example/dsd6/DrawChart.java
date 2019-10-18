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
    private Paint paintBlue = new Paint();
    private Paint paintGreen = new Paint();
    private Paint paintYellow = new Paint();
    private Paint paintWhiteText = new Paint();
    private Paint paintPurple = new Paint();
    private Paint paintCyan = new Paint();

    private float[] lines = new float[1024*2];

    ArrayList<DbHandler.BandActivity> mData;
    long mIniTime, mFinTime;
    int mHIni, mHFin;
    int mPpadding;
    int mWidth, mHeight;
    int xLeft, xRight, yTop, yBottom;

    long mMask = 0xff;
    public DrawChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintRed.setColor(Color.RED);
        paintRed.setStrokeWidth(8);

        paintBlue.setColor(Color.BLUE);
        paintBlue.setStrokeWidth(8);
        paintGreen.setColor(Color.GREEN);
        paintYellow.setColor(Color.YELLOW);
        paintBlue.setStrokeWidth(8);

        paintGray.setColor(Color.GRAY);
        paintGray.setStrokeWidth(2);

        paintCyan.setColor(Color.CYAN);
        paintCyan.setStrokeWidth(2);

        paintPurple.setColor(Color.argb(255,152,118,170));

        paintWhiteText.setColor(Color.WHITE);
        paintWhiteText.setStyle(Paint.Style.FILL);
        paintWhiteText.setTextSize(20);
        paintWhiteText.setTextAlign(Paint.Align.CENTER);

        mPpadding = (int)paintWhiteText.getTextSize();
    }
    public void setChannel(int channel, boolean state)
    {
        if (state)
            mMask |= (1<<channel);
        else
            mMask &= ~(1<<channel);
    }

    void AddPoints(int hIni, int hFin, java.util.Date ini, java.util.Date fin, ArrayList<DbHandler.BandActivity> data)
    {
        mIniTime = ini.getTime()/1000;
        mFinTime = fin.getTime()/1000;

        mHIni = hIni;
        mHFin = hFin;

        mData = data;
        invalidate();
    }

    int map(int x, int minx, int maxx, int min, int max)
    {
        int xspan = (maxx-minx);
        int t = (x-minx);
        return min + (t * (max-min)) / xspan;
    }

    int mapX(int x, int minx, int maxx)
    {
        int min = mPpadding;
        int max = mWidth - mPpadding;
        return map(x, minx, maxx, min, max);
    }

    int mapY(int x, int minx, int maxx)
    {
        int min = mPpadding;
        int max = mHeight - mPpadding;
        return mHeight - map(x, minx, maxx, min, max);
    }

    int map(long x, long minx, long maxx, long min, long max)
    {
        return map((int) x, (int) minx, (int) maxx, (int) min, (int) max);
    }

    void drawBar(Canvas canvas, int x, int y, int maxY, Paint p)
    {
        int yy = mapY(y, 0, maxY);
        canvas.drawRect(x - 1, yTop, x + 1, yy, p);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mWidth = getWidth();
        mHeight = getHeight();
        xLeft = mapX(0, 0, 4);
        xRight = mapX(4, 0, 4);
        yTop = mapY(0, 0, 4);
        yBottom = mapY(4, 0, 4);

        int step = 60;
        int maxY = 200;

        if (canvas!=null ) {
            // draw hour ticks
            for(int i=0;i<=12;i++) {
                int x = mapX(i, 0, 12);
                int h = mHIni +  i*(mHFin -mHIni)/12;

                canvas.drawLine(x, yTop, x, yBottom, paintGray);
                canvas.drawText(""+h, x, yBottom, paintWhiteText);
            }

            if (mData!=null) {
                for (DbHandler.BandActivity data : mData) {

                    int x = mapX((int) data.timestamp, (int) mIniTime, (int) mFinTime);

                    // types:
                    // 0- exercising
                    // 1- realtime exercising (this uses value2)
                    // 2- sleeping
                    // 3- sleeping heart rate

                    //
                    if (data.type==0) {
                        switch(data.flags)
                        {
                            case 1: { // steps, running?
                                if ((mMask & 4) > 0)
                                    drawBar(canvas, x, data.value, 1000, paintYellow);
                                break;
                            }
                            case 2: { //steps, walking
                                if ((mMask & 2) > 0)
                                    drawBar(canvas, x, data.value, 1000, paintBlue);
                                break;
                            }
                            default: { // unknown
                                drawBar(canvas, x, 1000, 1000, paintGreen);
                            }
                        }
                    }
                    else if (data.type==1) {
                        drawBar(canvas, x, 1000, 1000, paintCyan); // unknown
                    }
                    else if (data.type==2) {
                        drawBar(canvas, x, 1000, 1000, paintCyan); // unknown
                    }
                    else if (data.type==3) {
                        if (data.flags == 1) {
                            if ((mMask & 1) > 0)
                                drawBar(canvas, x, data.value, 200, paintRed); //heart rate
                        } else {
                            // not seen
                            drawBar(canvas, x, 1000, 1000, paintGreen);
                        }
                    }
                    else if (data.type==7) {
                        int eventDuration = 10 * 60; //in seconds
                        switch(data.flags) {
                            case 0: { //start sleeping
                                int xx = mapX((int) data.timestamp - eventDuration, (int) mIniTime, (int) mFinTime);
                                drawBar(canvas, xx, 100, 1000, paintYellow); // unknown
                                break;
                            }
                            case 1: { // sleep quality, the span of this is 10 minutes
                                Paint p;
                                int yy0 = mapY(maxY * 0, 0, maxY);
                                int yy1 = mapY(maxY * 1 / 10, 0, maxY);
                                if (data.value == 11)  // light sleep
                                    p = paintPurple;
                                else if (data.value == 12)
                                    p = paintBlue;  // deep sleep
                                else
                                    p = paintGray; // unknown

                                int xx0 = mapX((int) data.timestamp - eventDuration + 60, (int) mIniTime, (int) mFinTime);
                                canvas.drawRect(xx0, yy0, x, yy1, p);
                                break;
                            }
                            case 3: { //end sleeping
                                drawBar(canvas, x, data.value, 10000, paintYellow);
                                break;
                            }
                            default: { // not seen
                                drawBar(canvas, x, 1000, 1000, paintGreen);
                            }
                        }
                    }
                }
            }

            for(int i=0;i<=4;i++) {
                int y = mapY(i, 0, 4);
                canvas.drawLine(xLeft, y, xRight, y, paintGray);
            }

            for(int i=50;i<100;i+=10) {
                int y = mapY(i, 0, 200);
                canvas.drawLine(xLeft, y, xRight, y, paintGray);
            }
        }
    }
}
