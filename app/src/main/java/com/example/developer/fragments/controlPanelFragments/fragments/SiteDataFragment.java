package com.example.developer.fragments.controlPanelFragments.fragments;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.developer.ServerSide.AppleProjectDB;
import com.example.developer.adapters.PatrolPointLearningModeAdapter;
import com.example.developer.fragments.KioskFragment;
import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.fullpatrol.R;
import com.example.developer.objects.PatrolPointConfig;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;


public class SiteDataFragment extends KioskFragment implements View.OnClickListener{
    private Cursor siteCursor, pointCursor;
    private TextView name, siteId, area, startTime, endTime, startEndPoint, min, max, interval, delay;
    private AppleProjectDB projectDB;
    private Button btn_load_data;
    private List<PatrolPointConfig> pointConfigsList;
    private ListView listViewControlPanel;
    private PatrolPointLearningModeAdapter pointLearningModeAdapter;
    private TextView pointCount;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.site_data_layout, container, false);

        pointConfigsList = new ArrayList<>();
        projectDB = MainActivity.getAppleProjectDBServer();
        name = view.findViewById(R.id.ttv_name);
        siteId = view.findViewById(R.id.ttv_siteId);
        area = view.findViewById(R.id.ttv_area);

        pointCount = view.findViewById(R.id.view_content_control_panel);

        startTime = view.findViewById(R.id.ttv_startPatrolTime);
        endTime = view.findViewById(R.id.ttv_endPatrolTime);
        startEndPoint = view.findViewById(R.id.ttv_startEndPoint);
        listViewControlPanel = view.findViewById(R.id.listview_control_panel);
        min = view.findViewById(R.id.ttv_min);
        max = view.findViewById(R.id.ttv_max);
        interval = view.findViewById(R.id.ttv_intervalTimer);
        delay = view.findViewById(R.id.ttv_start_delay);
        siteCursor = projectDB.getTableData("Sites");
        pointCursor = projectDB.getTableData("PatrolPoints");
        btn_load_data = view.findViewById(R.id.btn_load_data);

        List<PatrolPointConfig> listOfFakePatrolPoints = new ArrayList<>();

        pointLearningModeAdapter = new PatrolPointLearningModeAdapter(getActivity(), R.layout.site_item,listOfFakePatrolPoints);

        listViewControlPanel.setAdapter(this.pointLearningModeAdapter);
        displaySiteData();

        btn_load_data.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Data Loaded", Toast.LENGTH_SHORT).show();
                Log.i("ZAQ!", "close: " + "4542");
                displaySiteData();
            }
        });
        return view;
    }
    private void displayPoints(List<PatrolPointConfig> listItems){
        if(listItems == null)
            return;
        for(int pos = 0; pos < listItems.size(); pos++){
            listItems.get(pos).isScanned = false;
        }


        pointCount.setText("Point Scanned: "+ listItems.size());
        this.pointLearningModeAdapter.clear();
        this.pointLearningModeAdapter.addAll(listItems);
    }
    private void displayPointData(){
        String[] tableCols = projectDB.getColumnNames("PatrolPoints");
        double longi = 0, lati = 0;
        String pointId = "", pointDescription = "";
        if(pointCursor.moveToNext()) {
            do {


                for (int i = 0; i < tableCols.length; i++) {
                    String colContent = pointCursor.getString(pointCursor.getColumnIndex(tableCols[i]));
                    switch (tableCols[i]) {

                        case "pointId":
                            pointId = colContent;
                            break;
                        case "longi":
                            longi = Double.valueOf(colContent);
                            break;
                        case "lati":
                            lati = Double.valueOf(colContent);
                            break;
                        case "pointDescription":
                            pointDescription = colContent;
                            break;
                    }
                    Log.i("ZAQ", "displayPointData: " +longi+"|"+lati+"|"+pointId+"|"+"|");
                    if (lati != 0 && longi != 0 && !pointId.isEmpty()) {
                        Log.i("ZAQ", "added: " +longi+"|"+lati+"|"+pointId+"|"+"|"+pointDescription);
                        GeoPoint geoPoint = new GeoPoint(lati, longi);
                        PatrolPointConfig pointObj = new PatrolPointConfig(geoPoint, "", pointId);
                        longi = 0; lati = 0;
                        pointId = ""; pointDescription = "";
                        if(pointConfigsList != null){
                            if (!pointConfigsList.contains(pointObj))
                                pointConfigsList.add(pointObj);
                        }
                    }


                }
            } while (pointCursor.moveToNext());
        }
        displayPoints(pointConfigsList);

    }
    private void displaySiteData(){
        String[] tableCols = projectDB.getColumnNames("Sites");
        displayPointData();

        siteCursor = projectDB.getTableData("Sites");
        if(siteCursor.moveToNext()){
            do{
                for(int i = 0 ; i < tableCols.length ; i++){
                    String colContent = siteCursor.getString(siteCursor.getColumnIndex(tableCols[i]));
                    switch (tableCols[i]){
                        case "siteId":
                            siteId.setText("siteId : " + colContent);
                            break;
                        case "siteName":
                            name.setText(""+ colContent);
                            break;
                        case "area":
                            area.setText("area : "+ colContent);
                            break;
                        case "startEndPoint":
                            startEndPoint.setText("startPoint : "+ colContent);
                            break;
                        case "startPatrolTime":
                            startTime.setText("startTime : "+ colContent);
                            break;
                        case "endPatrolTime":
                            endTime.setText("endTime : "+ colContent);
                            break;
                        case "minTime":
                            min.setText("min : "+ colContent);
                            break;
                        case "maxTime":
                            max.setText("max : "+ colContent);
                            break;
                        case "intervalTimer":
                            interval.setText("interval : "+ colContent);
                            break;
                        case "startDelay":
                            delay.setText(" Delay : "+ colContent);
                            break;
                    }
                }
            }while (siteCursor.moveToNext());
        }


    }

    @Override
    public String getTitle() {
        return this.getClass().getSimpleName();
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_load_data:

                break;


        }
    }
}
