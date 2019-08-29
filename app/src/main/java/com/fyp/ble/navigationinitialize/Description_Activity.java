package com.fyp.ble.navigationinitialize;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.fyp.ble.navigationinitialize.BLE.BLTE_Device;
import com.fyp.ble.navigationinitialize.BLE.Scanner_BLTE;
import com.fyp.ble.navigationinitialize.HTTP.HTTPRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class Description_Activity extends AppCompatActivity {

    //BLE
    private HashMap<String, BLTE_Device> mBTDevicesHashMap;
    private ArrayList<BLTE_Device> mBTDevicesArrayList;
    //    ListAdapter_BTLE_Devices adapter;
    private Scanner_BLTE mBTLeScanner;

    private ArrayList<String> itrList = new ArrayList<>();
    private ArrayList<String> macList = new ArrayList<>();
    private ArrayList<String> allValues = new ArrayList<>();
    private static final int PERMISSION_REQUEST_CODE = 200;

    TextToSpeech tts;
    HTTPRequest httpRequest;
    RequestQueue requestQueue;
    public String[] macList2;
    public String[] descriptionList;
    public Map<String, String> destinations;

    public boolean activateButton = false;


    public String locatedInitialMAC;
    public String selectedLocationMAC;
    public String selectedLocationName;


    public boolean flag = false;

    StringBuilder stringBuilder;
    TextView textView;
    Button button2;
    Button button3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description_);

        final Handler handler = new Handler(getMainLooper());

        //Configure HTTP Request
        requestQueue = Volley.newRequestQueue(this);
        destinations = new HashMap<>();

        mBTLeScanner = new Scanner_BLTE(this, 5000, -100);
        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();

        textView = (TextView) findViewById(R.id.textView);
        stringBuilder = new StringBuilder();


        //Configure text to speech
        tts = new TextToSpeech(Description_Activity.this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    tts.setSpeechRate((float) 0.90);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("error", "This Language is not supported");
                    }
                } else
                    Log.e("error", "Initilization Failed!");
            }
        });


        button2 = (Button) findViewById(R.id.button2);

        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag = true;
                convertTextToSpeech("selected destination is, " + selectedLocationName);
            }
        });


        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, "http://192.168.8.101:8080/getAllInitial", null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            macList2 = new String[response.length()];
                            descriptionList = new String[response.length()];

                            for (int i = 0; i < response.length(); i++) {
                                macList2[i] = response.getJSONObject(i).getString("mac");
                                descriptionList[i] = response.getJSONObject(i).getString("description");
                            }
                            convertTextToSpeech("Responses came and the scan started");
                            startScan();
                            Toast.makeText(getApplicationContext(), "Done", Toast.LENGTH_SHORT).show();

                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            System.out.println(e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("test", error.networkResponse.toString());
            }
        });

        requestQueue.add(request);

        

    }

    public synchronized void stopScan() {
        mBTLeScanner.stop();
        Iterator it = mBTDevicesHashMap.entrySet().iterator();
        boolean flag = false;

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();

            String s = (String) pair.getKey();

            for (int h = 0; h < macList2.length; h++) {
                if (macList2[h].equals(s)) {
                    flag = true;
                    locatedInitialMAC = macList2[h];
                    convertTextToSpeech("The scan stopped.An initializer MAC address has been found. The found MAC address is." + locatedInitialMAC + "." + "Hello!" + descriptionList[h]);
                    break;
                }
            }

            if (flag) {
                break;
            }
        }

        if (!flag) {
            convertTextToSpeech("The scan stopped. No initializer beacons has been found. Please try again");
        } else {

            JsonArrayRequest request2 = new JsonArrayRequest(Request.Method.GET, "http://192.168.8.101:8080/getDestinations", null,
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            try
                            {
                                for (int i=0;i<response.length();i++){
                                    destinations.put(response.getJSONObject(i).getString("mac"),response.getJSONObject(i).getString("location"));
                                }

                                Toast.makeText(getApplicationContext(),"Destinations Stored",Toast.LENGTH_SHORT).show();

                            }
                            catch (Exception e)
                            {
                                Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                                System.out.println(e.getMessage());
                            }
                        }},new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("test",error.networkResponse.toString());
                }});
            requestQueue.add(request2);
        }
    }

    //BLE Scan Methods
    public void startScan() {
        mBTDevicesHashMap.clear();
        mBTDevicesArrayList.clear();
        mBTLeScanner.start();
    }

    public synchronized void addDevice(BluetoothDevice device, int rssi) {
        String address = device.getAddress();
        if (!mBTDevicesHashMap.containsKey(address)) {
            BLTE_Device btleDevice = new BLTE_Device(device);
            btleDevice.setRSSI(rssi);
            mBTDevicesHashMap.put(address, btleDevice);
            mBTDevicesArrayList.add(btleDevice);
        } else {
            mBTDevicesHashMap.get(address).setRSSI(rssi);
        }
    }

    //Text to Speech
    private void convertTextToSpeech(String s) {
        tts.speak(s, TextToSpeech.QUEUE_FLUSH, null);
    }


}
