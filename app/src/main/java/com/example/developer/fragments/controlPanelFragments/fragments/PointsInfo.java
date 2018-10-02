package com.example.developer.fragments.controlPanelFragments.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.example.developer.adapters.PatrolPointLearningModeAdapter;
import com.example.developer.fragments.KioskFragment;
import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.fullpatrol.R;
import com.example.developer.objects.PatrolPointConfig;
import com.example.developer.services.TrackDeviceLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.LOCATION_SERVICE;

public class PointsInfo extends KioskFragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    public static final String POINT_CONTENT = "content";
    public static final float PERCENTAGE = 25/100;
    private static final int REQ_DONE_LEARNING_MODE = 999;
    private static final int REQ_SCAN_LEARNING_MODE = 1000;
    private ListView listViewLearningMode;
    private TextView crcDuration;
    private List<PatrolPointConfig> pointConfigs;
    private CardView cvDuration;
    private SwitchCompat enableLearningMode;


    private PatrolPointLearningModeAdapter pointLearningModeAdapter;
    private Chronometer patrolDurationConfig;
    private TrackDeviceLocation deviceLocation;
    private LocationManager locationManager;
    private BroadcastReceiver updateLocationReceiver;
    private FusedLocationProviderClient mfusedLocationProviderclient;
    IntentFilter filter;
    private TextView pointCount;
    private boolean gIsChecked;
    private int randPoint;
    private String title = "Point Info";
    Bundle extraData;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("ZAQ", "onCreate: ");

        pointConfigs = new ArrayList<>();
        deviceLocation = new TrackDeviceLocation(getContext());
        locationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);
        mfusedLocationProviderclient = LocationServices.getFusedLocationProviderClient(getContext());


        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 150000, 0, deviceLocation);

        filter = new IntentFilter();
        filter.addAction(TrackDeviceLocation.ACTION_LOCATION_BROADCAST);

        updateLocation();


    }

    private void getDeviceCurrentLocation(final String pointId) {
        Log.i("ZAQ!", "close: " + pointId);
        Log.i("ZAQ!", "onFragmentResult: 4");
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i("ZAQ!", "close: no permission");
            return;
        }
        mfusedLocationProviderclient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null){
                    Log.i("ZAQ", "fused location: ");
                    Log.i("ZAQ", "Fused Location Client: "
                            + location.getLatitude()
                            +" | "
                            + location.getLongitude()
                            + " | "
                            + TrackDeviceLocation.getTime());

                    Toast.makeText(getContext(), location.getLatitude()
                            +" | "+ location.getLongitude()
                            + " | "
                            + TrackDeviceLocation.getTime()
                            , Toast.LENGTH_SHORT).show();
                    GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());

                    addPoint(pointId, geoPoint);


                }else{
                    Toast.makeText(getContext(), "Failed to get Location", Toast.LENGTH_LONG).show();
                    GeoPoint geoPoint = new GeoPoint(0, 0);

                    addPoint(pointId, geoPoint);
                }
            }
        });

    }
    private void updateLocation(){
        updateLocationReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i("ZAQ", "onReceive: "+ "updateLocation");

                if(intent != null){

                    Log.i("ZAQ", "onLocationChanged: "
                            + intent.getDoubleExtra("latitude", 0)
                            + " | "
                            + intent.getDoubleExtra("longitude", 0) + " | "
                            +intent.getStringExtra("timestamp"));

                    Toast.makeText(context, intent.getDoubleExtra("latitude", 0)
                            +" | "+ intent.getDoubleExtra("longitude", 0)
                            + " | " +intent.getStringExtra("timestamp")
                            , Toast.LENGTH_SHORT).show();

                }

                }
        };

        getActivity().registerReceiver(this.updateLocationReceiver, filter);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        Log.i("ZAQ", "onCreateView: PatrolPointsInfo");
        View view = inflater.inflate(R.layout.points_info, container, false);



        cvDuration = view.findViewById(R.id.cv_duration);
        enableLearningMode = view.findViewById(R.id.spn_enable_lm);
        listViewLearningMode = view.findViewById(R.id.listview_lm);
        pointCount = view.findViewById(R.id.view_content_lm);
        patrolDurationConfig = view.findViewById(R.id.chr_patrol_duration); // initiate a chronometer
        // patrolDurationConfig.setFormat("00:00:00");

        pointLearningModeAdapter = new PatrolPointLearningModeAdapter(this.getActivity(), R.layout.site_item, new ArrayList<PatrolPointConfig>());
        listViewLearningMode.setAdapter(this.pointLearningModeAdapter);

        view.findViewById(R.id.btn_scan_lm).setOnClickListener(this);
        view.findViewById(R.id.btn_finish_lm).setOnClickListener(this);
        enableLearningMode.setOnCheckedChangeListener(this);



        return view;
    }

    private void addPoint(String pointId, GeoPoint geoPoint){
        Log.i("ZAQ!", " close 2 : " + pointId);
        if(pointConfigs.isEmpty())
        {
            PatrolPointConfig pointObj = new PatrolPointConfig(geoPoint, "not set", pointId);
            pointConfigs.add(pointObj);
        }else
        {

            isObtained(pointId, geoPoint);
        }
        displayPoints(pointConfigs);


    }

    private boolean contains(List<PatrolPointConfig> pointCol, String scanData) {
        for(int pos = 0; pos < pointCol.size() ; pos++){
            if(pointCol.get(pos).pointId.contains(scanData))
                return true;
        }

        return false;
    }
    private void close(int resultCode, Bundle extraData){
        Log.i("ZAQ!", "close: " + extraData);
        this.removePointLearningMode(resultCode, extraData);
    }
    public void isObtained(String pointId, GeoPoint geoPoint)
    {

         if(pointConfigs.get(0).pointId.equals(pointId)) {
             patrolDurationConfig.stop();

             extraData = new Bundle();
             extraData.putStringArray(DurationInfo.INTERVAL, patrolDurationConfig.getText().toString().split(":"));
             Toast.makeText(getContext(), "Patrol Ended ", Toast.LENGTH_SHORT).show();
             return;
         }else{
             if(contains(pointConfigs, pointId)){
                 Toast.makeText(getContext(), "Point Already Scanned", Toast.LENGTH_SHORT).show();
                 return;
             }else {
                 PatrolPointConfig pointObj = new PatrolPointConfig(geoPoint, "", pointId);
                 pointConfigs.add(pointObj);
             }
         }










    }
    @Override
    protected void onFragmentResult(int requestCode, int resultCode, Bundle extraData) {
        Log.i("ZAQ!", "onFragmentResult: " +requestCode);
        if(requestCode == REQ_SCAN_LEARNING_MODE){
            Log.i("ZAQ!", "onFragmentResult: 2");
            if(resultCode == Activity.RESULT_OK){
                Log.i("ZAQ!", "onFragmentResult: 3");
                String scanData = extraData.getString(POINT_CONTENT);
                Log.i("ZAQ!", "close: " + extraData);
                getDeviceCurrentLocation(scanData);
            }else if(resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(getContext(),"Scan Cancelled", Toast.LENGTH_LONG).show();
            }

        }
        super.onFragmentResult(requestCode, resultCode, extraData);
    }

    @Override
    public void onClick(View v) {

        randPoint = 1;
        switch (v.getId()){

            case R.id.btn_scan_lm:
                //start scan
                if(gIsChecked){
                    startFragmentForResult(new ScanFragmentLearningMode(), REQ_SCAN_LEARNING_MODE);
                    //getDeviceCurrentLocation("point " + randPoint++);
                }else{
                    Toast.makeText(getContext(), "Enable Learning Mode", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_finish_lm:
                //finish learning mode process all data
                if(extraData != null && pointConfigs != null ){

                    if(!pointConfigs.isEmpty() && !extraData.isEmpty())
                    {
                        Toast.makeText(getContext(), "DONE. . .", Toast.LENGTH_SHORT).show();
                        MainActivity.getAppleProjectDBServer().updatePointsValues(pointConfigs);
                        ContentValues startPointValue =  new ContentValues();
                        startPointValue.put("startEndPoint", pointConfigs.get(0).pointId);
                        MainActivity.getAppleProjectDBServer().updateEachRow("Sites",startPointValue ,MainActivity.siteId);
                        startPointValue.clear();
                        close(Activity.RESULT_OK, extraData);
                    }else {
                        Toast.makeText(getContext(), "Error 317 No points Scanned", Toast.LENGTH_SHORT).show();
                    }


                    break;
                }else{

                    Toast.makeText(getContext(), "Error 217 Patrol Not started/ended", Toast.LENGTH_SHORT).show();
                }


        }
    }

    private void displayPoints(List<PatrolPointConfig> listItems){
        if(listItems.isEmpty())
        {
            pointCount.setText("Point Scanned: 0");
            this.pointLearningModeAdapter.clear();
            return;
        }
        for(int pos = 0; pos < listItems.size(); pos++){
            listItems.get(pos).isScanned = false;
        }


        pointCount.setText("Point Scanned: "+ listItems.size());
        this.pointLearningModeAdapter.clear();
        this.pointLearningModeAdapter.addAll(listItems);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(isChecked){
            gIsChecked = isChecked;
            startPatrolChronometer();
        }else{
            if(pointConfigs != null){


                pointConfigs.clear();
                displayPoints(pointConfigs);



            }
            chronometerInterrupt("Resetting...");
        }
    }
    public void chronometerInterrupt(String msg){

        if(!patrolDurationConfig.getText().toString().isEmpty()){
            patrolDurationConfig.stop();
            patrolDurationConfig.setBase(SystemClock.elapsedRealtime());
        }
        cvDuration.setVisibility(View.GONE);
        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();

    }

    private void startPatrolChronometer(){

        cvDuration.setVisibility(View.VISIBLE);
        patrolDurationConfig.setBase(SystemClock.elapsedRealtime());
        patrolDurationConfig.start(); // start a chronometer

    }


    @Override
    public String getTitle() {
        return title;
    }
}
