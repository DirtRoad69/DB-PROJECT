package com.example.developer.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.developer.fullpatrol.AlarmReceiver;
import com.example.developer.fullpatrol.FirebaseManager;
import com.example.developer.fullpatrol.Interpol;
import com.example.developer.fullpatrol.LinkDeviceActivity;
import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.objects.PatrolPoint;
import com.example.developer.fullpatrol.PatrolPointAdapter;
import com.example.developer.fullpatrol.R;
import com.example.developer.fullpatrol.SiteDataManager;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class PatrolFragment extends KioskFragment implements View.OnClickListener {
    public static final String TAG = "ZAQ@"
            ,TITLE = "Patrol Fragment"
            ,CONTENT = "content";

    private static final int REQ_SCAN = 123
            ,MIN_TO_MIL = 60000;
    private static long PATROL_DURATION;


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
    private long timePatrolEnded;
    private long MIN_TIME;
    private long MAX_TIME;
    long durationPatrol;
    int patrolTimer;

    //added
    private boolean isPlayed;
    private Chronometer patrolDurationConfig;
    private boolean isChrStarted;
    private long durationPatrolChr;
    private boolean guardOnPatrol;

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

        guardOnPatrol = true;
        MainActivity.wakeActive = false;
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //((MainActivity)getActivity()).wakeUpDevice();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.activity_display, container, false);

        timedOut = parentView.findViewById(R.id.ttv_count_down);
        //ttvDuraton = parentView.findViewById(R.id.ttv_patrol_duration);
        ttvMsg = parentView.findViewById(R.id.view_content);

        //added
        patrolDurationConfig = parentView.findViewById(R.id.chr_patrol_duration_temp);
        isChrStarted = false;


        listview = parentView.findViewById(R.id.listview);
        this.patrolPointAdapter = new PatrolPointAdapter(this.getActivity(), R.layout.site_item, new ArrayList<PatrolPoint>());
        listview.setAdapter(this.patrolPointAdapter);

        parentView.findViewById(R.id.btn_scan).setOnClickListener(this);
        parentView.findViewById(R.id.btn_panic).setOnClickListener(this);

        this.crvTimeout = parentView.findViewById(R.id.crv_timeout);

        setUpData();
        return parentView;
    }

    private String getPointDescription(String pointId){
        String pointDescription = "";
        for(int i = 0; i < pointCol.size();  i++){
            if(pointCol.get(i).pointId.contains(pointId)){
                pointDescription = pointCol.get(i).pointDescription;
                if(pointDescription.isEmpty()){
                    pointDescription = pointId;
                }
            }
        }
        return pointDescription;
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

        int countDown = this.siteDataManager.getLong("startDelay").intValue() * MIN_TO_MIL;
        int newCountDown = 2*MIN_TO_MIL;
        patrolTimer = this.siteDataManager.getLong("maxTime").intValue() * MIN_TO_MIL;

        MAX_TIME = this.siteDataManager.getLong("maxTime") * MIN_TO_MIL;
        MIN_TIME = this.siteDataManager.getLong("minTime") * MIN_TO_MIL;



        long durationEndStart = (AlarmReceiver.ReceiveTime + newCountDown) - System.currentTimeMillis();
        durationPatrol = (AlarmReceiver.ReceiveTime + patrolTimer) - System.currentTimeMillis();
        durationPatrol = (AlarmReceiver.ReceiveTime + patrolTimer) - System.currentTimeMillis();
        durationPatrolChr = (AlarmReceiver.ReceiveTime) - System.currentTimeMillis();

        startTimerCountDown(durationEndStart+1000);
        patrolDurationChronometer();
        startTimerPatrolDuration(getTimeEndPatrol());

        displayPoints(pointCol);

    }

    private long getTimeEndPatrol(){


        int startHour, startMin, intervalTimer;
        startHour = siteDataManager.getInt("startHour");
        startMin = siteDataManager.getInt("startMin");
        intervalTimer = siteDataManager.getLong("intervalTimer").intValue();
        // Set the alarm to start at 8:30 a.m.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, startHour);
        calendar.set(Calendar.MINUTE, startMin);
        calendar.set(Calendar.SECOND, 0);
        long nxtTime =  Interpol.getNextTimePatrol(calendar.getTimeInMillis(), 1000 * 60 * intervalTimer);
        long diff = nxtTime - System.currentTimeMillis() - 4000;
        //long duration = (long) ((diff) / 60000.0);
        Log.i("QAZ", "getTimeEndPatrol: "+diff);


        PATROL_DURATION = diff;

        return diff;

    }
    private void chronometerInterrupt(String msg){

        if(!patrolDurationConfig.getText().toString().isEmpty()){
            patrolDurationConfig.stop();
            isChrStarted = false;
            Log.i("QAZ", "onDestroy: WAS CALLED" );
        }

        Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();

    }
    private void patrolDurationChronometer(){

        if(!isChrStarted){
            patrolDurationConfig.setBase(SystemClock.elapsedRealtime() + durationPatrolChr);
            patrolDurationConfig.start(); // start a chronometer

            Log.i(TAG, "patrolDurationChronometer: " + patrolDurationConfig.getText());
            Toast.makeText(getContext(), patrolDurationConfig.getText(), Toast.LENGTH_SHORT).show();
            isChrStarted = true;
        }


    }



    private void startTimerPatrolDuration(long duration) {
        timePatrolDuration = new CountDownTimer(duration, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {


                timePatrolEnded = millisUntilFinished;
                Log.i("WSX", "onTick: "+timePatrolEnded);
                timePatrolEnded = Math.abs(timePatrolEnded - PATROL_DURATION);
                if(guardOnPatrol &&  timePatrolEnded > MAX_TIME){
                    guardOnPatrol = false;
                    Log.i("WSX", "missing guard: "+millisUntilFinished);
                    firebaseManager.sendEventType(MainActivity.eventsCollection, "Missing Guard", 6, "");
                }

            }

            @Override
            public void onFinish() {
                if(timeCountDown != null){
                    closeVoice();
                    timeCountDown.cancel();
                }
                verityPatrol(listItems, pointCol, true);

            }
        }.start();
    }

    private void startTimerCountDown(long countDowning) {
        timeCountDown = new CountDownTimer(countDowning, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long mTimeLeftInMillisCountOut = millisUntilFinished;
                try{
                    if(timeCountDown != null){
                        ((MainActivity)getActivity()).textToSpeech(getString(R.string.scan_point), getContext());
                    }
                }catch (Exception e){Log.i("WSX", e.getMessage());}
                int minutes = (int) (mTimeLeftInMillisCountOut / 1000) / 60;
                int seconds = (int) (mTimeLeftInMillisCountOut / 1000) % 60;

                String timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
                timedOut.setText(timeLeftFormatted);
            }

            @Override
            public void onFinish() {
                closeVoice();
               // ((MainActivity)getActivity()).setDeviceSleep();
                crvTimeout.setVisibility(View.GONE);

                firebaseManager.sendEventType(MainActivity.eventsCollection, "Failed To Start Patrol", 15, "");
                timePatrolDuration.cancel();
                if(patrolDurationConfig != null)
                    patrolDurationConfig.stop();
                close();
                Toast.makeText(getActivity(), "Finished!", Toast.LENGTH_LONG).show();
            }
        }.start();


    }
    private void closeVoice(){
        if(MainActivity.speakClass != null){
            MainActivity.speakClass.stop();
            MainActivity.speakClass.shutdown();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("WSX", "onDestroy: called");
        if(timeCountDown !=null)
            //closeVoice();
            timeCountDown.cancel();
        if(timePatrolDuration !=null)
            timePatrolDuration.cancel();
    }

    @Override
    protected void onFragmentResult(int requestCode, int resultCode, Bundle extraData) {
            if(requestCode == REQ_SCAN){
                if(resultCode == Activity.RESULT_OK){
                    String scanData = extraData.getString(CONTENT);

                    if(contains(pointCol, scanData)){
                        if( (contains(listItems, scanData) && !startingPoint.pointId.equals(scanData))){
                            Toast.makeText(getContext(), "Point Already scanned", Toast.LENGTH_LONG).show();
                            try{
                                ((MainActivity)getActivity()).textToSpeech(getString(R.string.point_already_scanned), getContext());
                            }catch (Exception e){
                                Log.i("WSX", "patrol: "+e.getMessage());
                            }

                        }else{
                            listItems.add(get(pointCol,scanData));
                            if(listItems.size()>1){
                                if(!startingPoint.pointId.contains(scanData))
                                    this.firebaseManager.sendEventType(MainActivity.eventsCollection,getPointDescription(scanData),scanData, 0, "");
                                Toast.makeText(getContext(),getPointDescription(scanData), Toast.LENGTH_LONG).show();
                                try{
                                    ((MainActivity)getActivity()).textToSpeech(getString(R.string.point_scanned), getContext());
                                }catch (Exception e){
                                    Log.i("WSX", "patrol: "+e.getMessage());
                                }
                            }
                            patrol();


                        }

                if(contains(pointCol, scanData)){
                    if( (contains(listItems, scanData) && !startingPoint.pointId.equals(scanData))){
                        Toast.makeText(getContext(), "Point Already scanned", Toast.LENGTH_LONG).show();
                    }else{
                        Toast.makeText(getContext(), "Point not from site", Toast.LENGTH_LONG).show();
                        try{
                            ((MainActivity)getActivity()).textToSpeech(getString(R.string.not_site_point), getContext());
                        }catch (Exception e){
                            Log.i("WSX", "patrol: "+e.getMessage());
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
                verityPatrol(listItems, pointCol, false);
                try{
                    ((MainActivity)getActivity()).textToSpeech(getString(R.string.patrol_completed), (getActivity().getApplicationContext()));
                }catch (Exception e){
                    Log.i("WSX", "patrol: "+e.getMessage());
                }

                timePatrolDuration.cancel();
                timeCountDown.cancel();

            }
        }
    }

    private void patrolTimeToComplete() {
        Log.i("WSX", "min: "+MIN_TIME);
        Log.i("WSX", "max: "+MAX_TIME);

        Log.i("WSX", "timePatrolEnded: "+timePatrolEnded);
        //timePatrolEnded = Math.abs(timePatrolEnded - PATROL_DURATION);
        Log.i("WSX", "timePatrolEnded: "+timePatrolEnded);
        if(timePatrolEnded >= MAX_TIME){
            //patrol too quick
            Log.i("WSX", "timePatrolEnded max: "+timePatrolEnded);

            this.firebaseManager.sendEventType(MainActivity.eventsCollection,"Guard Returned Late", 17, "");


            try{
                ((MainActivity)getActivity()).textToSpeech(getString(R.string.max_patrol_time),(getActivity().getApplicationContext()));
            }catch (Exception e){
                Log.i("WSX", "patrol: "+e.getMessage());
            }
        }else if(timePatrolEnded <= MIN_TIME ){
            //patrol to fast
            Log.i("WSX", "timePatrolEnded min: "+timePatrolEnded);

            this.firebaseManager.sendEventType(MainActivity.eventsCollection,"Patrolled Too Quickly", 5, "");
            try{
                ((MainActivity)getActivity()).textToSpeech(getString(R.string.min_patrol_time), (getActivity().getApplicationContext()));
            }catch (Exception e){
                Log.i("WSX", "patrol: "+e.getMessage());
            }

        }


    }

    public void checkStartPatrol(){
        if(listItems.contains(startingPoint) && listItems.size() == 1){
            crvTimeout.setVisibility(View.GONE);
            timeCountDown.cancel();
            timedOut.setText("");
            this.firebaseManager.sendEventType(MainActivity.eventsCollection, "Patrol started", 3,"");
            this.firebaseManager.sendEventType(MainActivity.eventsCollection,startingPoint.pointDescription,startingPoint.pointId, 0, "");
            Toast.makeText(getContext(), "Patrol started", Toast.LENGTH_LONG).show();

            try{
                ((MainActivity)getActivity()).textToSpeech(getString(R.string.patrol_started), getContext());
            }catch (Exception e){
                Log.i("WSX", "patrol: "+e.getMessage());
            }

        }else if(listItems.size() == 1){
            Toast.makeText(getContext(), "Not starting point ", Toast.LENGTH_LONG).show();
            try{
                ((MainActivity)getActivity()).textToSpeech(getString(R.string.not_starting_point), getContext());
            }catch(Exception e){
                Log.i("WSX", "checkStartPatrol: "+e.getMessage());
            }
            listItems.clear(); //empty non start points
        }
    }

    private void close(){
        try {
            //closeVoice();
            MainActivity.wakeActive = false;
            ((MainActivity)getActivity()).setScreenSleep();
        }catch (Exception e){
            //Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        PatrolFragment.this.removeSelf();
    }

    private void verifyPatrolVisually(List<PatrolPoint> scannedPoints, List<PatrolPoint> pointCollection){

        List<PatrolPoint> missedPoints = new ArrayList<>();

        Log.i("ZAQ@", pointCollection.size()+" verifyPatrolVisually: "+pointCol);
        Log.i("ZAQ@", scannedPoints.size()+" countList verifyPatrolVisually: list"+listItems);

        //loop over scanned points and check if all
        for(int i=0; i < pointCollection.size();i++){
            if(!scannedPoints.contains(pointCollection.get(i))){
                //add this
                missedPoints.add(pointCollection.get(i));

            }
        }

        int tot =  missedPoints.size();
        ttvMsg.setText(Integer.toString(tot) + " Points Remaining");
        try{
            ((MainActivity)getActivity()).textToSpeech(Integer.toString(tot) + " Points Remaining", getContext());
        }catch (Exception e){
            Log.i("WSX", "patrol: "+e.getMessage());
        }


        if(tot>0){
            String allPoints ="\tMissed Points\n";
            //display missed points
            for(int i =0; i< missedPoints.size(); i++){
                allPoints += missedPoints.get(i)+"\n";
            }
            //ttvMsg.setText(allPoints);

        }else{
            String allPoints ="All points scanned!\n";
            try{
                ((MainActivity)getActivity()).textToSpeech("All points scanned please end patrol, All points scanned please end patrol, All points scanned please end patrol", getContext());
            }catch (Exception e){
                Log.i("WSX", "patrol: "+e.getMessage());
            }
            ttvMsg.setText(allPoints);
        }
        if(tot==0 && listItems.get(listItems.size()-1).pointId.equals(startingPoint.pointId)){
            Toast.makeText(this.getActivity(), "Good Patrol ", Toast.LENGTH_LONG).show();
            try{
                ((MainActivity)getActivity()).textToSpeech("Good Patrol", this.getActivity().getApplicationContext());
            }catch (Exception e){
                Log.i("WSX", "patrol: "+e.getMessage());
            }
        }
    }

    private void verityPatrol(List<PatrolPoint> scannedPoints, List<PatrolPoint> pointCollection, boolean isFinished) {
        Log.i("WSX", "verityPatrol: 789");
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
            try{
                ((MainActivity)getActivity()).textToSpeech(String.format("%d points missing",tot),(getActivity().getApplicationContext()));
            }catch (Exception e){
                Log.i("WSX", "patrol: "+e.getMessage());
            }
            close();
            //}
        }else if(!isFinished && listItems.get(listItems.size()-1).pointId.equals(startingPoint.pointId)){
            //send good patrol eventType 7

            patrolTimeToComplete();
            this.firebaseManager.sendEventType(MainActivity.eventsCollection,"Good patrol", 7, "");
            this.firebaseManager.sendEventType(MainActivity.eventsCollection,"Patrol Ended", 66, "");
            Toast.makeText(getContext(), " --Patrol ended-- ", Toast.LENGTH_LONG).show();
            ((MainActivity)getActivity()).textToSpeech("Good Patrol", this.getActivity().getApplicationContext());
            close();

        } else{
            this.firebaseManager.sendEventType(MainActivity.eventsCollection,"Patrol not ended", 68, "");
            try{
                ((MainActivity)getActivity()).textToSpeech("Patrol not ended",(getActivity().getApplicationContext()));
            }catch (Exception e){
                Log.i("WSX", "patrol: "+e.getMessage());
            }
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