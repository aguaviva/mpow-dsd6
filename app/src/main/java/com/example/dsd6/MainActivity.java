package com.example.dsd6;

import android.content.BroadcastReceiver;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import java.util.ArrayList;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

@TargetApi(21)
public class MainActivity extends Activity {

    public static final String EXTRAS_DEVICE_NAME = "device_name";
    public static final String EXTRAS_DEVICE_ADDRESS = "device_address";
    private TextView mStatusView;
    private DrawChart mDrawChartNight;
    private DrawChart mDrawChartDay;
    private TextView mDateView;
    private ProgressBar mProgressBar;
    private java.util.Date mDate;
    DbHandler dbHandler;
    CheckBox checkHeartBeats, checkSteps;
    SharedPreferences sharedPref;
    String mDeviceAddress;

    private void AddText(final String str)
    {
        Log.i("AddText", str);
    }

    private void setStatus(final String str)
    {
        //Log.i("AddText", str);
        runOnUiThread(new Runnable() {
            public void run() {
                mStatusView.setText(str);
            }
        });
    }

    private void setProgressBar(final int i)
    {
        mProgressBar.setProgress(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = getSharedPreferences("myPref", MODE_PRIVATE);

        setContentView(R.layout.activity_main);

        mStatusView = (TextView)findViewById(R.id.statusView);

        mDrawChartNight = (DrawChart) findViewById(R.id.imageView1);
        mDrawChartDay = (DrawChart) findViewById(R.id.imageView2);
        mDateView = (TextView)findViewById(R.id.dateView);

        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        dbHandler = new DbHandler(MainActivity.this);
        dbHandler.deleteRecord();
        UpdateToLatest();

        Button prevButton = (Button)findViewById(R.id.prevButton);
        prevButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                if(mDate!=null) {
                    mDate = new java.util.Date(mDate.getTime() - 24 * 3600 * 1000);
                    UpdateGraph(mDate);
                }
            }
        });

        Button nextButton = (Button)findViewById(R.id.nextButton);
        nextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                if(mDate!=null) {
                    mDate = new java.util.Date(mDate.getTime() + 24 * 3600 * 1000);
                    UpdateGraph(mDate);
                }
            }
        });

        checkHeartBeats = (CheckBox) findViewById(R.id.checkHeartBeats);
        checkHeartBeats.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
           {
               @Override
               public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                   mDrawChartNight.setChannel(0,isChecked);
                   mDrawChartDay.setChannel(0,isChecked);
                   UpdateGraph(mDate);
               }
           }
        );

        checkSteps = (CheckBox) findViewById(R.id.checkSteps);
        checkSteps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
           {
               @Override
               public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                   mDrawChartNight.setChannel(1,isChecked);
                   mDrawChartDay.setChannel(1,isChecked);
                   UpdateGraph(mDate);
               }
           }
        );

        syncDevice();
    }

    void UpdateToLatest() {
        try {
            //DbHandler.BandActivity lastActivity = dbHandler.GetLastEntry();
            //mDate = new java.util.Date(lastActivity.timestamp * 1000);
            mDate = new java.util.Date();
            UpdateGraph(mDate);
        } catch (Exception e) {
            AddText(" err: " + e.getMessage() +"\n");
        }
    }
    void UpdateGraph(java.util.Date date)
    {
        try {
            long timeMod = date.getTime() % (24 * 60 * 60 * 1000);
            timeMod = date.getTime() - timeMod;

            java.util.Date dateIni = new java.util.Date(timeMod);
            java.util.Date dateMid = new java.util.Date(dateIni.getTime() + 3600 * 12 * 1000);
            java.util.Date dateFin = new java.util.Date(dateIni.getTime() + 3600 * 24 * 1000);

            mDateView.setText(new java.text.SimpleDateFormat("EEE, d MMM yyyy").format(dateIni));

            ArrayList<DbHandler.BandActivity> data = dbHandler.GetDataRange(dateIni, dateFin);
            mDrawChartNight.AddPoints(0,12, dateIni, dateMid, data);
            mDrawChartDay.AddPoints(12, 24, dateMid, dateFin, data);
        } catch (Exception e) {
            AddText(" err: " + e.getMessage() + "\n");
        }
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                String string = bundle.getString(BleStuffService.STATUS);
                int resultCode = bundle.getInt(BleStuffService.RESULT);
                setStatus(string);
                setProgressBar(resultCode);
                UpdateToLatest();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(BleStuffService.NOTIFICATION));

        mDeviceAddress = sharedPref.getString("mDeviceAddress", null);
        if (mDeviceAddress==null) {
            Intent intent = new Intent(this, DeviceScanActivity.class);
            startActivityForResult(intent, 33);
        } else {
            syncDevice();
        }

    }
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 33)
        {
            String mDeviceName = data.getStringExtra(EXTRAS_DEVICE_NAME);
            String mDeviceAddress = data.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            sharedPref.edit().putString("mDeviceAddress", mDeviceAddress).commit();
            syncDevice();
        }
        /*
        else if (requestCode == mBleStuff.REQUEST_ENABLE_BT)
        {
            if (resultCode == Activity.RESULT_CANCELED)
            {
                //Bluetooth not enabled.
                finish();
                return;
            }
        }
        */
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void syncDevice()
    {
        mDeviceAddress = sharedPref.getString("mDeviceAddress", null);
        Intent i= new Intent(this, BleStuffService.class);
        i.putExtra("HOST", mDeviceAddress);
        startService(i);
    }
}