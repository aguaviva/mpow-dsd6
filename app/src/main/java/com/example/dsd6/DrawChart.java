package com.example.dsd6;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

class DrawChart extends View {

    Paint paintGray = new Paint();
    private Paint paintRed = new Paint();
    private Paint paintBlue = new Paint();
    private Paint paintGreen = new Paint();
    private Paint paintYellow = new Paint();
    private Paint paintWhiteText = new Paint();
    private Paint paintAwardText = new Paint();
    private Paint paintPurple = new Paint();
    private Paint paintDarkPurple = new Paint();
    private Paint paintCyan = new Paint();

    private Paint mPaintWalkingColor = paintBlue;

    private float[] lines = new float[1024*2];

    ArrayList<DbHandler.BandActivity> mData;
    long mIniTime, mFinTime;
    int mHIni, mHFin;
    int mPadding;
    int mWidth, mHeight;
    int xLeft, xRight, yTop, yBottom;

    long mMask = 0xff;
    public DrawChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        paintRed.setColor(Color.RED);
        paintRed.setStrokeWidth(8);

        paintBlue.setColor(Color.BLUE);
        paintBlue.setStrokeWidth(4);
        paintGreen.setColor(Color.GREEN);
        paintGreen.setStrokeWidth(4);
        paintYellow.setColor(Color.YELLOW);
        paintYellow.setStrokeWidth(4);

        paintGray.setColor(Color.GRAY);
        paintGray.setStrokeWidth(2);

        paintCyan.setColor(Color.CYAN);
        paintCyan.setStrokeWidth(6);

        paintPurple.setColor(Color.argb(255,128,128,255));
        paintDarkPurple.setColor(Color.argb(255,0,0,192));

        paintWhiteText.setColor(Color.WHITE);
        paintWhiteText.setStyle(Paint.Style.FILL);
        paintWhiteText.setTextSize(25);
        paintWhiteText.setTextAlign(Paint.Align.CENTER);

        paintAwardText.setColor(Color.WHITE);
        paintAwardText.setStyle(Paint.Style.FILL);
        paintAwardText.setTextSize(30);
        paintAwardText.setTextAlign(Paint.Align.CENTER);

        mPadding = (int)paintWhiteText.getTextSize();
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
        if (xspan==0)
            return min;
        int t = (x-minx);
        return min + (t * (max-min)) / xspan;
    }

    int mapX(int x, int minx, int maxx)
    {
        int min = mPadding;
        int max = mWidth - mPadding;
        return map(x, minx, maxx, min, max);
    }

    int mapY(int x, int minx, int maxx)
    {
        int min = mPadding;
        int max = mHeight - 3*mPadding/2;  //double padding at the top for the numbers
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

    void drawStar(Canvas canvas, float px,float py, float r, Paint p)
    {
        float x = 0;
        float y = r*0.5f;
        for(int i=1;i<11;i++)
        {
            float a = (2.0f*3.1415f*(float)i)/10.0f;
            float rr = ((i&1)==1)?r:(r * .5f);
            float xx = rr*(float)Math.sin(a);
            float yy = rr*(float)Math.cos(a);
            canvas.drawLine(px+x, py+y, px+xx, py+yy, p);
            x=xx;
            y=yy;
        }
    }

    void DrawFrame(Canvas canvas)
    {
        for(int i=0;i<=4;i++) {
            int y = mapY(i, 0, 4);
            canvas.drawLine(xLeft, y, xRight, y, paintGray);
        }

        for(int i=50;i<100;i+=10) {
            int y = mapY(i, 0, 200);
            canvas.drawLine(xLeft, y, xRight, y, paintGray);
        }

        // draw hour ticks
        for(int i=0;i<=12;i++) {
            int x = mapX(i, 0, 12);
            int h = mHIni +  i*(mHFin -mHIni)/12;

            canvas.drawLine(x, yTop, x, yBottom, paintGray);
            canvas.drawText(""+h, x, yBottom-mPadding/3, paintWhiteText);
        }
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

            DrawFrame(canvas);

            if (mData!=null) {

                //count steps walking and running
                int totalSteps = 0;
                for (DbHandler.BandActivity data : mData) {
                    if (data.type==0) {
                        totalSteps+= data.value;
                    }
                }

                // steps stuff
                int stepsForStar=5000;
                int totalStepsMax = (int)(Math.ceil((float)totalSteps/stepsForStar)*stepsForStar);
                totalStepsMax = Math.max(totalStepsMax, stepsForStar*2);
                int starCount=0;
                List<Integer> startList = new ArrayList<Integer>();
                int lastStepX = mapX((int)mIniTime, (int)mIniTime, (int)mFinTime);
                int stepsAcc = 0;

                for (DbHandler.BandActivity data : mData) {

                    if (data.timestamp>mFinTime)
                        break;

                    int x = mapX((int) data.timestamp, (int) mIniTime, (int) mFinTime);

                    if (data.type==0) {
                        //
                        // steps
                        //
                        if ((mMask & 2) > 0) {

                            //make long horizontal lines blue
                            if (x-lastStepX>10)
                                mPaintWalkingColor = paintBlue;

                            int y0 = mapY(stepsAcc, 0, totalStepsMax);
                            canvas.drawLine(lastStepX, y0, x, y0, mPaintWalkingColor);
                            stepsAcc += data.value;
                            lastStepX = x;

                            int y1 = mapY(stepsAcc, 0, totalStepsMax);

                            switch(data.flags) {
                                case 1:   mPaintWalkingColor = paintYellow; break;// steps, fast walking
                                case 2:   mPaintWalkingColor = paintBlue; break;// steps, walking
                                case 3:   mPaintWalkingColor = paintGreen; break;// steps, running
                                default:  mPaintWalkingColor = paintCyan;
                            }

                            canvas.drawLine(x, y0, x, y1, mPaintWalkingColor);

                            // mark awards positions
                            if (stepsAcc>stepsForStar*(starCount+1))
                            {
                                startList.add(x);
                                starCount++;
                            }
                        }
                    } else if (data.type==3) {
                        //
                        // heart rate
                        //
                        if (data.flags == 1) {
                            if ((mMask & 1) > 0)
                                drawBar(canvas, x, data.value, 200, paintRed); //heart rate
                        } else {
                            // not seen
                            drawBar(canvas, x, 1000, 1000, paintGreen);
                        }
                    } else if (data.type==7) {
                        //
                        // sleeping quality
                        //
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
                                    p = paintDarkPurple;  // deep sleep
                                else
                                    p = paintGray; // unknown

                                int xx0 = mapX((int) data.timestamp - eventDuration + 60, (int) mIniTime, (int) mFinTime);
                                canvas.drawRect(xx0, yy0, x, yy1, p);
                                break;
                            }
                            case 3: { //end sleeping
                                drawBar(canvas, x, data.value & 0xf, 10, paintYellow);
                                break;
                            }
                            default: { // not seen
                                drawBar(canvas, x, 1000, 1000, paintGreen);
                            }
                        }
                    } else {
                        drawBar(canvas, x, 1000, 1000, paintCyan); // unknown
                    }
                }

                // more steps drawing: awards, misc
                if ((mMask & 2) > 0) {
                    //horizontal blue bar until the end
                    int y0 = mapY(stepsAcc, 0, totalStepsMax);
                    canvas.drawLine(lastStepX, y0, xRight, y0, paintBlue);
                    paintWhiteText.setTextAlign(Paint.Align.RIGHT);
                    canvas.drawText("" + stepsAcc, xRight, y0, paintWhiteText);
                    paintWhiteText.setTextAlign(Paint.Align.CENTER);

                    // draw stars
                    for (int c = 0; c < startList.size(); c++) {
                        int steps = (c + 1) * stepsForStar;
                        int x = startList.get(c);
                        int y = mapY(steps, 0, totalStepsMax);
                        drawStar(canvas, x, y, 20, paintYellow);
                        canvas.drawText("" + steps, x, y + 50, paintAwardText);
                    }
                }
            }
        }
    }
}
