package com.example.developer.ServerSide;

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;


import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.objects.MyPoint;
import com.example.developer.services.subroutines.Observer;
import com.example.developer.services.subroutines.Subject;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FirebaseClientManager extends Subject {

    private static FirebaseClientManager firebaseClientManagerInstance;
    private FirebaseFirestore db;
    private DocumentReference docRef;
    private DocumentReference docRefData;
    private DocumentReference docPointRef;

    private FirebaseClientManager(String name){
        super(name);

    }

    public void init(String site, String docPath){
        this.db = FirebaseFirestore.getInstance();
        this.docRef = db.collection(site).document(docPath);
        this.docRefData = db.collection(site).document(docPath);
    }

    public void pushToCloud(Map<String, Object> siteMap){

            docRefData
                    .update(siteMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("ZA@", "DocumentSnapshot successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("ZA@", "Error writing document", e);
                        }
                    });


    }

    public void getPatrolDataInSync(){

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
                    Log.d("ZAQ@", source + " data: " + snapshot.getData());
                    Log.d("ZA@", "DocumentSnapshot data: " + snapshot.getData());
//                    String startPatrolTime = snapshot.getString("startPatrolTime"), endPatrolTime = snapshot.getString("endPatrolTime");
//                    String[] times = (startPatrolTime + ":" + endPatrolTime).replace(" ", "").split(":");
                    Map<String, Object> data = snapshot.getData();

                    MainActivity.getAppleProjectDBServer().updateEachRow("Sites", toContentValues(data),MainActivity.siteId);
                    Log.i("ZAQ@", "onEvent: "+ data.keySet());

                    stateChanged(3);
//                    data.put("startHour", Integer.parseInt(times[0]));
//                    data.put("startMin", Integer.parseInt(times[1]));
//                    data.put("endHour", Integer.parseInt(times[2]));
//                    data.put("endMin", Integer.parseInt(times[3]));
                   // MainActivity.getAppleProjectDBServer().updateEachRow();


                } else {
                    Log.d("asd", source + " data: null");
                }
            }
        });
    }

    public ContentValues toContentValues(Map<String, Object> dataMap){
        ContentValues values = new ContentValues();
        Object[] keys = dataMap.keySet().toArray();


        for(int i = 0 ; i < keys.length ; i++){
            values.put(keys[i].toString(), dataMap.get(keys[i].toString()).toString());
        }


        return values;
    }

    public static FirebaseClientManager getFirebaseClientManagerInstance(){
        if(firebaseClientManagerInstance == null){
            firebaseClientManagerInstance = new FirebaseClientManager("firebase");
        }

        return firebaseClientManagerInstance;
    }


    @Override
    public void stateChanged(int state) {

        notifyAllObservers();

    }

    @Override
    public void attach(Observer observer) {
        AppleProjectDB.observers.add(observer);
    }

    @Override
    public void notifyAllObservers() {
        for (Observer observer : AppleProjectDB.observers) {
            observer.update(MainActivity.SERVER);
        }
    }

    public void getDeleteCol(String docId){

        String colName = "site/" + MainActivity.siteId +"/"+ "patrolPoints";
        db.collection(colName).document(docId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("ZAQ@", "DocumentSnapshot successfully deleted!");

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("ZA@", "Error deleting document", e);
                    }
                });
    }

    public void deleteCollection(final DataDelFinishedCallback dataDelFinishedCallback){
        String colName = "site/" + MainActivity.siteId +"/"+ "patrolPoints";
        db.collection(colName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {

                            List<String> docIds = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d("ZAQ@", document.getId() + " => " + document.getData());


                                docIds.add(document.getId());

                            }
                            dataDelFinishedCallback.OnDelComplete(docIds);

                        } else {
                            Log.d("ZAQ@", "Error getting documents: ", task.getException());
                        }
                    }
                });




    }
    public void completeDelete(final OnCompleteDeleteListener onCompleteDeleteListener){


        deleteCollection(new DataDelFinishedCallback() {
            @Override
            public void OnDelComplete(List<String> count) {
                for(int i = 0 ; i < count.size() ; i++){
                    getDeleteCol(count.get(i));

                }
                onCompleteDeleteListener.deleteCompleted(true);
            }

        });

    }
    private void updatePointCol(List<MyPoint> points){


        for(int i = 0 ; i < points.size(); i++){

            DocumentReference ref = db.document("site/" + MainActivity.siteId +"/"+ "patrolPoints/" + points.get(i).getPointId());

            ref
                    .set(points.get(i))
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("ZA@", "DocumentSnapshot successfully written!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("ZA@", "Error writing document", e);
                        }
                    });


        }



    }

    public void pushPointToServer(final List<MyPoint> points) {
        completeDelete(new OnCompleteDeleteListener() {
            @Override
            public void deleteCompleted(boolean bool) {
                if(bool){}
                    updatePointCol(points);
            }
        });


    }


    public interface DataDelFinishedCallback{
        void OnDelComplete(List<String> count);

    }
    public interface OnCompleteDeleteListener{
        void deleteCompleted(boolean bool);
    }

    public interface DataCallback{


        void onDataReceived(Map<String, Object> data);
    }
}
