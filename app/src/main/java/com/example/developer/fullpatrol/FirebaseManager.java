package com.example.developer.fullpatrol;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.developer.ServerSide.AppleProjectDB;
import com.example.developer.ServerSide.FirebaseClientManager;
import com.example.developer.objects.PatrolPoint;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class FirebaseManager {
    public static final String TAG = "OnFirebase";
    private static FirebaseManager instance;
    private FirebaseFirestore db;
    private DocumentReference docRef;
    private DocumentReference docRefData;
    private AppleProjectDB projectDB;


    private FirebaseManager(){

    }

    public void init(String site, String docPath){
        this.db = FirebaseFirestore.getInstance();
        this.docRef = db.collection(site).document(docPath);
        this.docRefData = db.collection(site).document(docPath);
        this.projectDB = MainActivity.getAppleProjectDBServer();
    }
    public void init(){
        this.db = FirebaseFirestore.getInstance();

    }

    public void addSite(String collectionName, Map object){
        
        MainActivity.getAppleProjectDBServer().addEvent(FirebaseClientManager.getFirebaseClientManagerInstance().toContentValues(object));
        db.collection(collectionName).add(object).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Log.d(TAG, "DocumentSnapshot added with ID" + documentReference.getId());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Error adding document", e);
            }
        });

    }

    public void pushData(String collectionName, Map object,final DataPushCallack dataPushCallack, String documentId){
        if(documentId != null){
            //site/hgdhnfkjfkjgkj/m
            db.collection(collectionName).document(documentId).set(object).addOnSuccessListener(new OnSuccessListener<DocumentReference>()  {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    dataPushCallack.onPushed();

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error adding document", e);
                }
            });
        }else{
            db.collection(collectionName).add(object).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    dataPushCallack.onPushed();
                    Log.d(TAG, "DocumentSnapshot added with ID" + documentReference.getId());
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error adding document", e);
                }
            });
        }


    }

    public void updateField(String collection, String document, Map<String, Object> data, final DataPushCallack dataPushCallack){

        db.collection(collection).document(document).update(data).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                dataPushCallack.onPushed();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Error adding document", e);
            }
        });
    }

    public void sendEventType(String collection, String description, int eventID, String siteID){
        Map<String, Object> event = new HashMap<>();
        event.put("siteId", MainActivity.siteId);
        event.put("eventId", eventID);
        event.put("siteIdInt", MainActivity.siteIdInt);
        event.put("machineId", MainActivity.deviceId);
        event.put("description", description);
        event.put("timestamp", FieldValue.serverTimestamp());
        event.put("location", "N-S");
        addSite(collection, event);

    }

    public void addDevice(String collection, boolean isActive, String siteID, String documentId ,DataPushCallack dataPushCallack){
        Map<String, Object> machine = new HashMap<>();
        machine.put("isActive", isActive);
        machine.put("timeStamp", FieldValue.serverTimestamp());

        pushData(collection, machine,dataPushCallack, documentId);

    }



    public void linkDevice(String col, String doucumentId, final DataPushCallack callack, String docSiteId){
        Map<String, Object> ref = new HashMap<>();

        final DocumentReference documentReferenceToSite = db.collection("site").document(docSiteId);
        final DocumentReference documentReference = db.collection("machines").document(doucumentId);
        ref.put("machineId", documentReference);
        db.collection(col).add(ref).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {
                if(task.isSuccessful()){
                    Map<String, Object> machineDoc = new HashMap<>();
                    machineDoc.put("isActive", true);
                    machineDoc.put("siteId", documentReferenceToSite );
                    documentReference.update(machineDoc).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                callack.onPushed();
                            }else{
                                Log.i("RFC", task.getException().getMessage());
                            }
                        }
                    });

                }else{
                    Log.i("RFC", task.getException().getMessage());

                }
            }
        });

    }

    public void unlinkDevice(final String uid, final String machineCollection, final String siteMachineCollection, final DataPushCallack dataPushCallack){
        final CollectionReference machines = this.db.collection(machineCollection);
        machines.document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot doc = task.getResult();
                    final  DocumentReference siteRef = doc.getDocumentReference("siteId");
                    Query query = siteRef.collection(siteMachineCollection).whereEqualTo("machineId", machines.document(uid));
                    query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.isSuccessful()){
                                CollectionReference siteMachines = siteRef.collection(siteMachineCollection);
                                for(DocumentSnapshot document  : task.getResult()){
                                    siteMachines.document(document.getId()).delete();
                                }

                                Map<String,Object> updates = new HashMap<>();
                                updates.put("siteId", FieldValue.delete());
                                updates.put("isActive", false);

                                updateField(machineCollection, uid, updates, dataPushCallack);
                            }else{
                                //error
                            }
                        }
                    });

                }else{
                    //error
                }
            }
        });
    }

    public void deleteDocument(String collection, String document, final DataPushCallack dataPushCallack){
        db.collection(collection).document(document).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful())
                    dataPushCallack.onPushed();
                else{
                    //error

                }
            }
        });
    }
    public void sendEventType(String collection, String description, String pointId, int eventID, String siteID){
        Map<String, Object> event = new HashMap<>();
        event.put("siteId", MainActivity.siteId);
        event.put("eventId", eventID);
        event.put("pointId", pointId);
        event.put("siteIdInt ", MainActivity.siteIdInt);
        event.put("machineId", MainActivity.deviceId);
        event.put("description", description);
        event.put("timestamp", FieldValue.serverTimestamp());
        event.put("location", "N-S");
        addSite(collection, event);



    }
    public void getData(String col, String docPath, final DataCallback dataCallback){
        FirebaseFirestore dbTmp = FirebaseFirestore.getInstance();
        DocumentReference docRefTmp = db.collection(col).document(docPath);
        docRefTmp.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()){

                        if(dataCallback != null){
                            dataCallback.onDataReceived(document.getData());
                        }

                    }else{
                        Log.d(TAG, "No such document");
                    }
                }else{
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }


    public void query(String col, String field, Object obj, final DataCallback callback){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Query capitalCities = db.collection(col).whereEqualTo(field, obj);
        capitalCities.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    QuerySnapshot query = task.getResult();
                    if(!query.isEmpty()){
                        Log.i("RFC", "GOT");
                       List<DocumentSnapshot> documentSnapshots = query.getDocuments();
                       List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();


                       for(int pos = 0; pos < documentSnapshots.size(); pos++){
                           DocumentSnapshot doc = documentSnapshots.get(pos);
                           Map<String, Object> item = doc.getData();
                           item.put("documentID", doc.getId());
                           data.add(item);
                           Log.i("RFC", "GOyuT|" + doc.getId() + "|"+ item.get("documentID"));

                       }
                       callback.onDataReceived(data);
                    }else{
                        callback.onDataReceived(new ArrayList<Map<String, Object>>());

                    }

                }else{
                    Log.i("RFC", task.getException().getMessage());
                }
            }
        });
    }
    public void getPatrolPointsLocally(String patrolPointsCol, final  DataCallback dataCallback){
        getPatrolPoints(patrolPointsCol, new DataCallback() {
            @Override
            public void onDataUpdated(Map<String, Object> data) {

            }

            @Override
            public void onDataReceived(Map<String, Object> data) {

                List<PatrolPoint> dada = getPointDataLocally();
                Log.i(TAG, "getPatrolPointsLocally: "+getPointDataLocally());
                SiteDataManager.getInstance().put("patrolPoints", getPointDataLocally());
                dataCallback.onDataReceived(new HashMap());
            }

            @Override
            public void onDataReceived(List<Map<String, Object>> data) {

            }
        });


    }

    private List<PatrolPoint> getPointDataLocally(){

        List<PatrolPoint> data = new ArrayList<>();
        String[] tableCols = projectDB.getColumnNames("PatrolPoints");
        double longi = 0, lati = 0;
        Cursor pointCursor = projectDB.getTableData("PatrolPoints");
        Log.i("WSX", "getPointDataLocally: "+pointCursor.getCount());
        String pointId = "", pointDescription = "";
        if(pointCursor.moveToNext()) {
            do {


                for (int i = 0; i < tableCols.length; i++) {
                    Log.i("WSX", "getPointDataLocally2: "+pointCursor.getColumnIndex(tableCols[i]));
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
                        PatrolPoint pointObj = new PatrolPoint(geoPoint, pointDescription, pointId, false);
                        longi = 0; lati = 0;
                        pointId = ""; pointDescription = "";
                        if(data != null){
                            if (!data.contains(pointObj))
                                data.add(pointObj);
                        }
                    }


                }
            } while (pointCursor.moveToNext());
        }

        return data;
    }
    public void getPatrolPoints(String patrolPointsCol, final  DataCallback dataCallback){
        db.collection(patrolPointsCol).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){


                    List<PatrolPoint> patrolPoints = new ArrayList<>();

                    for (DocumentSnapshot doc : task.getResult()) {
                        PatrolPoint pointObj = new PatrolPoint(doc.getGeoPoint("geoPoint"), doc.getString("pointDescription"), doc.getId(), false);

                        patrolPoints.add(pointObj);
                        Log.i("RFV", patrolPoints.size()+"  -  "+ pointObj.pointDescription);


                    }


                    Log.i("RFV", patrolPoints.size()+"|"+patrolPoints+" -  "+  task.getResult().size());

                    //SiteDataManager.getInstance().put("patrolPoints", patrolPoints);

                    MainActivity.getAppleProjectDBServer().updatePointsValuesList(patrolPoints);
                    dataCallback.onDataReceived(new HashMap());

                }
            }
        });

    }

    public void getPatrolDataInSync(final DataCallback dataCallback){

        docRefData.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w("asd", "Listen failed.", e);
                    return;
                }

                String source = snapshot != null && snapshot.getMetadata().hasPendingWrites()
                        ? "Local" : "Server";

                if (snapshot != null && snapshot.exists()) {
                    Log.d("asd", source + " data: " + snapshot.getData());
                    Log.d("asd", "DocumentSnapshot data: " + snapshot.getData());
                    String startPatrolTime = snapshot.getString("startPatrolTime"), endPatrolTime = snapshot.getString("endPatrolTime");
                    String[] times = (startPatrolTime + ":" + endPatrolTime).replace(" ", "").split(":");

                    Map<String, Object> data = snapshot.getData();
                    data.put("startHour", Integer.parseInt(times[0]));
                    data.put("startMin", Integer.parseInt(times[1]));
                    data.put("endHour", Integer.parseInt(times[2]));
                    data.put("endMin", Integer.parseInt(times[3]));

                    Log.d("asd", source + " data: null");
                    if(dataCallback != null){
                        dataCallback.onDataUpdated(data);
                        dataCallback.onDataReceived(data);

                    }
                } else {
                    Log.d("asd", source + " data: null");
                }
            }
        });
    }


    private Map<String, Object> getLocalSiteData(){
        String[] tableCols = projectDB.getColumnNames("Sites");
        //displayPointData();
        Cursor siteCursor;
        siteCursor = projectDB.getTableData("Sites");
        Map<String, Object> data = new HashMap<>();
        if(siteCursor.moveToNext()){
            do{
                for(int i = 0 ; i < tableCols.length ; i++){
                    String colContent = siteCursor.getString(siteCursor.getColumnIndex(tableCols[i]));
                    switch (tableCols[i]){
                        case "siteId":
                            data.put("siteId", colContent);
                            break;
                        case "siteIdInt":
                            data.put("siteIdInt", colContent);
                            break;
                        case "siteName":
                            data.put("siteName", colContent);
                            break;
                        case "area":
                            data.put("area", colContent);
                            break;
                        case "startEndPoint":
                            data.put("startEndPoint", colContent);
                            break;
                        case "startPatrolTime":
                            data.put("startPatrolTime", colContent);
                            break;
                        case "endPatrolTime":
                            data.put("endPatrolTime", colContent);
                            break;
                        case "minTime":
                            data.put("minTime", colContent);
                            break;
                        case "maxTime":
                            data.put("maxTime", colContent);
                            break;
                        case "intervalTimer":
                            data.put("intervalTimer", colContent);
                            break;
                        case "startDelay":
                            data.put("startDelay", colContent);
                            break;
                    }
                }
            }while (siteCursor.moveToNext());
        }

        Log.i(TAG, "getLocalSiteData: "+data.keySet() +"|"+data);
        return data;
    }

    public void getPatrolDataLocally(final DataCallback dataCallback){
        getPatrolData(new DataCallback() {
            @Override
            public void onDataUpdated(Map<String, Object> data) {

            }

            @Override
            public void onDataReceived(Map<String, Object> data) {
                //added
                Log.i("ZAQ@", "onDataReceived: "+data);
               if(data != null){


                   Map<String, Object> localData = getLocalSiteData();

                   Log.i("RFC", "getPatrolDataLocally: data set"+localData.keySet());
                   Log.i("RFC", "getPatrolDataLocally: "+localData);

                   String startPatrolTime = localData.get("startPatrolTime").toString(), endPatrolTime = localData.get("endPatrolTime").toString();
                   String[] times = (startPatrolTime + ":" + endPatrolTime).replace(" ", "").split(":");
                   localData.remove("startPatrolTime"); data.remove("endPatrolTime");
                   localData.put("startHour", Integer.parseInt(times[0]));
                   localData.put("startMin", Integer.parseInt(times[1]));
                   localData.put("endHour", Integer.parseInt(times[2]));
                   localData.put("endMin", Integer.parseInt(times[3]));

                   if(dataCallback != null){
                       if(!localData.isEmpty())
                           SiteDataManager.getInstance().setData(localData);
                           dataCallback.onDataReceived(localData);
                   }

                   //added

               }
            }

            @Override
            public void onDataReceived(List<Map<String, Object>> data) {




            }
        });




    }

    public void getPatrolData(final DataCallback dataCallback){

        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        String startPatrolTime = document.getString("startPatrolTime"), endPatrolTime = document.getString("endPatrolTime");
                        String[] times = (startPatrolTime + ":" + endPatrolTime).replace(" ", "").split(":");

                        Map<String, Object> data = document.getData();

                        ContentValues values = FirebaseClientManager.getFirebaseClientManagerInstance().toContentValues(data);

                        Log.i(TAG, "siteIdInt: "+document.get("siteIdInt"));
                        try {

                            MainActivity.siteIdInt = Integer.parseInt(String.valueOf( document.get("siteIdInt")));

                            Log.i(TAG, "siteIdInt: "+document.get("siteIdInt"));
                        }catch (Exception e){
                            Log.i(TAG, "onComplete: failed to get siteIdInt "+e);
                        }

                        data.put("startHour", Integer.parseInt(times[0]));
                        data.put("startMin", Integer.parseInt(times[1]));
                        data.put("endHour", Integer.parseInt(times[2]));
                        data.put("endMin", Integer.parseInt(times[3]));


                       // MainActivity.getAppleProjectDBServer().updateEachRow("Sites", values, MainActivity.siteId);
                        MainActivity.getAppleProjectDBServer().addValuesToSite(values);
                        //get data from the local db and push it.

                        if(dataCallback != null){
                            dataCallback.onDataReceived(data);
                        }
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

    }

    public static FirebaseManager getInstance(){

        if(instance == null)
            instance = new FirebaseManager();

        return instance;
    }

    public void getCollection(String collection, String field, int limit, final DataCallback callback){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(collection).orderBy(field, Query.Direction.DESCENDING).limit(limit).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    QuerySnapshot query = task.getResult();
                    if(!query.isEmpty()){
                        Log.i("RFC", "GOT");
                        List<DocumentSnapshot> documentSnapshots = query.getDocuments();
                        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();

                        for(int pos = 0; pos < documentSnapshots.size(); pos++){
                            DocumentSnapshot snapshot = documentSnapshots.get(pos);
                            Map<String, Object> item = snapshot.getData();
                            item.put("documentID", snapshot.getId());
                            data.add(item);
                            Log.i("RFC", "GOyuT");

                        }
                        callback.onDataReceived(data);
                    }else{
                        callback.onDataReceived(new ArrayList<Map<String, Object>>());
                    }
                }else{
                    Log.i("RFC", task.getException().getMessage());
                }
            }
        });
    }

    public void setSettings(FirebaseFirestoreSettings settings) {
        this.db.setFirestoreSettings(settings);
    }

    public interface DataCallback{

        void onDataUpdated(Map<String, Object> data);
        void onDataReceived(Map<String, Object> data);
        void onDataReceived(List<Map<String, Object>> data);
    }

    public  interface DataPushCallack{
        void onPushed();
    }

}
