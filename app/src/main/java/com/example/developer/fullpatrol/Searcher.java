package com.example.developer.fullpatrol;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;


import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class Searcher extends AsyncTask<String, Integer, String> {
    private final SiteAdapter Adp;
    private final ProgressDialog progressDialog;
    RequestQueue ExampleRequestQueue;

    public Searcher(Context context, SiteAdapter adp, ProgressDialog prog){
        this.Adp = adp;
        this.progressDialog = prog;
        RequestQueue xampleRequestQueue = Volley.newRequestQueue(context);

    }
    @Override
    protected String doInBackground(String... strings) {
        String url = strings[0];

        JsonRequest jsonRequest = new JsonArrayRequest(url, new Response.Listener<JSONArray>() {

            @Override
            public void onResponse(JSONArray response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
        ExampleRequestQueue.add(jsonRequest);
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        this.Adp.clear();
        if(result != null){
            try {
                JSONArray json = new JSONArray(result);
                if(json.length() > 0){
                    for(int pos = 0; pos < json.length(); pos++){
                       JSONObject doc =  json.getJSONObject(pos);
                        this.Adp.add(new SiteAdapter.SiteObject(doc.get("siteName").toString(), doc.get("area").toString(), doc.get("documentID").toString()));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        progressDialog.dismiss();
        super.onPostExecute(result);
    }
}
