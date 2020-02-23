package com.fyp.ble.navigationinitialize;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import org.json.JSONObject;
import java.util.Locale;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    Button button;
    TextView textView;
//    HTTPRequest httpRequest;
    RequestQueue requestQueue;

    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = (Button)findViewById(R.id.button);
        button.setText(Html.fromHtml("R<sup>3</sup>H"));

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

//        textView = (TextView)findViewById(R.id.textView);
//        requestQueue = Volley.newRequestQueue(this);


        //Configure text to speech
        tts=new TextToSpeech(MainActivity.this, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    int result=tts.setLanguage(Locale.US);
                    tts.setSpeechRate((float) 0.92);
                    tts.speak("Indoor Navigation Application Opened. Tap Anywhere on the screen to begin initializing", TextToSpeech.QUEUE_FLUSH, null);
                    if(result==TextToSpeech.LANG_MISSING_DATA ||
                            result==TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.e("error", "This Language is not supported");
                    }
                }
                else
                    Log.e("error", "Initilization Failed!");
            }
        });


//        httpRequest = new HTTPRequest(requestQueue);

        final JSONObject[] jsonObject = new JSONObject[1];

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//Navigate to the next activity
                Intent myIntent = new Intent(MainActivity.this, Description_Activity.class);
                MainActivity.this.startActivity(myIntent);

//                JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, "http://192.168.43.139:8080/getAllInitial", null,
//                        new Response.Listener<JSONArray>() {
//                            @Override
//                            public void onResponse(JSONArray response) {
//                                try
//                                {
//                                        JSONObject w = response.getJSONObject(0);
//                                        jsonObject[0] = w;
//                                        textView.setText(jsonObject[0].getString("description"));
//                                        convertTextToSpeech(jsonObject[0].getString("description"));
//                                }
//                                catch (Exception e)
//                                {
//                                        System.out.println(e.getMessage());
//                                }
//                            }},new Response.ErrorListener() {
//
//                                        @Override
//                                        public void onErrorResponse(VolleyError error) {
//            //                                System.out.println(error.networkResponse.toString());
//                                            Log.d("test",error.networkResponse.toString());
//            //                                System.out.println(error.getMessage());
//
//                            }});
//                                        //                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
////                        (Request.Method.GET, "http://192.168.8.100:8080/getAllInitial", null, new Response.Listener<JSONObject>() {
////
////                            @Override
////                            public void onResponse(JSONObject response) {
//////                                textView.setText("Response: " + response.toString());
////                                Log.d("test","res came");
////                            }
////                        }, new Response.ErrorListener() {
////
////                            @Override
////                            public void onErrorResponse(VolleyError error) {
////                                Log.d("test",error.getMessage());
////                                System.out.println(error.getMessage());
////
////                            }
////                        });
//
//
//                            // Request a string response from the provided URL.
////                StringRequest stringRequest = new StringRequest(Request.Method.GET, "http://192.168.43.139:8080/hello",
////                        new Response.Listener<String>() {
////                            @Override
////                            public void onResponse(String response) {
////                                // Display the first 500 characters of the response string.
////                                System.out.println("something");
////                                textView.setText("Response is: "+ response.substring(0,11));
////                            }
////                        }, new Response.ErrorListener() {
////                    @Override
////                    public void onErrorResponse(VolleyError error) {
////                        textView.setText(error.getMessage());
////                        Log.d("test",error.getMessage());
//////                        System.out.println(error.getMessage());
////                    }
////                });
//
//// Add the request to the RequestQueue.
////                requestQueue.add(request);

            }});

    }

    private void convertTextToSpeech(String s) {
        tts.speak(s, TextToSpeech.QUEUE_FLUSH, null);
    }

    private boolean checkPermission() {

        return ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                ;
    }

    private void openActivity() {
        //add your further process after giving permission or to download images from remote server.
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
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE
                                , READ_EXTERNAL_STORAGE}, 200);
                    }
                });
                AlertDialog alert = alertBuilder.create();
                alert.show();
                Log.e("", "permission denied, show dialog");
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE,
                        READ_EXTERNAL_STORAGE}, 200);
            }
        } else {
            openActivity();
        }
    }

}
