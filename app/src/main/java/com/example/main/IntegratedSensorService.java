package com.example.main;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;


import com.example.stepper.R;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.main.DbHandler;

import java.util.Date;

public class IntegrafhfhtedSensorService extends Service  implements SensorEventListener {

    public static final String KEY_SENSOR_TYPE = "sensor_type";
    public static final String KEY_COMMAND = "command";
    public static final String KEY_LOGGING = "logging";

    DbHandler dbHandler;

    private SensorManager mSensorManager = null;
    private boolean mLogging = false;
    private static int previousValue = 0;
    private static Date date;
    private String command = "None";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // get sensor manager on starting the service
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // have a default sensor configured
        command = "None";

        Bundle args = intent.getExtras();

        if (date==null)
        {
            date = new Date();
        }

        // get some properties from the intent
        if (args != null) {

            if (args.containsKey(KEY_COMMAND)) {
                command = args.getString(KEY_COMMAND);
            }

            // optional logging
            mLogging = args.getBoolean(KEY_LOGGING);
        }

        // we need the light sensor
        Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);

        dbHandler = new DbHandler(this);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

        int deltaStepCounter = countSteps - previousValue;
        previousValue = countSteps;

        Log.e("Alarm","steps " + deltaStepCounter + " command:" + command);

        if (command.equals("SYNC"))
        {
            if (deltaStepCounter>0)
                dbHandler.insertData(0, 2, date.getTime()/1000, deltaStepCounter, 0);
            date = new Date();
        }
        else if (command.equals("BLOCK"))
        {
            if (deltaStepCounter>0)
                dbHandler.insertData(0, 2, date.getTime()/1000, deltaStepCounter, 0);
            date = new Date();
        }

        // stop the sensor and service
        mSensorManager.unregisterListener(this);
        stopSelf();
    }
}

