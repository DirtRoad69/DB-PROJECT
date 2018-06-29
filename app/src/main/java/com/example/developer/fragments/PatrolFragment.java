package com.example.developer.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
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
import com.example.developer.fullpatrol.FirebaseManager;
import com.example.developer.fullpatrol.LinkDeviceActivity;
import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.fullpatrol.PatrolPoint;
import com.example.developer.fullpatrol.PatrolPointAdapter;
import com.example.developer.fullpatrol.R;
import com.example.developer.fullpatrol.SiteDataManager;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PatrolFragment extends KioskFragment implements View.OnClickListener {
    public static final String TAG = "PatrolFragment"
            ,TITLE = "Patrol Fragment"
            ,CONTENT = "content";

    private static final int REQ_SCAN = 123
            ,MIN_TO_MIL = 60000;

    private FirebaseManager firebaseManager;
    private  SiteDataManager siteDataManager;

    private TextView timedOut
            ,ttvDuraton
            ,ttvMsg;
    private CardView crvTimeout;
    private ListView listview;


    private PatrolPointAdapter patrolPointAdapter;
    private int panicCount;
    private Toast panicToast;

    private List<PatrolPoint> pointCol
            ,listItems;
    private PatrolPoint startingPoint;


    private CountDownTimer timePatrolDuration
            ,timeCountDown;



    public PatrolFragment(){
        this.firebaseManager = FirebaseManager.getInstance();
        this.siteDataManager = SiteDataManager.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        this.firebaseManager.setSettings(settings);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.panicCount = MainActivity.MAX_PANIC_TAPS;
        this.panicToast = Toast.makeText(this.getContext(), String.format("press panic %d more times", panicCount), Toast.LENGTH_SHORT);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.activity_display, container, false);

        timedOut = parentView.findViewById(R.id.ttv_count_down);
        ttvDuraton = parentView.findViewById(R.id.ttv_patrol_duration);
        ttvMsg = parentView.findViewById(R.id.view_content);


        listview = parentView.findViewById(R.id.listview);
        this.patrolPointAdapter = new PatrolPointAdapter(this.getActivity(), R.layout.site_item, new ArrayList<PatrolPoint>());
        listview.setAdapter(this.patrolPointAdapter);

        parentView.findViewById(R.id.btn_scan).setOnClickListener(this);
        parentView.findViewById(R.id.btn_panic).setOnClickListener(this);

        this.crvTimeout = parentView.findViewById(R.id.crv_timeout);

        setUpData();
        return parentView;
    }

    public void setUpData() {
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

        int countDown = this.siteDataManager.getLong("countDown").intValue() * MIN_TO_MIL;
        int patrolTimer = this.siteDataManager.getLong("patrolTimer").intValue() * MIN_TO_MIL;

        long durationEndStart = (AlarmReceiver.ReceiveTime + countDown) - System.currentTimeMillis();
        long durationPatrol = (AlarmReceiver.ReceiveTime + patrolTimer) - System.currentTimeMillis();

        startTimerCountDown(durationEndStart);
        startTimerPatrolDuration(durationPatrol);
        displayPoints(pointCol);

    }

    private void startTimerPatrolDuration(long duration) {
        timePatrolDuration = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;


                ((MainActivity)getActivity()).wakeUpDevice();
                String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

                ttvDuraton.setText(timeLeftFormatted);
            }

            @Override
            public void onFinish() {
                verityPatrol(listItems, pointCol, true);
            }
        }.start();
    }

    private void startTimerCountDown(long countDowning) {
        timeCountDown = new CountDownTimer(countDowning, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long mTimeLeftInMillisCountOut = millisUntilFinished;

                int minutes = (int) (mTimeLeftInMillisCountOut / 1000) / 60;
                int seconds = (int) (mTimeLeftInMillisCountOut / 1000) % 60;

                String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                timedOut.setText(timeLeftFormatted);
            }

            @Override
            public void onFinish() {

                ((MainActivity)getActivity()).setDeviceSleep();
                crvTimeout.setVisibility(View.GONE);
                firebaseManager.sendEventType(MainActivity.eventsCollection, "No Points Visited", 22, "");
                timePatrolDuration.cancel();
                close();
                Toast.makeText(getActivity(), "Finished!", Toast.LENGTH_LONG).show();
            }
        }.start();


    }

    @Override
    protected void onFragmentResult(int requestCode, int resultCode, Bundle extraData) {
        if(requestCode == REQ_SCAN){
            if(resultCode == Activity.RESULT_OK){
                String scanData = extraData.getString(CONTENT);

                if(contains(pointCol, scanData)){
                    if( (contains(listItems, scanData) && !startingPoint.pointId.equals(scanData))){
                        Toast.makeText(getContext(), "Point Already scanned", Toast.LENGTH_LONG).show();
                    }else{
                        listItems.add(get(pointCol,scanData));
                        patrol();
                        if(listItems.size()>1){
                            this.firebaseManager.sendEventType(MainActivity.eventsCollection,"",scanData, 0, "");
                            Toast.makeText(getContext(),"Point scanned", Toast.LENGTH_LONG).show();
                        }
                    }

                }else{
                    Toast.makeText(getContext(), "Point not from site", Toast.LENGTH_LONG).show();
                }

                verifyPatrolVisually(listItems, pointCol);

                displayPoints(listItems, pointCol);

            }else if(resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(getContext(),"Scan Cancelled", Toast.LENGTH_LONG).show();
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
                timePatrolDuration.cancel();
                timeCountDown.cancel();
                verityPatrol(listItems, pointCol, false);
            }
        }
    }

    public void checkStartPatrol(){
        if(listItems.contains(startingPoint) && listItems.size() == 1){
            crvTimeout.setVisibility(View.GONE);
            timeCountDown.cancel();
            timedOut.setText("");
            this.firebaseManager.sendEventType(MainActivity.eventsCollection, "Patrol started", 3,"");
            Toast.makeText(getContext(), "Patrol started", Toast.LENGTH_LONG).show();
        }else if(listItems.size() == 1){
            Toast.makeText(getContext(), " Not starting point ", Toast.LENGTH_LONG).show();
            listItems.clear(); //empty non start points
        }
    }

    private void close(){

        ((MainActivity)getActivity()).setDeviceSleep();
        PatrolFragment.this.removeSelf();
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

            this.firebaseManager.sendEventType(MainActivity.eventsCollection,"Missed point(s)", 4, "");
            Toast.makeText(this.getActivity(), String.format("%d points missing",tot), Toast.LENGTH_LONG).show();
            close();
            //}
        }else if(!isFinished && listItems.get(listItems.size()-1).pointId.equals(startingPoint.pointId)){
            //send good patrol eventType 7
            this.firebaseManager.sendEventType(MainActivity.eventsCollection,"Good patrol", 7, "");
            close();

        } else{
            this.firebaseManager.sendEventType(MainActivity.eventsCollection,"Patrol not ended", 68, "");
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

    private boolean contains(List<PatrolPoint> pointCol, String scanData) {
        for(int pos = 0; pos < pointCol.size() ; pos++){
            if(pointCol.get(pos).pointId.contains(scanData))
                return true;
        }

        return false;
    }

    private void displayPoints(List<PatrolPoint> scanned, List<PatrolPoint> all){
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

    private void displayPoints(List<PatrolPoint> listItems){
        for(int pos = 0; pos < listItems.size(); pos++){
            listItems.get(pos).isScanned = false;
        }

        this.patrolPointAdapter.clear();
        this.patrolPointAdapter.addAll(listItems);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_scan:
                PatrolFragment.this.startFragmentForResult(new ScanFragment(), REQ_SCAN);
                break;
            case R.id.btn_panic:
                onPanic();
                break;
        }
    }

    public void onPanic(){
        if(panicCount == MainActivity.MAX_PANIC_TAPS){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    panicCount = MainActivity.MAX_PANIC_TAPS;
                }
            }, MainActivity.PANIC_REST_DURATION);
        }

        panicCount--;

        if(panicCount == 0){
            String siteId = getContext().getSharedPreferences(getContext().getPackageName(), Context.MODE_PRIVATE).getString(LinkDeviceActivity.PREF_LINKED_SITE, null);
            if(siteId != null){
                this.firebaseManager.sendEventType(MainActivity.eventsCollection, MainActivity.PANIC_EVENT_DESCRIPTION, MainActivity.PANIC_EVENT_ID, siteId);
                panicCount = MainActivity.MAX_PANIC_TAPS;
                panicToast.setText("Panic Message Sent.");
                panicToast.show();
            }
        }else{
            panicToast.setText(String.format("press panic %d more times", panicCount));
            panicToast.show();
        }
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

}