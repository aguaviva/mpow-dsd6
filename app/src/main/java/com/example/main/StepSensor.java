package com.example.main;

import static android.content.Context.SENSOR_SERVICE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Calendar;
import java.util.Date;

public class StepSensor extends BroadcastReceiver  implements SensorEventListener  {
    private final static String TAG = "StepSensor";

    DbHandler dbHandler;

    private SensorManager mSensorManager = null;
    private boolean mLogging = false;
    private static int previousValue = 0;
    private static int currentValue = 0;

    private static Date mDate = new Date();;
    private String command = "None";
    Context mParent = null;
    private int lastTotalSteps = 0;
    private int totalSteps = 0;
    private int deltaSteps = 0;

    public StepSensor(Context context, int timeoutInSeconds)
    {
        mParent = context;

        IntentFilter intentFilter = new IntentFilter("blah");
        context.registerReceiver(this,intentFilter);

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("blah");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), timeoutInSeconds*1000, pendingIntent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //onStart(true);
        deltaSteps = totalSteps - lastTotalSteps;

        Log.i(TAG,"Total:" + totalSteps + "   Delta:" + deltaSteps);


        if (deltaSteps>0)
            sync(deltaSteps);
        lastTotalSteps = totalSteps;

        //Log.d("sender", "Broadcasting message");
        Intent intent2 = new Intent("custom-event-name");
        LocalBroadcastManager.getInstance(mParent).sendBroadcast(intent2);
    }


    public boolean onStart() {
        lastTotalSteps = 0;
        totalSteps = 0;
        deltaSteps = 0;

        Log.i(TAG,"onStart");

        // get sensor manager on starting the service
        mSensorManager = (SensorManager) mParent.getSystemService(SENSOR_SERVICE);

        // we need the light sensor
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        dbHandler = new DbHandler(mParent);
        return true;
    }

    public boolean onStop() {
        // stop the sensor and service
        mSensorManager.unregisterListener(this);
        return true;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) { }

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (previousValue == 0)
        {
            previousValue = (int) event.values[0];
        }

        int countSteps = (int) event.values[0];

        currentValue = countSteps - previousValue;
        previousValue = countSteps;

        totalSteps += currentValue;
    }

    void sync(int count)
    {
        dbHandler.insertData(0, 2, mDate.getTime() / 1000, count, 0);
    }

    public int getTotalSteps()
    {
        return totalSteps;
    }

    public int getDeltaSteps(){
        return deltaSteps;
    }

}
