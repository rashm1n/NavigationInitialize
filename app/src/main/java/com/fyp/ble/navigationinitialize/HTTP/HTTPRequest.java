package com.fyp.ble.navigationinitialize.HTTP;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

public class HTTPRequest {
    RequestQueue queue;
    public String response;

    public HTTPRequest(RequestQueue queue) {
        this.queue = queue;
        this.response = "";
    }

    public String sendSimpleRequest(String url){

        final String[] getRes = new String[1];

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                getRes[0] = response.substring(0,500);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                getRes[0] = "ERROR";
            }
        });

        return getRes[0];
    }
}
