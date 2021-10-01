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

public class StepSensor extends BroadcastReceiver implements SensorEventListener  {
    private final static String TAG = "StepSensor";

    DbHandler dbHandler;

    private SensorManager mSensorManager = null;
    private boolean mLogging = false;
    private static int previousValue = 0;
    private static int currentValue = 0;

    private static Date mDate;
    private String command = "None";
    Context mParent = null;
    private boolean mAutoOff = false;

    public StepSensor(Context context, int timeoutInSeconds)
    {
        mParent = context;
        mDate = new Date();

        IntentFilter intentFilter = new IntentFilter("blah");
        context.registerReceiver(this,intentFilter);

        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("blah");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), timeoutInSeconds*1000, pendingIntent);

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        onStart(true);
    }


    public boolean onStart(boolean autoOff) {
        mAutoOff = autoOff;
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

        Log.i(TAG,"Steps "+ countSteps + "          Delta:" + currentValue);

        sync();

        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent("custom-event-name");
        LocalBroadcastManager.getInstance(mParent).sendBroadcast(intent);
        if (mAutoOff)
            onStop();
        mAutoOff = false;
    }

    void sync()
    {
        if (currentValue>0) {
            dbHandler.insertData(0, 2, mDate.getTime() / 1000, currentValue, 0);
        }
        mDate = new Date();
    }
}
