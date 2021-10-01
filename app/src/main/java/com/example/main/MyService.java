package com.example.main;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.stepper.R;

import java.util.Date;

import static androidx.core.app.ActivityCompat.requestPermissions;

/**
 * this is an example of a service that prompts itself to a foreground service with a persistent
 * notification.  Which is now required by Oreo otherwise, a background service without an app will be killed.
 */

public class MyService extends Service {

    private final static String TAG = "MyForegroundService";
    private static final String NOTIFICATION_CHANNEL_ID = "123456";
    private static final CharSequence NOTIFICATION_CHANNEL_NAME = "cacacsedfa";
    //private static final String ANDROID_CHANNEL_ID = 3424325;
    private StepSensor mStepSensor = null;

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            //String message = intent.getStringExtra("message");
            //Log.d("receiver", "Got message: " + message);
            Date now = new Date();
            String str = new java.text.SimpleDateFormat("hh:mm:ss").format(now);
            ShowNotification("Update: "+str);
        }
    };


//    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int id = 234578632;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder builder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name));
            Notification notification = builder.build();
            startForeground(id, notification);
        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name));
            Notification notification = builder.build();
            startForeground(id, notification);
        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.i(TAG, "onCreate()");
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("custom-event-name"));

        ShowNotification("Starting");
        mStepSensor = new StepSensor(this, 10);
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "onDestroy()");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void ShowNotification(String str)
    {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);

        // create channel in new versions of android
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        Intent intent = new Intent(this, com.example.main.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new NotificationCompat.Builder(getBaseContext(),NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                //.setContentTitle("title")
                .setContentText(str)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();

        notificationManager.notify(0, notification);
    }
}