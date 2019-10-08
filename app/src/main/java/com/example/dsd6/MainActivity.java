package com.example.dsd6;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import android.widget.TextView;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@TargetApi(21)
public class MainActivity extends Activity {
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
    private int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private BluetoothGatt mGatt;
    private TextView mTextView;
    private Button mButton;
    private DrawChart mDrawChart;
    private TextView mDateView;
    private EditText editText;
    private java.util.Date mDate;
    BluetoothGattCharacteristic mTX, mRD;
    Queue<String> txQueue = new ArrayDeque<String>();
    boolean isWriting = false;

    DbHandler dbHandler;

    private void AddText(final String str)
    {
        Log.i("AddText", str);
        runOnUiThread(new Runnable() {
            public void run() {
                mTextView.setText(mTextView.getText() + str);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        mTextView = (TextView)findViewById(R.id.textView);
        mTextView.setMovementMethod(new ScrollingMovementMethod());
        mTextView.setText("");

        mDrawChart = (DrawChart) findViewById(R.id.imageView1);
        mDateView = (TextView)findViewById(R.id.dateView);
        editText = (EditText)findViewById(R.id.editText2);

        dbHandler = new DbHandler(MainActivity.this);
        UpdateToLatest();

        mButton = (Button)findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                String text = editText.getText().toString();
                if (text.equals("update")) {
                    for (int i = 0; i < 10; i++)
                        send("AT+DATA=" + Integer.toString(i));
                    UpdateToLatest();
                }
                else {
                    send(text);
                }

            }
        });

        Button prevButton = (Button)findViewById(R.id.prevButton);
        prevButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                mDate = new java.util.Date(mDate.getTime() - 24*3600*1000);
                UpdateGraph( mDate);
            }
        });
        Button nextButton = (Button)findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                mDate = new java.util.Date(mDate.getTime() + 24*3600*1000);
                UpdateGraph( mDate);
            }
        });
    }

    void UpdateToLatest()
    {
        DbHandler.BandActivity lastActivity = dbHandler.GetLastEntry();
        mDate =new java.util.Date(lastActivity.timestamp * 1000);
        UpdateGraph( mDate);
    }

    void UpdateGraph(java.util.Date date)
    {
        long timeMod = date.getTime() % (24*60*60*1000);
        timeMod = date.getTime() - timeMod;

        java.util.Date dateIni = new java.util.Date(timeMod);
        java.util.Date dateFin = new java.util.Date(dateIni.getTime() + 3600*24*1000);

        //String timestampStr1 = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(dateIni);
        //String timestampStr2 = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(dateFin);
        //AddText("Last: " + timestampStr1  + " -  " + timestampStr2 + "\n");

        mDateView.setText(new java.text.SimpleDateFormat("dd/MM/yyyy").format(dateIni));

        ArrayList<DbHandler.BandActivity> data = dbHandler.GetDataRange(dateIni, dateFin);
        mDrawChart.AddPoints(dateIni, dateFin, data);
    }

    protected void send(String str)
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
            if (mTX.setValue(str.getBytes())) {
                AddText(" tx: " + str +"\n");
                mGatt.writeCharacteristic(mTX);
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
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

    @Override
    protected void onPause()
    {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled())
        {
            scanLeDevice(false);
        }
    }

    @Override
    protected void onDestroy()
    {
        if (mGatt == null)
        {
            return;
        }
        mGatt.close();
        mGatt = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == Activity.RESULT_CANCELED)
            {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void scanLeDevice(final boolean enable)
    {
        if (enable) {
            AddText("scanLeDevice " + enable + "\n");
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
            AddText(" " + result.getDevice().getAddress().toString() + "\n");
            BluetoothDevice btDevice = result.getDevice();
            connectToDevice(btDevice);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            AddText("onBatchScanResults:\n");
            for (ScanResult sr : results) {
                AddText(" " + sr.toString()+"\n");
            }
        }

        @Override
        public void onScanFailed(int errorCode)
        {
            AddText("Error Code: " + errorCode + "\n");
        }
    };

    public void connectToDevice(BluetoothDevice device)
    {
        if (mGatt == null)
        {
            mGatt = device.connectGatt(this, false, gattCallback);
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
                    AddText("STATE_CONNECTED\n");
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    AddText("STATE_DISCONNECTED\n");
                    break;
                default:
                    AddText("STATE_OTHER\n");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            super.onServicesDiscovered(gatt, status);
            List<BluetoothGattService> services = gatt.getServices();
            AddText("onServicesDiscovered\n");
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
                    AddText("BluetoothGattService: " + "NORDIC_SERIAL" + "\n");
                    mTX = service.getCharacteristic(CHARACTERISTIC_NORDIC_SERIAL_TX);
                    mRD = service.getCharacteristic(CHARACTERISTIC_NORDIC_SERIAL_RX);

                    //enable read notifications
                    Boolean res1 = gatt.setCharacteristicNotification(mRD, true);
                    AddText("setCharacteristicNotification: " + res1 + "\n");
                    BluetoothGattDescriptor descriptor = mRD.getDescriptor(CHARACTERISTIC_NORDIC_SERIAL_RX_NOT);
                    if (descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                        Boolean res = gatt.writeDescriptor(descriptor); //apply these changes to the ble chip to tell it we are ready for the data
                        AddText("notification: " + res + "\n");
                    }
                }
                else if (service.getUuid().equals(SERVICE_NORDIC_UNKNOWN))
                {
                    //AddText("BluetoothGattService: " + "NORDIC_UNKNOWN" + "\n");
                }
                else
                {
                    AddText("BluetoothGattService: " + service.getUuid().toString() + "\n");

                    List<BluetoothGattCharacteristic> serviceCharacteristics = service.getCharacteristics();
                    for (BluetoothGattCharacteristic characteristic : serviceCharacteristics)
                    {
                        gatt.readCharacteristic(characteristic);

                        AddText("*   " + characteristic.getUuid().toString() + "\n");
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

        int state = 0;
        String command;
        int len = 0;
        byte[] data;
        int dataCnt = 0;

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (characteristic.getUuid().equals(CHARACTERISTIC_DEVICE_NAME))
            {
                AddText("   *   Device Name: "+characteristic.getStringValue(0) +"\n");
            }
            else if (characteristic.getUuid().equals(CHARACTERISTIC_NORDIC_SERIAL_RX))
            {
                //BluetoothGattCharacteristic nextRequest = readQueue.poll();
                AddText("   *   Serial RX: "+characteristic.getStringValue(0) +"\n");
            }
            else
            {
                AddText("   *   "+characteristic.getUuid().toString()+"\n");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicChanged(gatt, characteristic);
            //String data = characteristic.getStringValue(0);
            //AddText(data);
            byte[] s = characteristic.getValue();
            for (int i = 0; i < s.length; i++) {
                switch (state) {
                    case 0:
                        if (s[i] == 'A') state++;
                        else state = 0;
                        break;
                    case 1:
                        if (s[i] == 'T') state++;
                        else state = 0;
                        break;
                    case 2:
                        if (s[i] == '+') {
                            state++;
                            command = "";
                        } else state = 0;
                        break;
                    case 3:
                        if (s[i] == '\n') {
                            if (command.startsWith("DATA")) {
                                String[] ss = command.split(",");
                                len = Integer.valueOf(ss[1]);
                                data = new byte[len];
                                dataCnt = 0;
                                state++;
                                if (len==0)
                                    state=0;
                            }
                            else {
                                state=0;
                            }

                            AddText("* " + command + "\n");
                        } else {
                            if (s[i] != '\r')
                                command += (char)s[i];
                        }
                        break;
                    case 4: {
                        data[dataCnt++] = s[i];
                        if (dataCnt==len)
                        {
                            //AddText(bytesToHex(data));

                            for(int o=0;o<len;o+=6) {

                                int type = (data[o + 0] & 0xff) >>6;

                                long timestamp = 0;
                                timestamp = timestamp * 256 + ((data[o + 0]& 0xff) & 0x3f);
                                timestamp = timestamp * 256 + (data[o + 1]& 0xff);
                                timestamp = timestamp * 256 + (data[o + 2]& 0xff);
                                timestamp = timestamp * 256 + (data[o + 3]& 0xff);
                                timestamp += 1262304000;

                                int value = 0;
                                value = value * 256 + (data[o + 4]& 0xff);
                                value = value * 256 + (data[o + 5]& 0xff);

                                dbHandler.insertData(type, timestamp, value);

                                String timestampStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date(timestamp * 1000));
                                AddText((o/6) + ": " + timestampStr + " "+ value+"\n");
                            }
                            state=0;
                        }
                    }
                }
            }
        }
    };
}