package com.example.dsd6;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

public class BleStuff {

    final static public UUID SERVICE_NORDIC_SERIAL                   = UUID.fromString("0000190b-0000-1000-8000-00805f9b34fb");
    final static public UUID CHARACTERISTIC_NORDIC_SERIAL_TX         = UUID.fromString("00000003-0000-1000-8000-00805f9b34fb");
    final static public UUID CHARACTERISTIC_NORDIC_SERIAL_RX         = UUID.fromString("00000004-0000-1000-8000-00805f9b34fb");
    final static public UUID CHARACTERISTIC_NORDIC_SERIAL_RX_NOT     = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    final static public UUID SERVICE_NORDIC_UNKNOWN                  = UUID.fromString("0000190a-0000-1000-8000-00805f9b34fb");


    private BluetoothAdapter mBluetoothAdapter = null;
    public int REQUEST_ENABLE_BT = 1;
    private BluetoothGatt mGatt;

    BluetoothGattCharacteristic mTX, mRD;
    Queue<String> txQueue = new ArrayDeque<String>();
    BleCallbacks mBleCallBacks;

    public interface BleCallbacks
    {
        void onConnect();
        void onDisconect();
        void onFound();
        void onData(byte[] data);
        void onQueueEmpty();
    }

    final Thread t = new Thread() {
        @Override
        public void run() {

            byte[] data;

            while(mGatt!=null) {

                // wait for data to be queued
                //
                synchronized (txQueue) {
                    while (txQueue.size() == 0 || mTX==null) {
                        try {
                            txQueue.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    data = txQueue.peek().getBytes();
                }

                // send data, retrying after 100ms until it goes through
                //
                if (mTX!=null) {
                    if (mTX.setValue(data)) {
                        while(mGatt!=null) {
                            if( mGatt.writeCharacteristic(mTX) == true)
                            {
                                boolean bEmpty = false;
                                synchronized (txQueue) {
                                    String str = txQueue.poll();
                                    bEmpty = (txQueue.size()==0);
                                }
                                if (bEmpty) {
                                    mBleCallBacks.onQueueEmpty();
                                }
                                break;
                            }
                            else {
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
            Log.i("BleStuff", "Thread done!");
        }
    };

    public void Connect(Context context, BluetoothAdapter bluetoothAdapter, String address, BleCallbacks bleCallBacks)
    {
        if (mGatt==null) {
            mBleCallBacks = bleCallBacks;
            mBluetoothAdapter = bluetoothAdapter;
            if (mBluetoothAdapter != null) {
                final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                mGatt = device.connectGatt(context, false, gattCallback);
                t.start();
            }
        }
    }

    public void Disconnect()
    {
        if (mGatt == null)
        {
            return;
        }
        mGatt.close();
        mGatt = null;
    }

    public void Send(String str)
    {
        synchronized(txQueue)
        {
            txQueue.add(str);
            txQueue.notify();
        }
    }

    public int LeftToBeSend()
    {
        synchronized(txQueue)
        {
            return txQueue.size();
        }
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            super.onConnectionStateChange(gatt, status, newState);
            switch (newState)
            {
                case BluetoothProfile.STATE_CONNECTED:
                    mBleCallBacks.onConnect();
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Disconnect();
                    mBleCallBacks.onDisconect();
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            super.onServicesDiscovered(gatt, status);
            List<BluetoothGattService> services = gatt.getServices();
            //AddText("onServicesDiscovered\n");
            for (BluetoothGattService service : services)
            {
                if (service.getUuid().equals(SERVICE_NORDIC_SERIAL))
                {
                    mTX = service.getCharacteristic(CHARACTERISTIC_NORDIC_SERIAL_TX);
                    mRD = service.getCharacteristic(CHARACTERISTIC_NORDIC_SERIAL_RX);

                    //enable read notifications
                    Boolean res1 = gatt.setCharacteristicNotification(mRD, true);
                    if (res1) {
                        BluetoothGattDescriptor descriptor = mRD.getDescriptor(CHARACTERISTIC_NORDIC_SERIAL_RX_NOT);
                        if (descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                            Boolean res = gatt.writeDescriptor(descriptor); //apply these changes to the ble chip to tell it we are ready for the data
                            mBleCallBacks.onFound();
                        }
                    }
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] s = characteristic.getValue();
            mBleCallBacks.onData(s);
        }
    };
}

