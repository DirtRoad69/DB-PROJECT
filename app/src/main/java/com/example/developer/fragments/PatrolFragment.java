package com.example.developer.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.developer.fullpatrol.AlarmReceiver;
import com.example.developer.fullpatrol.Display;
import com.example.developer.fullpatrol.FirebaseManager;
import com.example.developer.fullpatrol.Interpol;
import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.fullpatrol.PatrolPoint;
import com.example.developer.fullpatrol.PatrolPointAdapter;
import com.example.developer.fullpatrol.R;
import com.example.developer.fullpatrol.ScannerActivity;
import com.example.developer.fullpatrol.SiteDataManager;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PatrolFragment extends KioskFragment {
    private static final String TAG = "dcf";
    public static final String TITLE = "Patrol Fragment";
    private static final int REQ_SCAN = 123;
    public static final String CONTENT = "content";

    private FirebaseManager firebaseManager;
    private  SiteDataManager siteDataManager;
    private TextView timedOut, ttvDuraton, ttvMsg;
    private CardView crvTimeout;
    private ListView listview;
    private PatrolPointAdapter patrolPointAdapter;
    private Button btnScan;
    private String collection;
    private int count;
    private List<PatrolPoint> pointCol;
    private PatrolPoint startingPoint;
    private int intervalTimer;
    private int countDown;
    private int patrolTimer;
    private int PATROLTIMER;
    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private CountDownTimer countDownOut;
    private static final int MIN_TO_MIL = 60000;

    private boolean isScanning;

    private List<PatrolPoint> listItems;


    public PatrolFragment(){
        this.firebaseManager = FirebaseManager.getInstance();
        this.siteDataManager = SiteDataManager.getInstance();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: ");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        isScanning = false;
        View parentView = inflater.inflate(R.layout.activity_display, container, false);

        timedOut = parentView.findViewById(R.id.ttv_count_down);
        ttvDuraton = parentView.findViewById(R.id.ttv_patrol_duration);
        ttvMsg = parentView.findViewById(R.id.view_content);


        listview = parentView.findViewById(R.id.listview);
        this.patrolPointAdapter = new PatrolPointAdapter(this.getActivity(), R.layout.site_item, new ArrayList<PatrolPoint>());
        listview.setAdapter(this.patrolPointAdapter);

        btnScan = parentView.findViewById(R.id.btn_scan);


        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isScanning = true;
                PatrolFragment.this.startFragmentForResult(ScanFragment.TITLE, REQ_SCAN);
            }
        });

        this.crvTimeout = parentView.findViewById(R.id.crv_timeout);

        collection = "patrolDataDummy";
        count = 5;
        Toast.makeText(this.getActivity(), "Created Display", Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onCreateView: ");
        getPatrolData();
        return parentView;
    }

    @Override
    public void onStart() {
        super.onStart();


        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        this.firebaseManager.setSettings(settings);


        Log.i(TAG, "onStart: ");

    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.i(TAG, "setUserVisibleHint: " + isVisibleToUser);
        if(isVisibleToUser){
            getPatrolData();

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
    }

    public void getPatrolData() {
        pointCol = (List<PatrolPoint>)this.siteDataManager.get("patrolPoints");
        for(int pos = 0; pos < pointCol.size(); pos++){
            PatrolPoint cPoint = pointCol.get(pos);
            cPoint.isScanned = false;
            if(cPoint.isStarting){
                startingPoint = cPoint;
                break;
            }
        }
        listItems = new ArrayList<>();

        intervalTimer = this.siteDataManager.getLong("intervalTimer").intValue();
        countDown = this.siteDataManager.getLong("countDown").intValue() * MIN_TO_MIL;
        patrolTimer = this.siteDataManager.getLong("patrolTimer").intValue() * MIN_TO_MIL;
        PATROLTIMER = patrolTimer;

        long durationEndStart = (AlarmReceiver.ReceiveTime + countDown) - System.currentTimeMillis();
        long durationPatrol = (AlarmReceiver.ReceiveTime + patrolTimer) - System.currentTimeMillis();
        startTimer(durationEndStart);
        startTimerStop(durationPatrol);
        displayMissedPoints(pointCol);
        //for debugging purposes
        // Log.i("RFC1", startingPoint.pointDescription + "|[" + startingPoint.pointId );
        //ttvMsg.setText(pointCol[4] + "\npatrolTimer :" + Integer.toString(patrolTimer * 60) + "\ncountDown :" + Integer.toString(countDown * 60));

    }

    private void startTimer(long duration) {
        mCountDownTimer = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

                updateCountDownText(millisUntilFinished);

            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                verityPatrol(listItems, pointCol, true);


            }
        }.start();

        mTimerRunning = true;
    }

    private void updateCountDownText(long millisUntilFinished) {
        int minutes = (int) (millisUntilFinished / 1000) / 60;
        int seconds = (int) (millisUntilFinished / 1000) % 60;

        String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        ttvDuraton.setText(timeLeftFormatted);
    }

    @Override
    protected void proccessCommand(String command) {

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.i(TAG, "onActivityCreated: ");
    }

    @Override
    public void onFragmentReturn(int requestCode, int resultCode, Intent data) {
        super.onFragmentReturn(requestCode, resultCode, data);
        Log.i(TAG, "onFragmentReturn: 0" + requestCode + "|" + requestCode + "|" + data);
        if(requestCode ==  REQ_SCAN){
            isScanning = false;
            Log.i(TAG, "onFragmentReturn: 1");
            if(resultCode == Activity.RESULT_OK){
                Log.i(TAG, "onFragmentReturn: 2");
                Bundle bundle = data.getExtras();
                String scanData = bundle.getString(CONTENT);
                //String content_ = bundle.getString(CONTENT);
                Log.d("POINTS", "scanned point "+scanData);

                if(contains(pointCol, scanData)){
                    if( (contains(listItems, scanData) && !startingPoint.pointId.equals(scanData))){
                        Toast.makeText(getContext(), "Point Already scanned", Toast.LENGTH_LONG).show();
                    }else{

                        listItems.add(get(pointCol,scanData));
                        patrol();
                        if(listItems.size()>1){
                            //create scanned point event
                            this.firebaseManager.sendEventType(collection,"",scanData, 0, "");
                            Toast.makeText(getContext(),"Point scanned", Toast.LENGTH_LONG).show();
                        }
                    }

                }else{
                    Toast.makeText(getContext(), "Point not from site", Toast.LENGTH_LONG).show();
                }

                verifyPatrolVisually(listItems, pointCol);

                sort(listItems, pointCol);

            }else if(resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(getContext(),"BACK PRESSED", Toast.LENGTH_LONG).show();
            }
        }

    }

    public void patrol(){
        //check if patrol started
        checkStartPatrol();
        //check if patrol ended
        if(listItems.size() > 1){
            if (listItems.get(listItems.size()-1).pointId.equals(startingPoint.pointId)){
                Toast.makeText(getContext(), " --Patrol ended-- ", Toast.LENGTH_LONG).show();
                mCountDownTimer.cancel();
                verityPatrol(listItems, pointCol, false);
            }
        }
    }

    public void checkStartPatrol(){
        if(listItems.contains(startingPoint) && listItems.size() == 1){
            crvTimeout.setVisibility(View.GONE);
            countDownOut.cancel();
            timedOut.setText("");
            this.firebaseManager.sendEventType(collection, "Patrol started", 3,"");
            Toast.makeText(getContext(), "Patrol started", Toast.LENGTH_LONG).show();
        }else if(listItems.size() == 1){

            Toast.makeText(getContext(), " Not starting point ", Toast.LENGTH_LONG).show();
            listItems.clear(); //empty non start points
        }
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    public void displayMissedPoints(List<PatrolPoint> listItems){
        for(int pos = 0; pos < listItems.size(); pos++){
            listItems.get(pos).isScanned = false;
            Log.i("RFHC",   listItems.get(pos).pointId +"|" +listItems.get(0).pointDescription + "|" + listItems.get(0).location.toString());
        }
        this.patrolPointAdapter.clear();
        this.patrolPointAdapter.addAll(listItems);
    }

    private void close(){
        //this.sendBroadcast(new Intent(BROD_KILL_SCANNER));
        //Interpol.getInstance().setOutOfMainActivity(false);
        //this.Unlock();
        if(isScanning){
            this.closeFragment(ScanFragment.TITLE);
        }
        PatrolFragment.this.returnToFragment();

        Log.i(TAG, "close: " +DutyFragment.TITLE);
    }

    private void verifyPatrolVisually(List<PatrolPoint> scannedPoints, List<PatrolPoint> pointCollection){

        List<PatrolPoint> missedPoints = new ArrayList<>();

        //loop over scanned points and check if all
        for(int i=0; i < pointCollection.size();i++){
            if(!scannedPoints.contains(pointCollection.get(i))){
                //add this
                missedPoints.add(pointCollection.get(i));

            }
        }

        int tot =  missedPoints.size();
        ttvMsg.setText(Integer.toString(tot) + " Points Remaining");

        if(tot>0){
            String allPoints ="\tMissed Points\n";
            //display missed points
            for(int i =0; i< missedPoints.size(); i++){
                allPoints += missedPoints.get(i)+"\n";
            }
            //ttvMsg.setText(allPoints);

        }else{
            String allPoints ="All points scanned!\n";
            ttvMsg.setText(allPoints);
        }
        if(tot==0 && listItems.get(listItems.size()-1).pointId.equals(startingPoint.pointId)){

            Toast.makeText(this.getActivity(), "Good Patrol ", Toast.LENGTH_LONG).show();
        }

    }
    private void verityPatrol(List<PatrolPoint> scannedPoints, List<PatrolPoint> pointCollection, boolean isFinished) {

        //array of missed points
        List<PatrolPoint> missedPoints = new ArrayList<>();

        //loop over scanned points and check if all
        for(int i=0; i < pointCollection.size();i++){
            if(!scannedPoints.contains(pointCollection.get(i))){
                //add this
                missedPoints.add(pointCollection.get(i));

            }
        }
        int tot =  missedPoints.size();




        if(tot>0){

            this.firebaseManager.sendEventType(collection,"Missed point(s)", 4, "");
            Toast.makeText(this.getActivity(), String.format("%d points missing",tot), Toast.LENGTH_LONG).show();
            close();
            //}
        }else if(!isFinished && listItems.get(listItems.size()-1).pointId.equals(startingPoint.pointId)){
            //send good patrol eventType 7
            this.firebaseManager.sendEventType(collection,"Good patrol", 7, "");
            close();

        } else{
            this.firebaseManager.sendEventType(collection,"Patrol not ended", 68, "");
            close();
        }
    }

    private PatrolPoint get(List<PatrolPoint> pointCol, String scanData) {
        for(int pos = 0; pos < pointCol.size() ; pos++){
            if(pointCol.get(pos).pointId.contains(scanData))
                return pointCol.get(pos);
        }
        return null;
    }


    private void startTimerStop(long countDowning) {
        countDownOut = new CountDownTimer(countDowning, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long mTimeLeftInMillisCountOut = millisUntilFinished;


                int minutes = (int) (mTimeLeftInMillisCountOut / 1000) / 60;
                int seconds = (int) (mTimeLeftInMillisCountOut / 1000) % 60;


                String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                timedOut.setText(timeLeftFormatted);//.setText("Time remaining: \n\n  "+timeLeftFormatted +"\n"+ "");


            }

            @Override
            public void onFinish() {
                crvTimeout.setVisibility(View.GONE);
                firebaseManager.sendEventType(collection, "No Points Visited", 22, "");
                mCountDownTimer.cancel();
                close();
                Toast.makeText(getActivity(), "Finished!", Toast.LENGTH_LONG).show();

            }
        }.start();


    }

    private boolean contains(List<PatrolPoint> pointCol, String scanData) {
        for(int pos = 0; pos < pointCol.size() ; pos++){
            if(pointCol.get(pos).pointId.contains(scanData))
                return true;
        }

        return false;
    }

    private void sort(List<PatrolPoint> scanned, List<PatrolPoint> all){
        List<PatrolPoint> items = new ArrayList<>();
        for(int pos = 0; pos <scanned.size(); pos++){

            scanned.get(pos).isScanned = true;
            items.add(scanned.get(pos));
        }

        for(int pos = 0; pos < all.size(); pos++){
            if(!scanned.contains(all.get(pos))){

                all.get(pos).isScanned = false;
                items.add(all.get(pos));
            }
        }

        this.patrolPointAdapter.clear();
        this.patrolPointAdapter.addAll(items);
    }



}