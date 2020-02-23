package com.fyp.ble.navigationinitialize;

import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.fyp.ble.navigationinitialize.BLE.BLTE_Device;
import com.fyp.ble.navigationinitialize.BLE.Scanner_BLTE;
import com.opencsv.CSVWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class NavigationActivity extends AppCompatActivity implements SensorEventListener{

    String MAC_LIST;
    JSONArray array;
    JSONArray finalArray;

    ToneGenerator toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);
    private volatile boolean toScan;
    private int requiredDegree = 0;
    int sensorCount = 0;
    private SensorManager mSensorManager;
    TextToSpeech tts;
    TextToSpeech tts2;

    Button buttonproxy;

    TextView t;

    //KalmanFilter
    public KalmanFilter kalmanFilter;
    public KalmanFilter kalmanFilterForMedian;

    //ML counter
    public List<String[]> finalValuelist;


    //StepCounter
    List<String[]> list1;
    List<String[]> list2;
    private double xValue;
    private double yValue;
    private double zValue;
    public double accTh = 10.40;
    //private final SensorManager sensorManager;
    //private Sensor accelerometer;
    public String flag = null;
    public long preIndex = 0;
    public double maxPeak = 0;
    public Dictionary<Long> overTH;
    public boolean flag1 = false;
    public int timeTH = 700;
    public int step = 0;
    //public TextView mStepCounterTextView ;

    private FileWriter mFileWriter;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    Queue<Integer> queue = new LinkedList<>();
    Queue<Integer> predictionQueue = new LinkedList<>();

    //BLE
    StandardDeviation standardDeviation;
    double stdValue;
    double medianValue;

    //BLE
    private HashMap<String, BLTE_Device> mBTDevicesHashMap;
    private ArrayList<BLTE_Device> mBTDevicesArrayList;
//    Queue<Integer> queue = new LinkedList<>();


    //    ListAdapter_BTLE_Devices adapter;
    private Scanner_BLTE mBTLeScanner;
    public String[] macList2;
    public String[] descriptionList;
    public Map<String, String> destinations;
    public boolean activateButton = false;
    public String locatedInitialMAC;
    boolean isCorrectDir = false;
    boolean isArrived = false;
    public static String nextBeaconMAC = "";
    TextView needed;
    TextView current;
    TextView now;

    JSONArray breakPoints;
    String[][] breakPointList;
    String[] nextBreakpoint;


    private float[] meanNormalize = new float[2];
    private float[] stdNormalize = new float[2];
    Interpreter tflite;

    private boolean isTopofStair = false;
    private int numberOfStairs = 0;
    private boolean toStepCount = false;
    private boolean isinProximity = false;

    public boolean stdevflag = false;
    public String juncMsg;

    int windowLength = 20;

    int bpCount = 0;
    private boolean tellIntro = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        Intent intent = getIntent();
        MAC_LIST = intent.getStringExtra("MAC_LIST");

        Log.d("rush",MAC_LIST);


        finalValuelist = new ArrayList<>();
        String[] a = new String[4];
        a[0] = "raw rssi";
        a[1] = "median";
        a[2] = "std";
        a[3] = "prediction";
        finalValuelist.add(a);

        t = (TextView)findViewById(R.id.textView);

        standardDeviation = new StandardDeviation();

        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();

        buttonproxy = (Button)findViewById(R.id.buttonProxy);

        buttonproxy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isinProximity = true;
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBTLeScanner = new Scanner_BLTE(this, 180000, -100);
        }

        toScan = false;
        kalmanFilter = new KalmanFilter(0.008,1);
        kalmanFilterForMedian = new KalmanFilter(0.008,1);
        needed = (TextView)findViewById(R.id.textViewNeeded);
        current = (TextView)findViewById(R.id.textViewCurrent);
        now = (TextView)findViewById(R.id.textViewNow);

        //Permission check
        if (!checkPermission()) {
            openActivity();
        } else {
            if (checkPermission()) {
                requestPermissionAndContinue();
            } else {
                openActivity();
            }
        }

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(this, "The permission to get BLE location data is required", Toast.LENGTH_SHORT).show();
            }else{
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }else{
            Toast.makeText(this, "Location permissions already granted", Toast.LENGTH_SHORT).show();
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            System.out.println("BLE NOT SUPPORTED");
            finish();
        }
        //stepcounter
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        list1 = new ArrayList<>();
        list2 = new ArrayList<>();

        overTH = new Dictionary();

        String[] heading = new String[2];
        heading[0]= "count";
        heading[1] = "Value";

        list1.add(heading);
        list2.add(heading);

        //Configure text to speech
        tts=new TextToSpeech(NavigationActivity.this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int result=tts.setLanguage(Locale.US);
                    tts.setSpeechRate((float) 0.90);
//                    tts.speak("Indoor Navigation Application Opened. Tap Anywhere on the screen to begin initializing", TextToSpeech.QUEUE_FLUSH, null);

                    if(result==TextToSpeech.LANG_MISSING_DATA ||
                            result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("error", "This Language is not supported");
                    }
                }
                else{
                    Log.e("error", "Initilization Failed!");
                }
            }
        });

        tts2=new TextToSpeech(NavigationActivity.this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int result=tts2.setLanguage(Locale.US);
                    tts2.setSpeechRate((float) 0.90);
//                    tts.speak("Indoor Navigation Application Opened. Tap Anywhere on the screen to begin initializing", TextToSpeech.QUEUE_FLUSH, null);

                    if(result==TextToSpeech.LANG_MISSING_DATA ||
                            result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("error", "This Language is not supported");
                    }
                }
                else{
                    Log.e("error", "Initilization Failed!");
                }
            }
        });


        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);



        //should give the initialization input here/////////////////////////////////////////////////////////////////////////////////////////////////////////

        //convert MAC string to JSONArray Object
        try {
            array =  new JSONArray(MAC_LIST);
            finalArray = new JSONArray();

            for (int jj=0;jj<array.length();jj++){
                if (array.getJSONObject(jj).getInt("isStaircase")==0){
                    finalArray.put(array.getJSONObject(jj));
                }else {
                    JSONArray convertedArray = divideSteps(array.getJSONObject(jj));
                    for (int jjj=0;jjj<convertedArray.length();jjj++){
                        finalArray.put(convertedArray.getJSONObject(jjj));
                    }
                }
            }


        } catch (JSONException e) {
            e.printStackTrace();
            Log.d("test",e.getMessage());
        }

        try {
            Log.d("rush",finalArray.toString(4));
        } catch (JSONException e) {
            e.printStackTrace();
        }


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i =0;i<finalArray.length();i++){
                    toScan = false;
                    try {
                        isinProximity = false;
                        JSONObject nextBeacon = finalArray.getJSONObject(i);
                        int isStaircase = nextBeacon.getInt("isStaircase");
                        if (isStaircase==0)
                        {
                            meanNormalize[0] = (float) nextBeacon.getDouble("meanNormalize1");
                            meanNormalize[1] = (float) nextBeacon.getDouble("meanNormalize2");
                            stdNormalize[0] = (float) nextBeacon.getDouble("stdNormalize1");
                            stdNormalize[1] = (float) nextBeacon.getDouble("stdNormalize2");

                            try{
                                String fileName = selectModel(nextBeacon.getString("mac"));
                                tflite = new Interpreter(loadModelFile(fileName));
                            } catch (IOException e) {
                                Log.d("rush","error");
                                e.printStackTrace();
                            }

                            now.setText("Current Process - Correcting Angle");
                            int angle = nextBeacon.getInt("angle");
                            requiredDegree = angle;
                            needed.setText("Needed Angle = "+Integer.toString(angle));

                            isCorrectDir = false;

                            toScan = true;

                            while (!isCorrectDir){
                                Log.d("test","..........");
                            }

                            nextBeaconMAC = nextBeacon.getString("mac");

                            String beaconDescription;
                            if (i==finalArray.length()-1){
                                beaconDescription  = nextBeacon.getString("endMsg");
                            }else {
                                beaconDescription = nextBeacon.getString("juncMsg");
                            }


//                          BLE Scanning
                            startScan();
                            now.setText("Current Process - Navigating");
                            while (!isinProximity){
                                Thread.sleep(50);
//                                Log.d("rush","scanning");
//                                boolean allEqual = predictionQueue.stream().distinct().limit(2).count() <= 1;
//                                if (allEqual && predictionQueue.element()==1){
//                                    break;
//                                }
                            }
                            stopScan();
                            toScan=false;
                            Log.d("hash","to scan false speech started");

                            convertTextToSpeech(beaconDescription);
//                            Thread.sleep(5000);
                            Log.d("rush","found direction of node "+ i);

                            queue.clear();
                            //change this later
                            for (int ii=0;ii<windowLength;ii++){
                                queue.add(0);
                            }

                        }
                        else
                        {

                            Log.d("rush","counting steps");

                            toStepCount = true;

                            String beaconDescription = nextBeacon.getString("endMsg");
                            now.setText("Current Process - Counting Steps");
                            Log.d("test","inelse");
                            isTopofStair = false;

                            numberOfStairs = nextBeacon.getInt("stairs");
                            needed.setText("Needed Steps = "+numberOfStairs);
                            current.setText("Current Steps = ");

                            // step counting
                            while (!isTopofStair){
                                Log.d("test","....");
                            }
                            tellIntro = true;
                            juncMsg="";
                            convertTextToSpeech(beaconDescription);
                            toStepCount = false;
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d("test",e.getMessage());
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                        Log.d("test",e.getMessage());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                toScan =false;

            }
        });

        t.start();
        //beginning of navigation
        //start looping the array

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //compass
        if (toScan && event.sensor.getType() == Sensor.TYPE_ORIENTATION){

            float degree = Math.round(event.values[0]);
            int currentDegree = (int)degree;
            Log.d("test", Integer.toString(currentDegree));

            current.setText("Current degree = "+Integer.toString(currentDegree));

            int allowedThreshould = 9;

            if (sensorCount==80){  //prev value 150
                Log.d("hash","inside dir");

                int lowDeg = currentDegree-allowedThreshould;
//                if (lowDeg<0){
//                    lowDeg = 360+lowDeg; //
//                }

                int highDeg = currentDegree + allowedThreshould;
//                if (highDeg>360){
//                    highDeg = highDeg - 360;
//                }

                if (this.requiredDegree<highDeg && this.requiredDegree>lowDeg){
                    Log.d("test","correct path");
                    isCorrectDir = true;
                    convertTextToSpeech("straight");

                }else if (this.requiredDegree<=lowDeg && (lowDeg - this.requiredDegree)<=180){
                    Log.d("test","turn left");
                    isCorrectDir = false;
                    convertTextToSpeech("left");
//step counting
                }else if (this.requiredDegree>=highDeg && (this.requiredDegree - lowDeg)<180){
                    Log.d("test","turn right");
                    isCorrectDir = false;
                    convertTextToSpeech("right");
                }
                else if (this.requiredDegree>=highDeg && (this.requiredDegree - lowDeg)>=180){
                    Log.d("test","turn left");
                    isCorrectDir = false;
                    convertTextToSpeech("left");
                }
                else if (this.requiredDegree<=highDeg && (lowDeg - this.requiredDegree)>180){
                    Log.d("test","turn right");
                    isCorrectDir = false;
                    convertTextToSpeech("right");
                }
                sensorCount=0;
            }else {
                sensorCount++;
            }
        }

        //stepCounting
        if (toStepCount && event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
//            Log.d("test","Step Counting");

//            if (!juncMsg.equals("NaN") && tellIntro) {
//                Log.d("rush","inside intial msg stair");
//                convertTextToSpeech(juncMsg);
//                try {
//                    Thread.sleep(3000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                tellIntro=false;
//            }

            //String[] temperory1 = new String[2];
            String[] temperory2 = new String[2];


            temperory2[0] = Long.toString(System.currentTimeMillis());
            temperory2[1] = Float.toString (event.values[2]);


            //boolean add1 = list1.add(temperory1);
            boolean add2 =list2.add(temperory2);


     /*  if (sensorEvent.values[2] > UT) {
        flag1 = true;
        }


       if (sensorEvent.values[2] < LT) {
          flag2 = true;
       }


       if (flag1 && flag2) {
          mStepDetectCounter++;
           flag1 = false ;
           flag2 = false ;
       }*/
            double accZ = event.values[2];
            if (accZ > accTh) {
                overTH.insert(System.currentTimeMillis(),
                        accZ);
                flag1 = true;
            } else {
                if (flag1) {
                    Entry<Long, Double> maxEntry = overTH.findMax();
                    double max = maxEntry.value();
                    long index = maxEntry.key();

                    if (index - preIndex > timeTH) {

                        step++;
                        Log.d("rush",step+"");
//                        if (step==Integer.parseInt(nextBreakpoint[0])){
//                            try {
//                                Thread.sleep(100);
//                            } catch (InterruptedException e) {
//                                e.printStackTrace();
//                            }
//                            Log.d("rush","at next beackpoint"+nextBreakpoint[1]);
//                            bpCount++;
//                            nextBreakpoint = breakPointList[bpCount];
//                        }

                        preIndex = index;
                    } else {
                        if (max >= maxPeak) {
                            maxPeak = max;
                        }
                    }
                    flag1=false;
                    overTH=new Dictionary<>();
                }
            }

            current.setText("Current Steps = " +step);

//            Log.d("rush",step+"");
//            if (step==Integer.parseInt(nextBreakpoint[0])){
//                Log.d("rush","at next beackpoint"+nextBreakpoint[1]);
//                bpCount++;
//                nextBreakpoint = breakPointList[bpCount];
//            }
            if (numberOfStairs == step){
                isTopofStair = true;
                step=0;
            }


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    //BLE Scan Methods
    public void startScan() {
        Log.d("rush","scan started");
        mBTDevicesHashMap.clear();
        mBTDevicesArrayList.clear();
        predictionQueue.clear();

        queue.clear();

        //change this later
        for (int i=0;i<windowLength;i++){
            queue.add(0);
        }

        Log.d("rush","added to queue");

        for (int i=0;i<3;i++){
            predictionQueue.add(100);
        }

        predictionQueue.add(150);
        mBTLeScanner.start();
    }

    public synchronized void stopScan() {
        mBTLeScanner.stop();
        stdevflag = false;
        try {
            writeCSV(finalValuelist);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addDevice(BluetoothDevice device, int rssi) {
        //set a queue to form a moving average
        // add each one and after adding  run through filter, run through ML model
        // add condition to break the infinite while loop

        Log.d("rush","recieved");

        String address = device.getAddress();

//        String ADD = "C4:52:32:5C:31:E7";
        if (!mBTDevicesHashMap.containsKey(address)) {
            BLTE_Device btleDevice = new BLTE_Device(device);
            btleDevice.setRSSI(rssi);
            mBTDevicesHashMap.put(address, btleDevice);
            mBTDevicesArrayList.add(btleDevice);

            if (address.equals(nextBeaconMAC)){
                queue.poll();
                queue.add(rssi);

                stdValue = getStandardDeviation(queue,20);   //window size
                medianValue = applyMedianFilter(queue);
                double kalmanValue = kalmanFilter.filter(rssi);
                double kalmanMedianValue = kalmanFilterForMedian.filter(medianValue);
                int prediction = classify((float) stdValue,(float)medianValue);
//                int prediction = classify((float) stdValue,(float)kalmanMedianValue);
//                int prediction = classification.classify((float) stdValue,(float) kalmanValue);
                Log.d("rush",(float) stdValue+"");
                Log.d("rush",(float) medianValue+"");
                Log.d("rush",prediction+"");
                predictionQueue.poll();
                predictionQueue.add(prediction);

                String pred=" ";
                if(prediction==0){
                    pred="Near";
                }else {
                    pred="Far";
                }
                t.setText("Current Prediction - "+pred);


                String[] a = new String[4];
                a[0] = Integer.toString(rssi);
                a[1] = Double.toString(medianValue);
                a[2] = Double.toString(stdValue);
                a[3] = Integer.toString(prediction);
                finalValuelist.add(a);

                boolean allEqual = predictionQueue.stream().distinct().limit(2).count() <= 1;
                if (allEqual && predictionQueue.element()==0){
                    isinProximity = true;
                }
            }
//            Random n = new Random();
//            if (address.equals(ADD)){
//                Log.d("rush","found found");
//            }
//            isinProximity = n.nextBoolean();

        } else {
            mBTDevicesHashMap.get(address).setRSSI(rssi);
            if (address.equals(nextBeaconMAC)){
                queue.poll();
                queue.add(rssi);
                stdValue = getStandardDeviation(queue,20);   //window size
                medianValue = applyMedianFilter(queue);
                double kalmanValue = kalmanFilter.filter(rssi);
                double kalmanMedianValue = kalmanFilterForMedian.filter(medianValue);
                int prediction = classify((float) stdValue,(float)medianValue);
//                int prediction = classify((float) stdValue,(float)kalmanMedianValue);

//                int prediction = classification.classify((float) stdValue,(float) kalmanValue);


                Log.d("rush",(float) stdValue+"");
                Log.d("rush",(float) kalmanMedianValue+"");
                Log.d("rush",prediction+"");
                predictionQueue.poll();
                predictionQueue.add(prediction);

                String pred=" ";
                if(prediction==0){
                    pred="Near";
                }else {
                    pred="Far";
                }
                t.setText("Current Prediction - "+pred);

                String[] a = new String[4];
                a[0] = Integer.toString(rssi);
                a[1] = Double.toString(medianValue);
                a[2] = Double.toString(stdValue);
                a[3] = Integer.toString(prediction);
                finalValuelist.add(a);

                boolean allEqual = predictionQueue.stream().distinct().limit(2).count() <= 1;
                if (allEqual && predictionQueue.element()==0){
                    isinProximity = true;
                }

            }
        }
//            Random n = new Random();
//            if (address.equals(ADD)){
//                Log.d("rush","found found");
//            }
//            isinProximity = n.nextBoolean();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this,mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        tts.shutdown();
        tts2.shutdown();
    }

    private synchronized void  convertTextToSpeech(String s) {
        if (!s.equals("left") && !s.equals("right") && !s.equals("straight")){
            toScan =false;
        }

        tts.speak(s, TextToSpeech.QUEUE_ADD, null);

        while (tts.isSpeaking()){
            Log.d("rush",".");
        }

        if (!s.equals("left") && !s.equals("right") && !s.equals("straight")){
            toScan =true;
        }
    }


    private void beep(){
        toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,500);
    }

    private void requestPermissionAndContinue() {
        if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, WRITE_EXTERNAL_STORAGE)
                    && ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setCancelable(true);
                alertBuilder.setTitle("permission_necessary");
                alertBuilder.setMessage("storage_permission_is_encessary_to_wrote_event");
                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(NavigationActivity.this, new String[]{WRITE_EXTERNAL_STORAGE
                                , READ_EXTERNAL_STORAGE}, 200);
                    }
                });
                AlertDialog alert = alertBuilder.create();
                alert.show();
                Log.e("", "permission denied, show dialog");
            } else {
                ActivityCompat.requestPermissions(NavigationActivity.this, new String[]{WRITE_EXTERNAL_STORAGE,
                        READ_EXTERNAL_STORAGE}, 200);
            }
        } else {
            openActivity();
        }
    }

    private float applyMeanFilter(Queue<Integer> queue){
        float avg = 0;

        for (int i:queue){
            avg = avg + i;
        }

        avg = avg /queue.size();

//            r = Math.pow(10.0,(-76.57-avg)/20);
//            movingAverage.add((float)r);

        return avg;
    }

    private float applyMedianFilter(Queue<Integer> queue){
        int c= Collections.frequency(queue,0);


        int[] numArray = new int[queue.size()-c];

        int count = 0;

        for (Integer i:queue){
            if (i!=0) {
                numArray[count] = i;
                count++;
            }

        }

        Arrays.sort(numArray);

        double median;

        if (numArray.length % 2 == 0)
            median = ((double)numArray[numArray.length/2] + (double)numArray[numArray.length/2 - 1])/2;
        else
            median = (double) numArray[numArray.length/2];

        return (float)median;
    }

    private double getStandardDeviation(Queue<Integer> numQueue,int windowLength){
//      Integer.parseInt(windowSize.getText().toString())
        int c= Collections.frequency(numQueue,0);
        double std = 0.0;
        if (c==0){
            stdevflag=true;
        }else {
            Log.d("rush","insidee");
            double[] stdArray2 = new double[windowLength-c];
            int k = 0;
            for (int i : numQueue) {
                if (i!=0){
                    double val = (double) i;
                    stdArray2[k] = val;
                    k++;
                }
            }

            std = standardDeviation.evaluate(stdArray2);
        }

        if (stdevflag) {
            double[] stdArray = new double[windowLength];
            int k = 0;
            for (int i : numQueue) {
                double val = (double) i;
                stdArray[k] = val;
                k++;
            }



            std = standardDeviation.evaluate(stdArray);
        }
        return std;
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    private void openActivity() {
        //add your further process after giving permission or to download images from remote server.
    }

    // Step Counting Aid Classes
    public class Dictionary<Long> {
        private List<Entry<Long, Double>> set;

        public Dictionary() {
            this.set = new LinkedList<Entry<Long, Double>>();
        }

        public Entry<Long, Double> find(Long key) {
            for (Entry<Long, Double> entry : set) {
                if (entry.key() == key) {
                    return entry;
                }
            }
            return null;
        }


        public Entry<Long, Double> findMax() {
            Entry<Long, Double> maxEntry = null;
            for (Entry<Long, Double> entry : set) {
                if (maxEntry == null) {
                    maxEntry = entry;
                }
                if (maxEntry.value() < entry.value()) {
                    maxEntry = entry;
                }
            }
            return maxEntry;
        }

        public void insert(Long key, Double value) {
            set.add(new Entry<Long, Double>(key, value));

        }

        public void remove(Long key) {
            for (Entry<Long, Double> entry : set) {
                if (entry.key() == key) {
                    set.remove(entry);
                }
            }
        }

    }

    public class Entry<K, V> {
        private K key;
        private V value;

        public Entry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K key() {
            return key;
        }

        public V value() {
            return value;
        }
    }

    public void writeCSV(List<String[]> a) throws IOException {

        String topic = "";
        Random r = new Random();
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        Log.d("Write",baseDir);
        Date date = new Date();
        String text = Long.toString(date.getTime());
        String fileName = text+topic+".csv";
        String filePath = baseDir + File.separator + fileName;
        File f = new File(filePath);
        CSVWriter writer;
        // File exist
        if(f.exists()&&!f.isDirectory())
        {
            mFileWriter = new FileWriter(filePath, true);
            writer = new CSVWriter(mFileWriter);
        }
        else
        {
            writer = new CSVWriter(new FileWriter(filePath));
        }

        for (String[] s:a){
            writer.writeNext(s);
        }

        writer.close();
        Log.d("Write","written");
        System.out.println("writeeeeeeeeeeeeeeeeee");

    }

    public int classify(float std, float rssi){
        float result = doInterference(std,rssi);
        if (result>0.5){
            return 1;
        }else{
            return 0;
        }
    }

    private float doInterference(float std, float rssi){
        float[] normalized = normalization(std,rssi);

        float[] inputVal = new float[2];
        inputVal[0] = normalized[0];
        inputVal[1] = normalized[1];

        float[][] outputval = new  float[1][1];
        tflite.run(inputVal,outputval);
        return outputval[0][0];
    }

    private MappedByteBuffer loadModelFile(String fileName) throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(fileName);
        FileInputStream inputStream = new FileInputStream((fileDescriptor.getFileDescriptor()));
        FileChannel fileChannel = inputStream.getChannel();
        long startoffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startoffset,declaredLength);
    }

    private float[] normalization(float std, float kalman){
        float normalSTD = (std - meanNormalize[0])/ stdNormalize[0];
        float normalkalman = (kalman - meanNormalize[1])/ stdNormalize[1];
        float[] result = {normalSTD,normalkalman};
        return result;
    }

    private JSONArray divideSteps(JSONObject o) throws JSONException {
        JSONArray breakPoints = o.getJSONObject("staircase").getJSONArray("breakPoints");
        JSONArray returnArray = new JSONArray();
        int stepCount = 0;

        JSONObject oo = new JSONObject();
        oo.put("MAC","");
        oo.put("endMsg",breakPoints.getJSONObject(0).getString("msg"));
        oo.put("juncMsg","Now You are about to climb a staircase. Please be careful.");
        oo.put("angle",0);
        oo.put("isStaircase",1);
        oo.put("stairs",breakPoints.getJSONObject(0).getInt("steps")-stepCount);
        stepCount = breakPoints.getJSONObject(0).getInt("steps");
        oo.put("meanNormalize1",0);
        oo.put("meanNormalize2",0);
        oo.put("stdNormalize1",0);
        oo.put("stdNormalize2",0);
        returnArray.put(oo);

        for (int i=1;i<breakPoints.length();i++){
//            "{\n" +
//                    "  \"MAC\":\"NaN\",\n" +
//                    "  \"description\":\"NaN\",\n" +
//                    "  \"angle\":0,\n" +
//                    "  \"isStaircase\":1,\n" +
//                    "  \"stairs\":24,\n" +
//                    "  \"meanNormalize1\":0,\n" +
//                    "  \"meanNormalize2\":0,\n" +
//                    "  \"stdNormalize1\":0,\n" +
//                    "  \"stdNormalize2\":0,\n" +
//                    "  \"staircase\":{ \"endMsg\":\"you arrived\",\n" +
//                    "                \"numberOfSteps\":24,\n" +
//                    "                \"breakPoints\":[\n" +
//                    "                  {\"steps\":10,\"msg\":\"turn slightly right\"},{\"steps\":15,\"msg\":\"turn slightly left\"}]\n" +
//                    "              }\n" +
//                    "},\n" +

            JSONObject oo2 = new JSONObject();
            oo2.put("MAC","");
            oo2.put("endMsg",breakPoints.getJSONObject(i).getString("msg"));
            oo2.put("juncMsg","NaN");
            oo2.put("angle",0);
            oo2.put("isStaircase",1);
            oo2.put("stairs",breakPoints.getJSONObject(i).getInt("steps")-stepCount);
            stepCount = breakPoints.getJSONObject(i).getInt("steps");
            oo2.put("meanNormalize1",0);
            oo2.put("meanNormalize2",0);
            oo2.put("stdNormalize1",0);
            oo2.put("stdNormalize2",0);
            returnArray.put(oo2);

        }

        JSONObject oo1 = new JSONObject();
        oo1.put("MAC","");
        oo1.put("endMsg",o.getJSONObject("staircase").getString("msg"));
        oo1.put("juncMsg","NaN");
        oo1.put("angle",0);
        oo1.put("isStaircase",1);
        oo1.put("stairs",o.getJSONObject("staircase").getInt("numberOfSteps")-stepCount);
        oo1.put("meanNormalize1",0);
        oo1.put("meanNormalize2",0);
        oo1.put("stdNormalize1",0);
        oo1.put("stdNormalize2",0);
        returnArray.put(oo1);

        return returnArray;
    }

    public String selectModel(String m){
        String m1;
        switch(m) {
            case "C4:52:32:5C:31:E7":
                Log.d("rush","vision file");
                m1= "vision_median.tflite";
                break;
            case "FA:35:76:56:6F:E3":
                Log.d("rush","j2 file");
                m1= "junc2_median.tflite";
                break;
            case "EA:01:26:75:A4:C3":
                m1= "dialog_median.tflite";
                // code block
                break;
            case "D0:4E:10:2E:CB:84":
                m1= "j3_median.tflite";
                // code block
                break;
            case "D9:5F:F5:4F:10:89":
                m1= "mw_median.tflite";
                // code block
                break;
            case "ED:61:E4:E8:22:30":
                m1= "telecom_new_median.tflite";
                // code block
                break;
            default:
                Log.d("rush","inside default");
                m1= "j2_dialog_j3_median.tflite";
                Log.d("rush",m1);
                // code block
        }

        return m1;
    }



}
