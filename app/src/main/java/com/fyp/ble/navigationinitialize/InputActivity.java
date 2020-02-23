package com.fyp.ble.navigationinitialize;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

public class InputActivity extends AppCompatActivity {

    String[] destinations;
    String[] adjustedDestinations;
    String[] macs;
    String initialMac;
    private TextView txtSpeechInput;
    private ImageButton btnSpeak;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    TextToSpeech tts;
    Map<String,String> combinedList;
    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);
        requestQueue = Volley.newRequestQueue(this);

        Intent intent = getIntent();
        destinations = intent.getStringArrayExtra("destinations");
        adjustedDestinations = new String[destinations.length];
        macs = intent.getStringArrayExtra("macs");
        initialMac = intent.getStringExtra("initialMac");
        combinedList = new HashMap<>();



        for (int i=0;i<destinations.length;i++){
            adjustedDestinations[i]=destinations[i].toLowerCase();
        }

        for (int j = 0;j<adjustedDestinations.length;j++){
            combinedList.put(adjustedDestinations[j],macs[j]);
        }

        for (String s:macs){
            Log.d("rush",s);
        }

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Tap on the screen and say where you want to go.");
        stringBuilder.append("Your choices are,");

        int count = 0;
        for (String k:adjustedDestinations){
            stringBuilder.append(k);
            count++;
            if (count!=adjustedDestinations.length-1) {
                stringBuilder.append(", ");
            }else {
                stringBuilder.append(".");
            }
        }

        Log.d("op",stringBuilder.toString());

        //Configure text to speech
        tts = new TextToSpeech(InputActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    tts.setSpeechRate((float) 0.88);
                    convertTextToSpeech(stringBuilder.toString());
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("error", "This Language is not supported");
                    }
                } else
                    Log.e("error", "Initilization Failed!");
            }
        });

        txtSpeechInput = (TextView) findViewById(R.id.txtSpeechInput);
        btnSpeak = (ImageButton) findViewById(R.id.btnSpeak);

        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });


    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                getString(R.string.speech_prompt));
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.speech_not_supported),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiving speech input
     * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    txtSpeechInput.setText(result.get(0));

                    String s = result.get(0).toLowerCase();
                    if (combinedList.containsKey(s)){
                        convertTextToSpeech("Roger that, you want to go to "+s+"."+" Please Start navigating now.");
                        try {
                            Thread.sleep(4000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d("rush",initialMac);
                        Log.d("rush",combinedList.get(s));//calculatePath

                        JsonArrayRequest request2 = new JsonArrayRequest(Request.Method.GET, "http://192.168.8.101:8080/api/calculatePath?s="+initialMac+"&d="+s, null,
                                new Response.Listener<JSONArray>() {
                                    @Override
                                    public void onResponse(JSONArray response) {
                                        try
                                        {
                                            Log.d("rush",response.toString());
                                            Intent myIntent = new Intent(InputActivity.this, NavigationActivity.class);
                                            myIntent.putExtra("MAC_LIST", response.toString()); //Optional parameters
                                            InputActivity.this.startActivity(myIntent);


//                                for (String i:destinations){
//                                    convertTextToSpeech(i);
//                                    Thread.sleep(1500);
//                                }



//                                convertTextToSpeech("The destinations which are available are, ");
//                                Toast.makeText(getApplicationContext(),"Destinations Stored",Toast.LENGTH_SHORT).show();

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




                    }else {
                        convertTextToSpeech("Not Clear, please speak again.");
                        Log.d("op",s);
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        promptSpeechInput();
                    }


                    Log.d("op",result.get(0));
                }
                break;
            }

        }
    }

    private void convertTextToSpeech(String s) {
        tts.speak(s, TextToSpeech.QUEUE_FLUSH, null);
    }



//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
}
