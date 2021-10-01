package com.example.main;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.stepper.R;

@TargetApi(21)
public class MainActivity extends Activity {

    public static final String EXTRAS_DEVICE_NAME = "device_name";
    public static final String EXTRAS_DEVICE_ADDRESS = "device_address";
    private TextView mStatusView;
    private DrawChart mDrawChartNight;
    private DrawChart mDrawChartDay;
    private TextView mDateView;
    private java.util.Date mDate;
    DbHandler dbHandler;
    SharedPreferences sharedPref;

    private void AddText(final String str)
    {
        Log.i("AddText", str);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = getSharedPreferences("myPref", MODE_PRIVATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("custom-event-name"));

        setContentView(R.layout.activity_main);

        mDrawChartNight = findViewById(R.id.imageView1);
        mDrawChartDay = findViewById(R.id.imageView2);
        mDateView = findViewById(R.id.dateView);

        dbHandler = new DbHandler(MainActivity.this);
        UpdateToLatest();

        Button prevButton = findViewById(R.id.prevButton);
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

        Button nextButton = findViewById(R.id.nextButton);
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


        Intent number5 = new Intent(getBaseContext(), MyService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(number5);
        } else {
            //lower then Oreo, just start the service.
            startService(number5);
        }
    }



    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    void UpdateToLatest() {
        try {
            mDate = new java.util.Date();
            UpdateGraph(mDate);
        } catch (Exception e) {
            AddText(" err: " + e.getMessage() +"\n");
        }
    }
    @SuppressLint("SimpleDateFormat")
    void UpdateGraph(java.util.Date date)
    {
        try {
            Calendar cal = new GregorianCalendar();
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            date = cal.getTime();

            long timeMod = date.getTime();

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
    protected void onResume() {
        super.onResume();
        UpdateGraph(mDate);
    }

    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            //String message = intent.getStringExtra("message");
            //Log.d("receiver", "Got message: " + message);
            UpdateGraph(mDate);
        }
    };
}
