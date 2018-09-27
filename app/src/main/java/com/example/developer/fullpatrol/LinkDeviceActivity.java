package com.example.developer.fullpatrol;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LinkDeviceActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String SHARED_DEVICE_ID = "Shared Device id";
    public static final String SHARED_SITE_ID = "Shared Site id";
    public static final String SHARED_SITE_ID_INT = "Shared Site id int";
    public static final String PREF_UID = "PREF_UID";
    public static final String PREF_LINKED_SITE = "PREF_LINKED_SITE";
    public static final String PREF_LINKED_SITE_NAME= "PREF_LINKED_SITE_NAME";
    public static final String PREF_LINKED_SITE_AREA = "PREF_LINKED_SITE_AREA";

    private TextView ttvUID;
    private EditText  edtSearch;
    private ImageView imgSearch;
    private ListView lstSiteDisplay;
    private Button btnDone;

    private SiteAdapter adp;
    private SharedPreferences sharedPreferences;

    private ProgressDialog progressDialog;
    private int selectedPosition = 0;

    FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_activty);
        this.firebaseManager  = FirebaseManager.getInstance();
        this.firebaseManager.init();

        this.ttvUID = this.findViewById(R.id.ttv_device_id);
        this.edtSearch = this.findViewById(R.id.edt_search_input);

        this.imgSearch = this.findViewById(R.id.img_search);
        this.imgSearch.setOnClickListener(this);

        this.btnDone = this.findViewById(R.id.btn_done);
        this.btnDone.setOnClickListener(this);
        this.lstSiteDisplay = this.findViewById(R.id.lst_sites_display);

         this.adp = new SiteAdapter(this, R.layout.site_item, new ArrayList<SiteAdapter.SiteObject>());
         lstSiteDisplay.setAdapter(adp);
        lstSiteDisplay.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        lstSiteDisplay.setSelector(R.color.colorAccent);
         lstSiteDisplay.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
                selectedPosition = position;
                if(!btnDone.isEnabled()){
                    btnDone.setEnabled(true);
                    btnDone.setBackgroundResource(R.color.colorAccent);
                }
                //lstSiteDisplay.setSelection(position);
                //view.setSelected(true);
               //view.setBackgroundResource(R.color.colorAccent);
            }
        });

        this.sharedPreferences = this.getSharedPreferences(this.getPackageName(), MODE_PRIVATE);
        String UID = this.sharedPreferences.getString(PREF_UID, null);
        if(UID == null){
            generateUID();
        }else{
            ttvUID.setText(UID);
        }
    }

    private void generateUID() {
        showProgress("Unique Id", "Generating Unique id for device...");

        firebaseManager.getCollection("machines", "timeStamp", 1, new FirebaseManager.DataCallback() {
            @Override
            public void onDataUpdated(Map<String, Object> data) {

            }

            @Override
            public void onDataReceived(Map<String, Object> data) {

            }

            @Override
            public void onDataReceived(List<Map<String, Object>> data) { 
                String UID = "0000";
               if(data.size() > 0){
                   Map<String, Object> lastDevice = data.get(0);
                   int lastId = Integer.decode("0x" + lastDevice.get("documentID"));
                   lastId++;
                   String hex = Integer.toHexString(lastId).replace("0x", "");

                   UID =  String.format("%4s", hex).replace(' ', '0');

                }

                ttvUID.setText(UID);
                createDeviceOnFireStore(UID);
            }
        });
    }

    private void showProgress(String title, String msg) {
        if(progressDialog == null){
            View parent = LayoutInflater.from(this).inflate(R.layout.progress_layout, null, false);
            TextView ttvTitle = parent.findViewById(R.id.ttv_title), ttvMsg = parent.findViewById(R.id.ttv_msg);
            ttvTitle.setText(title);
            ttvMsg.setText(msg);
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            progressDialog.setContentView(parent);

        }
    }

    private void dismissProgress(){
        if(progressDialog != null){
            progressDialog.dismiss();
            progressDialog  = null;
        }
    }

    private void createDeviceOnFireStore(final String uid) {
        firebaseManager.addDevice("machines", false,"none", uid, new FirebaseManager.DataPushCallack() {
            @Override
            public void onPushed() {
                dismissProgress();
                sharedPreferences.edit().putString(PREF_UID, uid).apply();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.img_search:
                if(edtSearch.getText().length() >  0){
                    InputMethodManager inputManager = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
                    inputManager.hideSoftInputFromWindow(
                            edtSearch.getWindowToken(),
                            InputMethodManager.HIDE_NOT_ALWAYS);
                    showProgress("Search", "Connecting to server...");
                    search(edtSearch.getText().toString());
                }
                    break;
            case R.id.btn_done:
                 linkDevice(adp.getItem(selectedPosition));
                break;
        }
    }

    private void linkDevice(final SiteAdapter.SiteObject selectedItem) {
        String uid = ttvUID.getText().toString();
        showProgress("Link", "Linking Device [ " + uid + " ] with Site [ " + selectedItem.Name + " ]");
        firebaseManager.linkDevice("site/" + selectedItem.DocID + "/siteMachines", uid, new FirebaseManager.DataPushCallack() {
            @Override
            public void onPushed() {
                dismissProgress();
                sharedPreferences.edit().putString(PREF_LINKED_SITE, selectedItem.DocID)
                        .putString(PREF_LINKED_SITE_NAME, selectedItem.Name)
                        .putString(PREF_LINKED_SITE_AREA, selectedItem.Area)
                        .apply();
                finish();
                Log.i("RFV", selectedItem.Name + "|" + selectedItem.Area + "|" + selectedItem.DocID);
            }
        }, selectedItem.DocID);
    }

    public void connect(String query){
        try {

            query = URLEncoder.encode(query.trim(), "utf-8");

            RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
            String url = "https://us-central1-project-apple-34c2c.cloudfunctions.net/helloWorld?site=" + query;


            // Request a string response from the provided URL.
            StringRequest stringRequest = new StringRequest( Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            // Display the first 500 characters of the response string.
                            adp.clear();
                            try {
                                JSONArray json;
                                json = new JSONArray(response);

                                if(json.length() > 0){
                                    for(int pos = 0; pos < json.length(); pos++){
                                        try {
                                            JSONObject doc;
                                            doc = json.getJSONObject(pos);
                                            adp.add(new SiteAdapter.SiteObject(doc.get("siteName").toString(), doc.get("area").toString(),  doc.get("documentId").toString()));

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            dismissProgress();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String test = "That didn't work!";
                    Toast.makeText(getApplicationContext(), test, Toast.LENGTH_SHORT).show();
                    Log.i("dcf", "onErrorResponse: " + error.getMessage());
                }
            });
            // Add the request to the RequestQueue.
            stringRequest.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            queue.add(stringRequest);


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

    }
    private void search(String query) {
       // Searcher searcher = new Searcher(adp, progressDialog);
        //searcher.execute("https://us-central1-project-apple-34c2c.cloudfunctions.net/helloWorld?site=" + query);
        RequestQueue ExampleRequestQueue = Volley.newRequestQueue(this);

        String url = "https://us-central1-project-apple-34c2c.cloudfunctions.net/helloWorld?site=" + query;

        connect(query);



//        FirebaseManager firebaseManager = FirebaseManager.getInstance();
//        firebaseManager.query("site", "siteName", query, new FirebaseManager.DataCallback() {
//            @Override
//            public void onDataReceived(Map<String, Object> data) {
//
//            }
//
//            @Override
//            public void onDataReceived(List<Map<String, Object>> data) {
//                adp.clear();
//
//                for(int pos = 0; pos < data.size(); pos++){
//                    Log.i("RFC", "GsOT");
//
//                    Map<String, Object> doc = data.get(pos);
//                    adp.add(new SiteAdapter.SiteObject(doc.get("siteName").toString(), doc.get("area").toString(), doc.get("documentID").toString()));
//                }
//                dismissProgress();
//            }
//        });
    }
}
