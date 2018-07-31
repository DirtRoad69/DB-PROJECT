package com.example.developer.fragments.controlPanelFragments.fragments;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.developer.ServerSide.FirebaseClientManager;

import com.example.developer.fragments.KioskFragment;
import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.fullpatrol.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DurationInfo extends KioskFragment implements View.OnClickListener,View.OnFocusChangeListener, KeyboardView.OnKeyboardActionListener {
    public static String INTERVAL = "interval";
    View view;
    private EditText edtEndTime, edtStartTime, edtInterval, edtDelay, edtMin, edtMax;
    int hour, minute;
    private String title = "DurationInfo";
    ContentValues value;
    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private boolean isCaps;
    private RelativeLayout relTemp;
    private EditText selectedEdt;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.duration_info, container, false);
        Log.i("ZAQ", "onCreateView: DurationInfo");
        this.relTemp = view.findViewById(R.id.tmp_rel);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                relTemp.setVisibility(View.VISIBLE);

            }
        }, 700);





        value = new ContentValues();
        Calendar mcurrentTime = Calendar.getInstance();
        hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        minute = mcurrentTime.get(Calendar.MINUTE);


        edtStartTime = view.findViewById(R.id.edt_start_time);
        edtEndTime = view.findViewById(R.id.edt_end_time);
        edtDelay = view.findViewById(R.id.edt_end_count_time);
        edtInterval = view.findViewById(R.id.edt_interval_time);
        edtMin = view.findViewById(R.id.edt_min_time);
        edtMax = view.findViewById(R.id.edt_max_time);

        displaySiteTimes();




        view.findViewById(R.id.btn_collect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                done();
                setAllTimesToValue();
                if(value.size() != 0){
                    MainActivity.getAppleProjectDBServer().updateEachRow("Sites", value, MainActivity.siteId);
                    Log.i("ZAQ@", "onClick: " + value.keySet());
                    close();
                    addSiteToCloud();

                    Toast.makeText(getContext(), "DATA SAVED", Toast.LENGTH_SHORT).show();
                    ControlPanelFragment.setViewPager(2);

                    value.clear();
                }else{
                    Toast.makeText(getContext(), "Error 413 No Changes \n Detected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        view.findViewById(R.id.ttv_start_time).setOnClickListener(this);
        edtStartTime.setOnClickListener(this);
        edtEndTime.setOnClickListener(this);
        edtDelay.setOnClickListener(this);
        edtInterval.setOnClickListener(this);
        edtMin.setOnClickListener(this);
        edtMax.setOnClickListener(this);



        //keyBoard

        edtStartTime.requestFocus();

        this.edtStartTime.setOnFocusChangeListener(this);
        this.edtEndTime.setOnFocusChangeListener(this);
        this.edtDelay.setOnFocusChangeListener(this);
        this.edtInterval.setOnFocusChangeListener(this);
        this.edtMin.setOnFocusChangeListener(this);
        this.edtMax.setOnFocusChangeListener(this);
        //view.findViewById(R.id.btn_done).setOnClickListener(this);
        this.keyboardView = view.findViewById(R.id.kbv_input);
        this.keyboard = new Keyboard(getContext(), R.xml.qwerty);
        this.keyboardView.setPreviewEnabled(false);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);

        isCaps = false;



        view.findViewById(R.id.edt_end_time).setOnClickListener(this);

        return view;
    }

    private void close(){
        DurationInfo.this.removeSelf();
    }
    public KioskFragment getObject(){
        return ControlPanelFragment.getDurationInfoFragment();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.edt_start_time:
           // case R.id.ttv_start_time :
                //setTime(edtStartTime, 0);
                getTime(edtStartTime, 0);
                break;

            case R.id.edt_end_time:
            //case R.id.ttv_end_time:
                //setTime(edtEndTime, 1);
                getTime(edtEndTime, 1);
                break;



            case R.id.edt_min_time:
            //case R.id.ttv_min_time:
               // setMinutes(edtMin, 2);
                getTime(edtMin, 2);
                break;

            case R.id.edt_max_time:
            //case R.id.ttv_max_time:

                //setMinutes(edtMax, 3);
                getTime(edtMax, 3);
                break;


            case R.id.edt_interval_time:
           // case R.id.ttv_interval_time:
               // setMinutes(edtInterval, 4);
                getTime(edtInterval, 4);
                break;

            case R.id.edt_end_count_time:
          // case R.id.ttv_end_count_time:
                //setMinutes(edtDelay, 5);
                getTime(edtDelay, 5);
                break;



        }
    }

    private void setAllTimesToValue(){
        List<EditText> editTextList = getAllEditText();
        for(int i = 0; i < editTextList.size() ; i++){

            getTime(editTextList.get(i), i);

        }
    }
    private void addSiteToCloud(){

        Object[] keys = value.keySet().toArray();
        Map<String, Object> siteDataObject = new HashMap<>();
        for(int i = 0 ; i < keys.length; i++){

            switch (keys[i].toString()){

                case "intervalTimer":
                    siteDataObject.put(keys[i].toString(), Integer.valueOf(value.get(keys[i].toString()).toString()));
                    break;
                case "maxTime":
                    siteDataObject.put(keys[i].toString(), Integer.valueOf(value.get(keys[i].toString()).toString()));
                    break;
                case "minTime":
                    siteDataObject.put(keys[i].toString(), Integer.valueOf(value.get(keys[i].toString()).toString()));
                    break;
                case "startDelay":
                    siteDataObject.put(keys[i].toString(), Integer.valueOf(value.get(keys[i].toString()).toString()));
                    break;
                default:
                        siteDataObject.put(keys[i].toString(), value.get(keys[i].toString()));


            }
            Log.i("ZAQ@", "addSiteToCloud: "+keys[i].toString()+" "+value.get(keys[i].toString()));
        }
        Log.i("ZAQ@", "addSiteToCloud: "+keys+" | "+ value);
        FirebaseClientManager.getFirebaseClientManagerInstance().pushToCloud(siteDataObject);

    }
    private List<EditText> getAllEditText(){
        List<EditText> editTextList = new ArrayList<>();
        editTextList.add(edtStartTime);
        editTextList.add(edtEndTime);
        editTextList.add(edtMin);
        editTextList.add(edtMax);
        editTextList.add(edtInterval);
        editTextList.add(edtDelay);
        return editTextList;
    }
    private void displaySiteTimes(){
        String[] tableCols = MainActivity.getAppleProjectDBServer().getColumnNames("Sites");
        Cursor siteCursor = MainActivity.getAppleProjectDBServer().getTableData("Sites");
        if(siteCursor.moveToNext()){
            do{
                for(int i = 0 ; i < tableCols.length ; i++){
                    String colContent = siteCursor.getString(siteCursor.getColumnIndex(tableCols[i]));
                    switch (tableCols[i]){


                        case "startPatrolTime":
                            edtStartTime.setText(colContent);
                            break;
                        case "endPatrolTime":
                            edtEndTime.setText(colContent);
                            break;
                        case "minTime":
                            edtMin.setText(colContent);
                            break;
                        case "maxTime":
                            edtMax.setText(colContent);
                            break;
                        case "intervalTimer":
                            edtInterval.setText(colContent);
                            break;
                        case "startDelay":
                            edtDelay.setText(colContent);
                            break;
                    }
                }
            }while (siteCursor.moveToNext());
        }


    }
    public void setMinutes(final EditText editText, final int id){
        final NumberPicker picker = new NumberPicker(getContext());
        picker.setMinValue(0);
        picker.setMaxValue(90);

        FrameLayout layout = new FrameLayout(getContext());
        layout.addView(picker, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        new AlertDialog.Builder(getContext())
                .setView(layout)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("ZAQ", "setMinutes: "+picker.getValue());
                        editText.setText(Integer.toString(picker.getValue()));
                        switch (id){
                            case 2:
                                value.put("minTime", Integer.toString(picker.getValue()));

                                break;
                            case 3:
                                value.put("maxTime", Integer.toString(picker.getValue()));
                                break;
                            case 4:
                                value.put("intervalTimer", Integer.toString(picker.getValue()));
                                break;
                            case 5:
                                value.put("startDelay", Integer.toString(picker.getValue()));
                                break;


                        }
                    }
                } )
                .show();


    }

    private void getTime(EditText editText, int id){

        switch (id){
            case 0:
                if(!editText.getText().toString().isEmpty())
                    value.put("startPatrolTime",  editText.getText().toString());
                break;
            case 1:
                if(!editText.getText().toString().isEmpty())
                    value.put("endPatrolTime", editText.getText().toString());
                break;
            case 2:
                if(!editText.getText().toString().isEmpty())
                    value.put("minTime", editText.getText().toString());

                break;
            case 3:
                if(!editText.getText().toString().isEmpty())
                    value.put("maxTime", editText.getText().toString());
                break;
            case 4:
                if(!editText.getText().toString().isEmpty())
                    value.put("intervalTimer", editText.getText().toString());
                break;
            case 5:
                if(!editText.getText().toString().isEmpty())
                    value.put("startDelay", editText.getText().toString());
                break;

        }
    }

//    public void setTime(final EditText editText, final int id){
//
//        TimePickerDialog mTimePicker;
//        mTimePicker = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
//            @Override
//            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
//
//                editText.setText(selectedHour + ":" + selectedMinute);
//
//                switch (id){
//                    case 0:
//                        value.put("startPatrolTime", selectedHour + ":" + selectedMinute);
//                        break;
//                    case 1:
//                        value.put("endPatrolTime", selectedHour + ":" + selectedMinute);
//                        break;
//
//                }
//
//            }
//        }, hour, minute, true);//Yes 24 hour time
//        mTimePicker.setTitle("Start Time");
//        mTimePicker.show();
//    }

    @Override
    protected void onFragmentResult(int requestCode, int resultCode, Bundle extraData) {
        Log.i("", "onFragmentResult: in");



        if(resultCode == Activity.RESULT_OK){

        }

        ControlPanelFragment.setViewPager(0);

//        edtMin.setText("15");
//        edtInterval.setText("50");
//        edtMax.setText("20");

    }

    private void done () {
        relTemp.setVisibility(View.GONE);
        //checkInputAndSignIn();
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public void onPress(int primaryCode) {

    }

    @Override
    public void onRelease(int primaryCode) {

    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        if (selectedEdt != null) {
            switch (primaryCode) {
                case Keyboard.KEYCODE_DELETE:
                    String text = selectedEdt.getText().toString();
                    if(!text.isEmpty()){
                        int pos = selectedEdt.getSelectionStart(), end = selectedEdt.getSelectionEnd();
                        if (pos > 0){
                            String textNew = selectedEdt.getText().delete(pos - 1, end).toString();
                            selectedEdt.setText(textNew);
                            selectedEdt.setSelection(pos - 1);
                        }else{
                            String textNew = selectedEdt.getText().delete(0, end).toString();
                            selectedEdt.setText(textNew);
                            selectedEdt.setSelection(0);
                        }
                    }
                    break;
                case Keyboard.KEYCODE_SHIFT:
                    isCaps = !isCaps;
                    keyboard.setShifted(isCaps);
                    keyboardView.invalidateAllKeys();
                    break;
                case Keyboard.KEYCODE_DONE:
                    this.done();
                    break;
                default:
                    char code = (char) primaryCode;
                    if (Character.isLetter(code) && isCaps) {
                        code = Character.toUpperCase(code);
                    }

                    String textToInsert = String.valueOf(code);
                    int start = Math.max(selectedEdt.getSelectionStart(), 0);
                    int end = Math.max(selectedEdt.getSelectionEnd(), 0);
                    selectedEdt.getText().replace(Math.min(start, end), Math.max(start, end),
                            textToInsert, 0, textToInsert.length());
            }


        }
    }

    @Override
    public void onText(CharSequence text) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {


        switch (v.getId()){
            case R.id.edt_start_time:
                selectedEdt = edtStartTime;
                break;

            case R.id.edt_end_time:
                selectedEdt = edtEndTime;
                break;

            case R.id.edt_min_time:
                selectedEdt = edtMin;
                break;

            case R.id.edt_max_time:
                selectedEdt = edtMax;
                break;


            case R.id.edt_interval_time:
                selectedEdt = edtInterval;
                break;

            case R.id.edt_end_count_time:
                selectedEdt = edtDelay;
                break;



        }


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        this.removeSelf();
    }
}
