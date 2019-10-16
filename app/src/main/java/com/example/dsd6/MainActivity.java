package com.example.dsd6;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattDescriptor;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import android.widget.TextView;

import java.util.Queue;
import java.util.UUID;

@TargetApi(21)
public class MainActivity extends Activity {

    private TextView mStatusView;
    private DrawChart mDrawChartNight;
    private DrawChart mDrawChartDay;
    private TextView mDateView;
    private ProgressBar mProgressBar;
    private java.util.Date mDate;
    DbHandler dbHandler;
    BleStuff mBleStuff;
    BleStuff.BleCallbacks mBleCallbacks;
    BlockProcessor mBlockProcessor = new BlockProcessor();
    BlockProcessor.ProcessorCallbacks mMyCallbacks;
    CheckBox checkHeartBeats, checkSteps, checkSports;

    private void AddText(final String str)
    {
        Log.i("AddText", str);
    }

    private void setStatus(final String str)
    {
        Log.i("AddText", str);
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

        setContentView(R.layout.activity_main);

        mBlockProcessor = new BlockProcessor();
        mBleStuff = new BleStuff();

        mStatusView = (TextView)findViewById(R.id.statusView);

        mDrawChartNight = (DrawChart) findViewById(R.id.imageView1);
        mDrawChartDay = (DrawChart) findViewById(R.id.imageView2);
        mDateView = (TextView)findViewById(R.id.dateView);

        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        dbHandler = new DbHandler(MainActivity.this);
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

        checkSports = (CheckBox) findViewById(R.id.checkSports);
        checkSports.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
              {
                  @Override
                  public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                      mDrawChartNight.setChannel(2,isChecked);
                      mDrawChartDay.setChannel(2,isChecked);
                      UpdateGraph(mDate);
                  }
              }
        );

        class MyBlockCallbacks implements BlockProcessor.ProcessorCallbacks
        {
            @Override
            public void setProgress(final int i)
            {
                runOnUiThread(new Runnable() {
                    public void run() {
                        setProgressBar(i);
                        UpdateToLatest();
                        if (i==100)
                            setStatus("done");
                    }
                });
            }
            @Override
            public void addText(final String str)
            {
                AddText(str);
            }
            @Override
            public void send(String command)
            {
                mBleStuff.bleSend(command);
            }
        }
        mMyCallbacks = new MyBlockCallbacks();


        class MyBleCallbacks implements BleStuff.BleCallbacks
        {
            @Override
            public void onConnect()
            {
                setStatus("connected");
            }
            @Override
            public void onDisconect()
            {
                setStatus("disconnected");
            }
            @Override
            public void onFound()
            {
                setStatus("device found");
                mBlockProcessor.getBlocks(dbHandler, mMyCallbacks);
            }
            @Override
            public void onData(byte[] data)
            {
                mBlockProcessor.process(data);
            }
        };
        mBleCallbacks = new MyBleCallbacks();

        mBleStuff.onCreate(this,mBleCallbacks);
    }

    void UpdateToLatest() {
        try {
            DbHandler.BandActivity lastActivity = dbHandler.GetLastEntry();
            mDate = new java.util.Date(lastActivity.timestamp * 1000);
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
    protected void onResume()
    {
        super.onResume();
        mBleStuff.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mBleStuff.onPause();
    }

    @Override
    protected void onDestroy()
    {
        mBleStuff.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == mBleStuff.REQUEST_ENABLE_BT)
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
}