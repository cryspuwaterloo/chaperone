package ca.uwaterloo.crysp.chaperone;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


import ca.uwaterloo.crysp.chaperone.tracker.StatusClassifier;
import ca.uwaterloo.crysp.chaperone.tracker.UserTracker;
import umich.cse.yctung.libacousticsensing.AcousticSensingController;
import umich.cse.yctung.libacousticsensing.AcousticSensingControllerListener;
import umich.cse.yctung.libacousticsensing.Setting.AcousticSensingSetting;

import static ca.uwaterloo.crysp.chaperone.tracker.StatusClassifier.ABSENCE_SIGNAL;
import static ca.uwaterloo.crysp.chaperone.tracker.StatusClassifier.BOTH_SIGNAL;
import static ca.uwaterloo.crysp.chaperone.tracker.StatusClassifier.DEPARTURE_SIGNAL;


public class MainActivity extends AppCompatActivity implements AcousticSensingControllerListener {
    final String TAG = "MainActivity";


    private List<float[]> exampleRawData;
    JNICallback jc;
    AcousticSensingController asc;
    AcousticSensingSetting aset;
    UserTracker ut;
    StatusClassifier sc;

    int leavingCount = 0;
    int leftCount = 0;
    boolean prevLeaving = false;
    boolean prevLeft = false;

    Button buttonAuto, buttonStart, buttonReset, buttonSave, buttonProfile;
    TextView textViewDebugInfo, textResult, textAlert;
    Switch alarmSwitch, vibrationSwitch, concurrentSwitch;

    List<Float> distances;
    long t0;

    private LineChart chart;


    // acceleration trigger
    private static final int MAX_G_COUNTER = 5;
    private static final float GRAVITY = 9.81f;
    private static final float TOLERANCE = 0.5f;
    private float prev_g;
    private int counter_g;
    private boolean triggerEnabled;

    private SensorManager sensorManager;
    private Sensor accSensor;

    private boolean isAlert = false;

    private int prevVolume;
    private double targetVolume = -1;
    private String profileName = "Pixel";
    private float[][] remapMap = null;

    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEditor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        aset = new AcousticSensingSetting(this);
        ut = new UserTracker();
        sc = new StatusClassifier();
        distances = new ArrayList<>();
        aset.setMode(1); //set to standalone mode
        buttonAuto = (Button)findViewById(R.id.button);
        buttonStart = (Button)findViewById(R.id.button2);
        buttonReset = (Button)findViewById(R.id.button3);
        buttonSave = (Button)findViewById(R.id.button4);
        textViewDebugInfo = (TextView)findViewById(R.id.textView2);
        textResult = (TextView)findViewById(R.id.textView3);
        textAlert = (TextView)findViewById(R.id.textView4);
        alarmSwitch = (Switch)findViewById(R.id.switch1);
        vibrationSwitch = (Switch)findViewById(R.id.switch2);
        concurrentSwitch = (Switch) findViewById(R.id.switch3);
        buttonProfile = (Button)findViewById(R.id.buttonProfile);
        t0 = System.currentTimeMillis();

        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        prevVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        preferences = getPreferences(Context.MODE_PRIVATE);
        profileName = preferences.getString("Profile", "Pixel");
        loadProfile(profileName);

        // initialize display
        initChart();


        buttonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAll();
                textViewDebugInfo.setText("All reset");
            }
        });
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (asc.isSensing()) {
                    asc.stopSensingNow();
                }
                updateUI();

                try {
                    String date = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss",
                            Locale.getDefault()).format(new Date());
                    String filename = String.format("chp_%s.txt", date);
                    File file = new File(Environment.getExternalStorageDirectory().getPath(),
                            filename);
                    FileWriter fileWriter = new FileWriter(file);
                    List<float[]> rawData = ut.getAllRawMagnitude();
                    for(float[] row: rawData) {
                        for(int i = 0; i < row.length;  ++i){
                            fileWriter.append(Float.toString(row[i]));
                            if(i == row.length - 1) {
                                fileWriter.append('\n');
                            } else {
                                fileWriter.append(' ');
                            }
                        }
                        fileWriter.flush();
                    }
                    fileWriter.close();
                    textViewDebugInfo.setText(String.format("Save to %s!", filename));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        buttonAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(alarmSwitch.isChecked()) {
                    Context current = getApplicationContext();
                    Intent stopIntent = new Intent(getApplicationContext(), RingtoneService.class);
                    current.stopService(stopIntent);
                }
                if(triggerEnabled) {
                    forceStop();
                    triggerEnabled = false;
                } else {
                    EnableSensor();
                }
                updateUI();
            }
        });

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                onStartOrStopClicked();
            }
        });
        buttonProfile.setText("Profile: " + profileName) ;
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Select a profile");
        dialog.setItems(R.array.profileArray, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int position) {
                switch(position) {
                    case 0:
                        // Pixel profile
                        loadProfile("Pixel");
                        preferences.edit().putString("Profile", "Pixel").apply();
                        break;
                    case 1:
                        // Pixel3 profile
                        loadProfile("Pixel 3");
                        preferences.edit().putString("Profile", "Pixel 3").apply();
                        break;
                    case 2:
                        loadProfile("S8");
                        preferences.edit().putString("Profile", "S8").apply();
                        break;
                }
                buttonProfile.setText("Profile: " + profileName);
            }
        });



        buttonProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog alert = dialog.create();
                alert.show();

            }
        });

        jc = new JNICallback();
        jc.debugTest();
        asc = new AcousticSensingController(this, this);
        // onInitOrFinalizeClicked();
        triggerEnabled = false;
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // EnableSensor();
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        123);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        123);
            }
        }
    }


    void onStartOrStopClicked() {

        if (asc == null) { // not ready to sense yet
            // Log.e(TAG, "Not ready to sense yet");
            return;
        }

        if (!asc.isReadyToSense()) {
            initIfPossible();
        }

        if (!asc.isSensing()) {

            asc = new AcousticSensingController(this, this);
            onInitOrFinalizeClicked();
            asc.startSensingWhenPossible();

        } else { // need to stop sensing
            asc.stopSensingNow();
            recoverVolume();
        }
        updateUI();
    }

    void onInitOrFinalizeClicked() {

        if (asc == null || !asc.isReadyToSense()) {
            setSensingVolume();
            initIfPossible();
        } else {
            finalzeIfPossible();
        }
        updateUI();
    }

    void initIfPossible() {
        if (asc == null || asc.isReadyToSense()) {
            Log.e(TAG, "Unable to init when the sensing controller has be initialized");
            return;
        }

        boolean initResult = asc.init(aset);

        if (!initResult) {
            textViewDebugInfo.setText("Init fails");
            return;
        }

    }

    void finalzeIfPossible() {
        // TODO: finialize the sensing controller
    }


    void clearAll() {
        ut = new UserTracker();
        sc = new StatusClassifier();
        distances = new ArrayList<>();
        leftCount = 0;
        leavingCount = 0;
        // initialize display

        chart.invalidate();
        chart.clear();
        chart.getDescription().setEnabled(true);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setPinchZoom(true);
        chart.setBackgroundColor(Color.DKGRAY);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        chart.setData(data);

        Legend l = chart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = chart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(200f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);
    }


    //=================================================================================================
    //  Chart related functions
    //=================================================================================================

    private void addEntry(float distance) {

        LineData data = chart.getData();

        if (data != null) {

            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            data.addEntry(new Entry(set.getEntryCount(), distance), 0);
            data.notifyDataChanged();

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            // limit the number of visible entries
            chart.setVisibleXRangeMaximum(120);
            // chart.setVisibleYRange(30, AxisDependency.LEFT);

            // move to the latest entry
            chart.moveViewToX(data.getEntryCount());

            // this automatically refreshes the chart (calls invalidate())
            // chart.moveViewTo(data.getXValCount()-7, 55f,
            // AxisDependency.LEFT);
        }
    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "Distance");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(ColorTemplate.getHoloBlue());
        set.setCircleColor(Color.WHITE);
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setFillAlpha(65);
        set.setFillColor(ColorTemplate.getHoloBlue());
        set.setHighLightColor(Color.rgb(244, 117, 117));
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(9f);
        set.setDrawValues(false);
        return set;
    }


    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            forceStop();
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }


    public void forceStop() {
        asc.stopSensingNow();
        sensorManager.unregisterListener(accSensorListener);
        counter_g = 0;
        prev_g = 0;
    }


    //=================================================================================================
    //  Acoustic sensing callbacks
    //=================================================================================================
    @Override
    public void updateDebugStatus(boolean status, final String stringToShow) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewDebugInfo.setText(stringToShow);
            }
        });
    }

    @Override
    public void showToast(String stringToShow) {

    }

    public void updateUI() {
        if (!asc.isSensing()) {
            buttonStart.setText("Start");
            concurrentSwitch.setEnabled(true);

        } else {
            buttonStart.setText("Stop");
            if(triggerEnabled) {
                buttonStart.setEnabled(false);
                concurrentSwitch.setEnabled(false);
            }

        }

        if (triggerEnabled) {
            buttonAuto.setText("Manual");
        } else {
            buttonAuto.setText("Auto");
            buttonStart.setEnabled(true);
        }
    }

    @Override
    public void isConnected(boolean success, String resp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void sensingEnd() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        });
    }

    @Override
    public void sensingStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        });
    }

    @Override
    public void updateSensingProgress(int percent) {

    }

    @Override
    public void serverClosed() {
        // TODO: remote server is closed by some reason -> need to stop and clean everything

    }


    @Override
    public void updateResult(int valInt, float valDouble) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textResult.setText("Result: ("+valInt+", "+valDouble+")");
            }
        });
    }

    @Override
    public void dataJNICallback(long retAddr) {
        // AcousticSensingResult result = jc.dataCallback(retAddr);
        // result.dump();
        long t1 = System.currentTimeMillis();
        Log.d(TAG, String.format("Callback last called: %d ms", t1 - t0 ));
        t0 = t1;
        float[] results = jc.dataCallback(retAddr);

        if(remapMap != null) {
            results = remapResult(results, remapMap);
        }

        float curLoc = ut.add(results);
        t1 = System.currentTimeMillis();
        Log.d(TAG, String.format("Processing Time: %d ms", t1 - t0 ));

        distances.add(curLoc);
        if (concurrentSwitch.isChecked())
            addEntry(curLoc);
        if(ut.getMaxIndex() > 5 && (ut.getMaxIndex() + 1) % 5 == 0) {
            int decision;
            t0 = System.currentTimeMillis();
            decision = sc.addFeatures(ut.extractFeatures());
            t1 = System.currentTimeMillis();
            Log.d(TAG, String.format("Classification Time: %d ms", t1 - t0));
            if(decision == 0) {
                Log.i(TAG, "Nothing happen");
                prevLeaving = false;
                prevLeft = false;
            }
            else if(decision == 1) {
                Log.i(TAG, "Leaving");
                if(!prevLeaving) {
                    leavingCount++;
                    prevLeaving = true;
                }
                if(vibrationSwitch.isChecked()) {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 500 milliseconds
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Build.VERSION.SDK_INT >= 26) {
                                v.vibrate(VibrationEffect.createOneShot(5000, 255));
                            }
                            else {
                                v.vibrate(8000);
                            }
                            // onStartOrStopClicked();
                        }
                    });
                }
                if(alarmSwitch.isChecked()) {
                    Context current = getApplicationContext();
                    Intent i = new Intent(current, RingtoneService.class);
                    current.startService(i);
                    onStartOrStopClicked();
                }
            }

            else if(decision == ABSENCE_SIGNAL) {
                // Log.i(TAG, "Gone!");
                if (!prevLeft) {
                    leftCount++;
                    prevLeft = true;
                }

            }
            else if(decision == BOTH_SIGNAL) {
                // Log.i(TAG, "Leaving & Absent");
                if(!prevLeaving) {
                    leavingCount++;
                    prevLeaving = true;
                }
                if (!prevLeft) {
                    leftCount++;
                    prevLeft = true;
                }
                if(vibrationSwitch.isChecked()) {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    // Vibrate for 1000 milliseconds
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (Build.VERSION.SDK_INT >= 26) {
                                v.vibrate(VibrationEffect.createOneShot(5000, 255));
                            }
                            else {
                                v.vibrate(8000);
                            }
                            // onStartOrStopClicked();
                        }
                    });
                }
                if(alarmSwitch.isChecked()) {
                    Context current = getApplicationContext();
                    Intent i = new Intent(current, RingtoneService.class);
                    current.startService(i);

                    onStartOrStopClicked();
                }

            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(distances.size() > 0) {
                    textResult.setText(String.format("distance: %.2f m",
                            distances.get(distances.size() - 1) * ut.indexToDistance(1)));

                }
                textAlert.setText(String.format("Detected: %d", leavingCount));

                if(distances.get(distances.size() - 1) * ut.indexToDistance(1) > 1.8 && !isAlert) {
                    onStartOrStopClicked();
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_NEUTRAL:
                                    isAlert = false;
                                    if(alarmSwitch.isChecked()) {
                                        Context current = getApplicationContext();
                                        Intent stopIntent = new Intent(getApplicationContext(), RingtoneService.class);
                                        current.stopService(stopIntent);
                                    }
                                    clearAll();
                                    onStartOrStopClicked();
                                    break;

                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("You are too far away").setNeutralButton("Yes", dialogClickListener).show();
                    isAlert = true;

                }
            }
        });
    }


    // Motion sensor trigger
    public SensorEventListener accSensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];
            float g = (float) Math.sqrt(x * x + y * y + z * z);
            Log.i("Acc", "Running:" + x + "," + y +"," + z);
            if(Math.abs(g - prev_g) <= TOLERANCE &&
                    (Math.abs(z - GRAVITY) <= TOLERANCE ||
                            Math.abs(z + GRAVITY) <= TOLERANCE))  {
                counter_g += 1;
                if(counter_g > MAX_G_COUNTER) {
                    // trigger detection
                    textViewDebugInfo.setText("Placed on a surface: " + g + "," + z);
                    if(!asc.isSensing()) {
                        onStartOrStopClicked();
                        updateUI();
                    }
                }
            } else {
                counter_g = 0;
                if(asc.isSensing()) {
                    textViewDebugInfo.setText("Movement detected, stop sensing");
                    onStartOrStopClicked();
                    updateUI();
                }
            }
            prev_g = g;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }
    };

    private void EnableSensor() {
        triggerEnabled = true;
        prev_g = 0.0f;
        counter_g = 0;
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(accSensorListener, accSensor, 100000);
    }

    private void setSensingVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if(targetVolume == -1) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
        }  else {

            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                    (int)(targetVolume * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0);
        }
        Log.d(TAG, "Set to the required volume");
    }

    private void recoverVolume() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                prevVolume, 0);
        Log.d(TAG, "Set to the original volume");
    }

    private void initChart(){
        chart = findViewById(R.id.chart);
        chart.getDescription().setEnabled(true);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setDrawGridBackground(false);
        chart.setPinchZoom(true);
        chart.setBackgroundColor(Color.DKGRAY);
        LineData data = new LineData();
        data.setValueTextColor(Color.WHITE);

        chart.setData(data);

        Legend l = chart.getLegend();

        // modify the legend ...
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.WHITE);

        XAxis xl = chart.getXAxis();
        xl.setTextColor(Color.WHITE);
        xl.setDrawGridLines(false);
        xl.setAvoidFirstLastClipping(true);
        xl.setEnabled(true);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMaximum(200f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

    }

    private float[] remapResult(float[] result, float[][] remap) {
        int index = 0;
        for(int i = 0; i < result.length; ++i) {
            if(remap[index][0] == -1) result[i] = result[i] / remap[index][1];
            else if(i < (int)remap[0][0]) {
                result[i] = result[i] / remap[index][1];
            } else {
                result[i] = result[i] / remap[index][1];
                index += 1;
            }
            if(index >= remap.length) break;
        }
        return result;
    }

    private void loadProfile(String profile) {
        switch (profile) {
            case "Pixel":
                profileName = "Pixel";
                targetVolume = -1;
                remapMap = null;
                break;
            case "Pixel 3":
                profileName = "Pixel 3";
                targetVolume = 0.4;
                remapMap = new float[][]{
                        {5f, 4f},
                        {80f, 3f},
                        {-1f, 4.5f}
                };
                break;
            case "S8":
                profileName = "S8";
                targetVolume = 0.5;
                remapMap = new float[][]{
                        {5f, 10f},
                        {20f, 4f},
                        {75f, 2f}
                };
                break;
        }
    }
}
