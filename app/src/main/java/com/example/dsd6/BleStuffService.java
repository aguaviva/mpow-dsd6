package com.example.dsd6;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import android.os.Handler;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class BleStuffService extends Service {

    public static final String STATUS = "STATUS";
    public static final String RESULT = "RESULT";
    public static final String NOTIFICATION = "com.example.dsd6.service.receiver";

    int mNotificationId =23435345;

    String mDeviceAddress;
    private BluetoothAdapter mBluetoothAdapter;
    BlockProcessor mBlockProcessor;
    BlockProcessor.ProcessorCallbacks mMyCallbacks;

    DbHandler dbHandler;
    BleStuff mBleStuff;
    BleStuff.BleCallbacks mBleCallbacks;
    protected BleStuffService mBleStuffService = null;

    //so we can retry after a certain time
    final Handler handler = new Handler();
    Runnable retryGetBlocks = new Runnable() {
        @Override
        public void run() {
            mBlockProcessor.getBlocks();
        }
    };

    public void publishStatus(String status, int result) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(STATUS, status);
        intent.putExtra(RESULT, result);
        intent.setPackage("com.example.dsd6");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent==null)
            return Service.START_STICKY;

        Bundle bundle =intent.getExtras();
        if (bundle == null)
            return Service.START_STICKY;

        mDeviceAddress = bundle.getString("HOST");

        if (mBleStuffService!=null)
            return Service.START_STICKY;
        mBleStuffService = this;

        class MyBlockCallbacks implements BlockProcessor.ProcessorCallbacks
        {
            @Override
            public void setProgress(final int i)
            {
                mBleStuffService.publishStatus("Syncing: " + i + "%", i);
                if (i==100) {
                    mBleStuffService.publishStatus("done", 100);
                    mBleStuff.Disconnect();
                    stopSelf();
                }
            }
            @Override
            public void addText(final String str)
            {
               // AddText(str);
            }
            @Override
            public void send(String command)
            {
                mBleStuff.Send(command);
            }
        }
        mMyCallbacks = new MyBlockCallbacks();

        dbHandler = new DbHandler(BleStuffService.this);
        mBlockProcessor = new BlockProcessor(dbHandler, mMyCallbacks);

        // Ble callbacks
        //
        class MyBleCallbacks implements BleStuff.BleCallbacks
        {
            @Override
            public void onConnect()
            {
                mBleStuffService.publishStatus("connected",0);
            }
            @Override
            public void onDisconect()
            {
                mBleStuffService.publishStatus("disconnected",0);
                stopSelf();
            }
            @Override
            public void onFound()
            {
                publishStatus("device found",0);
                mBlockProcessor.init();
                mBlockProcessor.getBlocks();
            }
            @Override
            public void onData(byte[] data)
            {
                mBlockProcessor.process(data);
            }
            @Override
            public void onQueueEmpty()
            {
                //wait for 2 secs before checking I got all pieces
                handler.removeCallbacks(retryGetBlocks);
                handler.postDelayed(retryGetBlocks, 2000);
            }
        };
        mBleCallbacks = new MyBleCallbacks();

        dbHandler = new DbHandler(this);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBleStuff = new BleStuff();
        mBleStuff.Connect(this, mBluetoothAdapter, mDeviceAddress, mBleCallbacks);
        publishStatus("Connecting...", 0);

        // build a notification and show it
        //
        {
            Intent noteIntent = new Intent(this, BleStuffService.class);
            PendingIntent pIntent = PendingIntent.getActivity(this, 0, noteIntent, 0);

            Notification n = new Notification.Builder(this)
                    .setContentTitle("Syncing")
                    .setContentText("WIP")
                    .setSmallIcon(R.drawable.icon)
                    .setContentIntent(pIntent)
                    .setAutoCancel(true)
                    .addAction(R.drawable.icon, "First line", pIntent)
                    .addAction(R.drawable.icon, "Second line", pIntent)
                    .addAction(R.drawable.icon, "Third line", pIntent).build();
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(mNotificationId, n);
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(mNotificationId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

