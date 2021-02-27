package com.example.main;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.Message;

import androidx.core.app.NotificationCompat;

import android.util.Log;
import android.widget.Toast;

import com.example.integratedSensor.IntegratedSensorService;
import com.example.stepper.R;

import java.util.Random;

/**
 * this is an example of a service that prompts itself to a foreground service with a persistent
 * notification.  Which is now required by Oreo otherwise, a background service without an app will be killed.
 */

public class MyService extends Service {

    private final static String TAG = "MyForegroundService";


    @Override
    public void onCreate()
    {
        StartSensorRecording();
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent,int flags,int startId)
    {
        return super.onStartCommand(intent,flags,startId);
    }
    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void ShowNotification()
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(getBaseContext(),"notification_id")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("title")
                .setContentText("content")
                .setDefaults(NotificationCompat.DEFAULT_SOUND)
                .build();
        notificationManager.notify(0, notification);
        //the notification is not showing
    }


    private void StartSensorRecording()
    {
        AlarmManager scheduler = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, IntegratedSensorService.class);
        Bundle args = new Bundle();
        args.putString(IntegratedSensorService.KEY_COMMAND, "SYNC");
        intent.putExtras(args);
        PendingIntent scheduledIntent = PendingIntent.getService(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long interval = 1000L;
        scheduler.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, scheduledIntent);
    }
}