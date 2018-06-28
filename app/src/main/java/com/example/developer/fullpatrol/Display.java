package com.example.developer.fullpatrol;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Display extends LockableActivity {
    public static final String BROD_KILL_SCANNER = "Broadcast.kill.Scanner";


    private TextView ttvDuraton;


    private static final long START_TIME_IN_MILLIS = 7*60000;
    private TextView mTextViewCountDown;
    private CountDownTimer mCountDownTimer;
    private boolean mTimerRunning;
    private Button startPatrol;
    private long PATROLTIMER = START_TIME_IN_MILLIS;

    public String timeLeftFormatted;
    public ListView listview;

    private List<PatrolPoint> listItems = new ArrayList<PatrolPoint>();

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;

    //RECORDING HOW MANY TIMES THE BUTTON HAS BEEN CLICKED

    FirebaseFirestore db = FirebaseFirestore.getInstance();
    Button btnScan;


    //stuff from main
    private final static int REQUEST_SCANNER = 1;
    public final static String FORMAT = "format";
    public final static String CONTENT = "content";
    public final static String LOCATION = "location";
    public final static String TIMESTAMP = "time";
    private static final String TAG = "Firestore--you" ;
    TextView timedOut;
    TextView ttvMsg;
    private List<PatrolPoint> pointCol;

    private DocumentReference docRef = db.collection("site").document("jfGtB20apFAHdEYsgb8H");
    private Map<String, Object> setUp = new HashMap<>();
    private int intervalTimer;
    private int patrolTimer;
    private int countDown;
    Context context;
    private String collection;
    private static final int MIN_TO_MIL = 60000;
    private int count;
    private CountDownTimer countDownOut;
    private CardView crvTimeout;
    private PatrolPointAdapter patrolPointAdapter;
    private PatrolPoint startingPoint;


    //Managers
    FirebaseManager firebaseManager;
    SiteDataManager siteDataManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.Lock();
        setContentView(R.layout.activity_display);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.firebaseManager = FirebaseManager.getInstance();
        this.siteDataManager = SiteDataManager.getInstance();




        timedOut = findViewById(R.id.ttv_count_down);
        ttvDuraton = findViewById(R.id.ttv_patrol_duration);
        ttvMsg = findViewById(R.id.view_content);


        listview = findViewById(R.id.listview);
        this.patrolPointAdapter = new PatrolPointAdapter(this, R.layout.site_item, new ArrayList<PatrolPoint>());
        listview.setAdapter(this.patrolPointAdapter);

        btnScan = findViewById(R.id.btn_scan);

        this.crvTimeout = this.findViewById(R.id.crv_timeout);

        collection = "patrolDataDummy";
        context =this;
        count = 5;
        Toast.makeText(context, "Created Display", Toast.LENGTH_SHORT).show();

        getPatrolData();


        //START_TIME_IN_MILLIS = patrolTimer*60000;
       //





        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        this.firebaseManager.setSettings(settings);




        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Display.this.Unlock();
                Intent intent = new Intent(Display.this, ScannerActivity.class);
                startActivityForResult(intent, REQUEST_SCANNER);
            }
        });


        //displayMissedPoints(listItems);

       //startTimer();
       //startTimerStop(countDown);

    }

    @Override
    protected void onPause() {
        Log.i("RFC", "pause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.i("RFC", "Destroy");

        super.onDestroy();
    }

    public void checkStartPatrol(){
        if(listItems.contains(startingPoint) && listItems.size() == 1){
            crvTimeout.setVisibility(View.GONE);
            countDownOut.cancel();
            timedOut.setText("");
            this.firebaseManager.sendEventType(collection, "Patrol started", 3,"");
            Toast.makeText(getApplicationContext(), "Patrol started", Toast.LENGTH_LONG).show();
        }else if(listItems.size() == 1){

            Toast.makeText(context, " Not starting point ", Toast.LENGTH_LONG).show();
            listItems.clear(); //empty non start points
        }
    }
    public void patrol(){
        //check if patrol started
        checkStartPatrol();
        //check if patrol ended
        if(listItems.size() > 1){
            if (listItems.get(listItems.size()-1).pointId.equals(startingPoint.pointId)){
                Toast.makeText(context, " --Patrol ended-- ", Toast.LENGTH_LONG).show();
                mCountDownTimer.cancel();
                verityPatrol(listItems, pointCol, false);
            }
        }
    }

    public void pressPanic(View v){
        count--;
        Log.d(TAG, "pressed Panic "+Integer.toString(count));
        if(count==0){
            this.firebaseManager.sendEventType(collection,"Panic", 8, "");
            count = 5;
        }else{
            Toast.makeText(getApplicationContext(), String.format("press panic %d more times",count), Toast.LENGTH_SHORT).show();
        }
    }
    public void getPatrolData() {
        pointCol = (List<PatrolPoint>)this.siteDataManager.get("patrolPoints");
        for(int pos = 0; pos < pointCol.size(); pos++){
            PatrolPoint cPoint = pointCol.get(pos);
            if(cPoint.isStarting){
                startingPoint = cPoint;
                break;
            }
        }
        intervalTimer = this.siteDataManager.getLong("intervalTimer").intValue();
        countDown = this.siteDataManager.getLong("countDown").intValue() * MIN_TO_MIL;
        patrolTimer = this.siteDataManager.getLong("patrolTimer").intValue() * MIN_TO_MIL;
        PATROLTIMER = patrolTimer;
        startTimer();
        startTimerStop(countDown);
        displayMissedPoints(pointCol);
        //for debugging purposes
       // Log.i("RFC1", startingPoint.pointDescription + "|[" + startingPoint.pointId );
        //ttvMsg.setText(pointCol[4] + "\npatrolTimer :" + Integer.toString(patrolTimer * 60) + "\ncountDown :" + Integer.toString(countDown * 60));

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
        this.sendBroadcast(new Intent(BROD_KILL_SCANNER));
        Interpol.getInstance().setOutOfMainActivity(false);
        this.Unlock();
        this.finish();
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

            Toast.makeText(context, "Good Patrol ", Toast.LENGTH_LONG).show();
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
            Toast.makeText(getApplicationContext(), String.format("%d points missing",tot), Toast.LENGTH_LONG).show();
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

    private void startTimerStop(int countDowning) {
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
                Toast.makeText(context, "Finished!", Toast.LENGTH_LONG).show();

            }
        }.start();


    }
    private void startTimer() {
        mCountDownTimer = new CountDownTimer(PATROLTIMER, 1000) {
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

        timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);

        ttvDuraton.setText(timeLeftFormatted);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        btnScan.setText("Start Patrol");
        startTimer();
        //startTimerStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_SCANNER){
            this.Lock();
            if(resultCode == Activity.RESULT_OK){
                Bundle bundle = data.getExtras();
                String scanData = bundle.getString(CONTENT);
                //String content_ = bundle.getString(CONTENT);
                Log.d("POINTS", "scanned point "+scanData);

                if(contains(pointCol, scanData)){
                    if( (contains(listItems, scanData) && !startingPoint.pointId.equals(scanData))){
                        Toast.makeText(context, "Point Already scanned", Toast.LENGTH_LONG).show();
                    }else{

                        listItems.add(get(pointCol,scanData));
                        patrol();
                        if(listItems.size()>1){
                            //create scanned point event
                            this.firebaseManager.sendEventType(collection,"",scanData, 0, "");
                            Toast.makeText(getApplicationContext(),"Point scanned", Toast.LENGTH_LONG).show();
                        }
                    }

                }else{
                    Toast.makeText(context, "Point not from site", Toast.LENGTH_LONG).show();
                }

                verifyPatrolVisually(listItems, pointCol);

               sort(listItems, pointCol);

            }else if(resultCode == Activity.RESULT_CANCELED){
                Toast.makeText(getApplicationContext(),"BACK PRESSED", Toast.LENGTH_LONG).show();
            }
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
    @Override
    public void onBackPressed() {
        Toast.makeText(context, "Patrol in session...", Toast.LENGTH_SHORT).show();
    }
}
