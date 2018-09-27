package com.example.developer.ServerSide;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.objects.PatrolPoint;
import com.example.developer.objects.MyPoint;
import com.example.developer.objects.PatrolPointConfig;
import com.example.developer.services.subroutines.Observer;
import com.example.developer.services.subroutines.Subject;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class AppleProjectDB extends Subject {


    private SQLiteDatabase mDatabase;
    public static List<Observer> observers = new ArrayList<>();

    public AppleProjectDB(SQLiteDatabase db){
        super(db.toString());
        this.mDatabase = db;
    }

    public void createTable(String sql){

        mDatabase.execSQL(sql);

    }

    public void createSitesTable(){

        String sqlSite = "CREATE TABLE IF NOT EXISTS Sites (\n" +
                "\tsiteId varchar(200) PRIMARY KEY,\n" +
                "\tsiteName varchar(200), \n" +
                "\tarea varchar(200),\n" +
                "\tstartEndPoint varchar(200), \n" +
                "\tstartPatrolTime varchar(200), \n" +
                "\tendPatrolTime varchar(200),  \n" +
                "\tminTime int, \n" +
                "\tmaxTime int, \n" +
                "\tintervalTimer int, \n" +
                "\tstartDelay int, \n" +
                "\tsiteIdInt int \n" +
                ");";
        mDatabase.execSQL(sqlSite);
    }

    private void createEventsTable(){

        String sqlSite = "CREATE TABLE IF NOT EXISTS Events (\n" +
                "\tsiteId varchar(200) PRIMARY KEY,\n" +
                "\teventId int, \n" +
                "\tsiteIdInt int, \n" +
                "\tpointId varchar(200),  \n" +
                "\tlocation varchar(200) , \n" +
                "\tmachineId varchar(200), \n" +
                "\tdescription varchar(200), \n" +
                "\ttimestamp DATETIME \n" +
                ");";
        mDatabase.execSQL(sqlSite);
        /*

        event.put("siteId", MainActivity.siteId);
        event.put("eventId", eventID);
        event.put("pointId", pointId);
        event.put("machineId", MainActivity.deviceId);
        event.put("description", description);
        event.put("timeStamp", FieldValue.serverTimestamp());
        event.put("location", "N-S");
       */
    }

    public void addEvent(ContentValues values){
        createEventsTable();
        mDatabase.insertWithOnConflict("Events", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        //"INSERT INTO Events () VALUES ()"
    }

    @Override
    public void stateChanged(int state) {

        notifyAllObservers();

    }

    @Override
    public void attach(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void notifyAllObservers() {
        for (Observer observer : observers) {
            observer.update(MainActivity.LOCAL_DB);
        }
    }
    public void addValuesToSite(ContentValues sqlValues){
        mDatabase.insertWithOnConflict("Sites",null, sqlValues, SQLiteDatabase.CONFLICT_REPLACE);
    }
    public void updateEachRow(String tableName, ContentValues values, String id){
        mDatabase.update(tableName, values, "siteId = ?", new String[] {id});
        if(tableName.contains("Sites")){
            stateChanged(1);
        }else{
            stateChanged(2);
        }

    }

    public void insertSiteValues(ContentValues values){

        createSitesTable();
        //clearTable("Sites");

//        String sql = "INSERT INTO Sites (siteId, siteName, area, startEndPoint, startPatrolTime, endPatrolTime,  minTime, maxTime, intervalTimer, startDelay)"
//                     + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        mDatabase.insertWithOnConflict("Sites", null,values, SQLiteDatabase.CONFLICT_REPLACE);
        stateChanged(1);
    }
    public String[] getColumnNames(String tablename){

        if(tablename.contains("Sites")){
            String[] colNames = {"siteId", "siteName", "area", "startEndPoint", "startPatrolTime", "endPatrolTime",  "minTime", "maxTime", "intervalTimer", "startDelay", "siteIdInt"};
            return colNames;
        }else if(tablename.contains("PatrolPoints")) {
            String[] colNames = {"pointId", "siteId", "longi", "lati", "pointDescription"};
            return colNames;
        }
        return new String[]{"none"};

    }
    public void createPointsTable(){
        String sqlPoint = "CREATE TABLE IF NOT EXISTS PatrolPoints (\n" +
                "    pointId varchar (200) PRIMARY KEY, \n" +
                "    siteId varchar(200),\n" +
                "    pointDescription varchar(200),\n" +
                "    longi float, \n" +
                "    lati float,   \n" +
                "    siteIdInt int, \n" +

                "    FOREIGN KEY (siteId) REFERENCES Sites(siteId)\n" +
                ");     \n";

        mDatabase.execSQL(sqlPoint);
    }
    private void clearTable(String tableName){
        String sql = "DELETE FROM " +tableName;
        mDatabase.execSQL(sql);

    }

    public void updatePointsValues(List<PatrolPointConfig> values){
        createPointsTable();
        clearTable("PatrolPoints");
//        String sqlPointData  = "INSERT INTO PatrolPoints (pointId, siteId, longi, lati, pointDescription )\n" +
//                "VALUES (?, ?, ?, ?, ?)";
        stateChanged(2);
        ContentValues sqlValues = new ContentValues();
        List<MyPoint> pointsObjCollection = new ArrayList<>();

        for(int i = 0; i < values.size(); i++){

            String pointId = values.get(i).pointId;
            String longi = String.valueOf(values.get(i).location.getLongitude());
            String lati = String.valueOf(values.get(i).location.getLatitude());
            String pointDescription = values.get(i).pointDescription;

            sqlValues.put("pointId", pointId);
            sqlValues.put("siteId", MainActivity.siteId);
            sqlValues.put("longi", longi);
            sqlValues.put("lati", lati);
            sqlValues.put("pointDescription", pointDescription);






            //push to server

            GeoPoint location = new GeoPoint(Double.valueOf(lati),Double.valueOf(longi));
//            point.put("location", location);
//            point.put("pointDescription", pointDescription);

            MyPoint pointConfig = new MyPoint(pointDescription, location, pointId);
            pointsObjCollection.add(pointConfig);


            mDatabase.insertWithOnConflict("PatrolPoints",null, sqlValues, SQLiteDatabase.CONFLICT_REPLACE);
        }



        FirebaseClientManager.getFirebaseClientManagerInstance().pushPointToServer(pointsObjCollection);



    }

    public void updatePointsValuesList(List<PatrolPoint> values){
        createPointsTable();

        clearTable("PatrolPoints");
//        String sqlPointData  = "INSERT INTO PatrolPoints (pointId, siteId, longi, lati, pointDescription )\n" +
//                "VALUES (?, ?, ?, ?, ?)";
        stateChanged(2);
        ContentValues sqlValues = new ContentValues();
        List<MyPoint> pointsObjCollection = new ArrayList<>();
        Log.i("ZAQ@", "updatePointsValuesList: "+values);
        for(int i = 0; i < values.size(); i++){
            String pointId = values.get(i).pointId;
            String longi = String.valueOf(values.get(i).location.getLongitude());
            String lati = String.valueOf(values.get(i).location.getLatitude());
            String pointDescription = values.get(i).pointDescription;

            sqlValues.put("pointId", pointId);
            sqlValues.put("siteId", MainActivity.siteId);
            sqlValues.put("longi", longi);
            sqlValues.put("lati", lati);
            sqlValues.put("pointDescription", pointDescription);






            //push to server

            GeoPoint location = new GeoPoint(Double.valueOf(lati),Double.valueOf(longi));
//            point.put("location", location);
//            point.put("pointDescription", pointDescription);

            MyPoint pointConfig = new MyPoint(pointDescription, location, pointId);
            pointsObjCollection.add(pointConfig);


            mDatabase.insertWithOnConflict("PatrolPoints",null, sqlValues, SQLiteDatabase.CONFLICT_REPLACE);
        }





    }

    public Cursor getTableData(String tableName){
        String select = "SELECT * FROM " + tableName;
        return  mDatabase.rawQuery(select, null);
    }


}
