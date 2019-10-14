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

    final static public UUID SERVICE_GENERIC_ACCESS                  = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    final static public UUID CHARACTERISTIC_DEVICE_NAME              = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    final static public UUID CHARACTERISTIC_APPEARANCE               = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");
    final static public UUID CHARACTERISTIC_PREFERRED_PARAMS         = UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb");

    final static public UUID SERVICE_GENERIC_ATTRIBUTE               = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");

    final static public UUID SERVICE_NORDIC_SERIAL                   = UUID.fromString("0000190b-0000-1000-8000-00805f9b34fb");
    final static public UUID CHARACTERISTIC_NORDIC_SERIAL_TX         = UUID.fromString("00000003-0000-1000-8000-00805f9b34fb");
    final static public UUID CHARACTERISTIC_NORDIC_SERIAL_RX         = UUID.fromString("00000004-0000-1000-8000-00805f9b34fb");
    final static public UUID CHARACTERISTIC_NORDIC_SERIAL_RX_NOT     = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    final static public UUID SERVICE_NORDIC_UNKNOWN                  = UUID.fromString("0000190a-0000-1000-8000-00805f9b34fb");


    final static public UUID HEART_RATE_MEASUREMENT   = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");
    final static public UUID CSC_MEASUREMENT          = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");
    final static public UUID MANUFACTURER_STRING      = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb");
    final static public UUID MODEL_NUMBER_STRING      = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb");
    final static public UUID FIRMWARE_REVISION_STRING = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    final static public UUID APPEARANCE               = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");
    final static public UUID BODY_SENSOR_LOCATION     = UUID.fromString("00002a38-0000-1000-8000-00805f9b34fb");
    final static public UUID BATTERY_LEVEL            = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    final static public UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter mBluetoothAdapter;
    public int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;

    BluetoothGattCharacteristic mTX, mRD;
    Queue<String> txQueue = new ArrayDeque<String>();
    boolean isWriting = false;
    DbHandler dbHandler;

    Activity mActivity;
    BleCallbacks mBleCallBacks;

    public interface BleCallbacks
    {
        void onConnect();
        void onDisconect();
        void onFound();
        void onData(byte[] data);
    }

    public void onCreate(final Activity activity, BleCallbacks bleCallBacks) {
        mBleCallBacks = bleCallBacks;
        mActivity = activity;
        mHandler = new Handler();
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(activity, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            activity.finish();
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(activity.getBaseContext().BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

    }


    public  void onResume()
    {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            if (Build.VERSION.SDK_INT >= 21)
            {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                filters = new ArrayList<ScanFilter>();
                ScanFilter filter = new ScanFilter.Builder().setDeviceAddress("D3:71:90:1C:E9:C8").build();
                filters.add(filter);
            }
            scanLeDevice(true);
        }
    }

    public void onPause()
    {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
        {
            scanLeDevice(false);
        }
    }

    public void onDestroy()
    {
        if (mGatt == null)
        {
            return;
        }
        mGatt.close();
        mGatt = null;
    }


    public void bleSend(String str)
    {
        synchronized(txQueue)
        {
            txQueue.add(str);
            writeNextValueFromQueue();
        }
    }

    protected void writeNextValueFromQueue()
    {
        if ((isWriting == false) && (txQueue.size()>0))
        {
            isWriting = true;
            String str = txQueue.poll();
            if (mTX!=null) {
                if (mTX.setValue(str.getBytes())) {
                    if (mGatt.writeCharacteristic(mTX) == false) {
                        //AddText(" err: " + str + "\n");
                        isWriting = false;
                    }
                }
            }
            else
            {
                //AddText("err: not connected.\n");
            }
        }
    }

    private void scanLeDevice(final boolean enable)
    {
        if (enable) {
            //AddText("scanLeDevice " + enable + "\n");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLEScanner.stopScan(mScanCallback);
                }
            }, SCAN_PERIOD);

            mLEScanner.startScan(filters, settings, mScanCallback);
        }
        else
        {
            mLEScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            //AddText(" " + result.getDevice().getAddress().toString() + "\n");
            BluetoothDevice btDevice = result.getDevice();
            connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            //AddText("onBatchScanResults:\n");
            //for (ScanResult sr : results) {
            //    AddText(" " + sr.toString()+"\n");
            //}
        }

        @Override
        public void onScanFailed(int errorCode)
        {
            //AddText("Error Code: " + errorCode + "\n");
        }
    };

    public void connectToDevice(BluetoothDevice device)
    {
        if (mGatt == null)
        {
            mGatt = device.connectGatt(mActivity.getApplicationContext(), false, gattCallback);
            scanLeDevice(false);// will stop after first device detection
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
                    //AddText("STATE_CONNECTED\n");
                    mBleCallBacks.onConnect();
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    mBleCallBacks.onDisconect();
                    //AddText("STATE_DISCONNECTED\n");
                    break;
                default:
                    //AddText("STATE_OTHER\n");
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
                if (service.getUuid().equals(SERVICE_GENERIC_ACCESS))
                {
                    //AddText("BluetoothGattService: " + "GENERIC_ACCESS" + "\n");
                    //BluetoothGattCharacteristic name = service.getCharacteristic(CHARACTERISTIC_DEVICE_NAME);
                    //gatt.readCharacteristic(name);
                }
                else if (service.getUuid().equals(SERVICE_GENERIC_ATTRIBUTE))
                {
                    //AddText("BluetoothGattService: " + "NORDIC_UNKNOWN" + "\n");
                }
                else if (service.getUuid().equals(SERVICE_NORDIC_SERIAL))
                {
                    //AddText("BluetoothGattService: " + "NORDIC_SERIAL" + "\n");
                    mTX = service.getCharacteristic(CHARACTERISTIC_NORDIC_SERIAL_TX);
                    mRD = service.getCharacteristic(CHARACTERISTIC_NORDIC_SERIAL_RX);

                    //enable read notifications
                    Boolean res1 = gatt.setCharacteristicNotification(mRD, true);
                    if (res1) {
                        BluetoothGattDescriptor descriptor = mRD.getDescriptor(CHARACTERISTIC_NORDIC_SERIAL_RX_NOT);
                        if (descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                            Boolean res = gatt.writeDescriptor(descriptor); //apply these changes to the ble chip to tell it we are ready for the data
                            //AddText("notification: " + res + "\n");
                            mBleCallBacks.onFound();
                        }
                    }
                }
                else if (service.getUuid().equals(SERVICE_NORDIC_UNKNOWN))
                {
                    //AddText("BluetoothGattService: " + "NORDIC_UNKNOWN" + "\n");
                }
                else
                {
                    //AddText("BluetoothGattService: " + service.getUuid().toString() + "\n");

                    List<BluetoothGattCharacteristic> serviceCharacteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : serviceCharacteristics)
                    {
                        gatt.readCharacteristic(characteristic);

                        //AddText("*   " + characteristic.getUuid().toString() + "\n");
                    }
                }
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            synchronized(txQueue)
            {
                isWriting = false;
                writeNextValueFromQueue();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (characteristic.getUuid().equals(CHARACTERISTIC_DEVICE_NAME))
            {
                //AddText("   *   Device Name: "+characteristic.getStringValue(0) +"\n");
            }
            else if (characteristic.getUuid().equals(CHARACTERISTIC_NORDIC_SERIAL_RX))
            {
                //BluetoothGattCharacteristic nextRequest = readQueue.poll();
                //AddText("   *   Serial RX: "+characteristic.getStringValue(0) +"\n");
            }
            else
            {
                //AddText("   *   "+characteristic.getUuid().toString()+"\n");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicChanged(gatt, characteristic);
            //String data = characteristic.getStringValue(0);
            //AddText(data);
            byte[] s = characteristic.getValue();
            mBleCallBacks.onData(s);
         }
    };


}

