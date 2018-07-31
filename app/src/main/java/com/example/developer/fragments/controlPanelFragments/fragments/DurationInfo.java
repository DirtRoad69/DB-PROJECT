package com.example.developer.fragments.controlPanelFragments.fragments;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.NumberPicker;
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

public class DurationInfo extends KioskFragment implements View.OnClickListener {
    public static String INTERVAL = "interval";
    View view;
    private EditText edtEndTime, edtStartTime, edtInterval, edtDelay, edtMin, edtMax;
    int hour, minute;
    private String title = "DurationInfo";
    ContentValues value;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.duration_info, container, false);
        Log.i("ZAQ", "onCreateView: DurationInfo");


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
                setAllTimesToValue();
                if(value.size() != 0){
                    MainActivity.getAppleProjectDBServer().updateEachRow("Sites", value, MainActivity.siteId);
                    Log.i("ZAQ@", "onClick: " + value.keySet());

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




        view.findViewById(R.id.edt_end_time).setOnClickListener(this);

        return view;
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

    public void setTime(final EditText editText, final int id){

        TimePickerDialog mTimePicker;
        mTimePicker = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {

                editText.setText(selectedHour + ":" + selectedMinute);

                switch (id){
                    case 0:
                        value.put("startPatrolTime", selectedHour + ":" + selectedMinute);
                        break;
                    case 1:
                        value.put("endPatrolTime", selectedHour + ":" + selectedMinute);
                        break;

                }

            }
        }, hour, minute, true);//Yes 24 hour time
        mTimePicker.setTitle("Start Time");
        mTimePicker.show();
    }

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

    @Override
    public String getTitle() {
        return title;
    }
}
