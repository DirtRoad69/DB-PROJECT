package com.example.developer.ServerSide;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.example.developer.fullpatrol.AlarmReceiver;
import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.fullpatrol.R;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseClientManager extends Subject {

    private static FirebaseClientManager firebaseClientManagerInstance;
    private FirebaseFirestore db;
    private DocumentReference docRef;
    private DocumentReference docRefData;
    private DocumentReference docPointRef;
    private FirebaseStorage storage;
    private ProgressDialog progressDialog;
    TextView ttvMsg;


    private FirebaseClientManager(String name){
        super(name);

    }

    public void init(String site, String docPath){
        this.db = FirebaseFirestore.getInstance();
        this.docRef = db.collection(site).document(docPath);
        this.docRefData = db.collection(site).document(docPath);
        this.storage = FirebaseStorage.getInstance();
    }
    private void upload(final StorageReference storageReference, String path, final android.content.Context context,final String contentType){
        if(storageReference == null)
            return;

        try {

            Log.i("WSX", "upload: called");

//            final ProgressDialog progressDialog = new ProgressDialog(context);
//            progressDialog.setTitle("Uploading");
//            progressDialog.show();
            showProgress("UPLOAD TO SERVER", "Uploading", context);
            Uri pathUri = Uri.fromFile(new File(path));
            UploadTask uploadTask = storageReference.putFile(pathUri);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    Log.i("WSX", "failure: "+exception.getMessage());
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                            .getTotalByteCount());
                    ttvMsg.setText("Uploading "+(int)progress+"%");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    Log.i("WSX", "onSuccess: "+taskSnapshot.getMetadata());
                    StorageMetadata metadata = new StorageMetadata.Builder()
                            .setContentType(contentType)
                            .setCustomMetadata("site", "site")
                            .setCustomMetadata("deviceId", "deviceId")
                            .setCustomMetadata("timeStamp", "timeStamp")
                            .build();

                    // Update metadata properties
                    storageReference.updateMetadata(metadata)
                            .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                @Override
                                public void onSuccess(StorageMetadata storageMetadata) {
                                    // Updated metadata is in storageMetadata
                                    Toast.makeText(context, "Updated meta", Toast.LENGTH_LONG);
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Uh-oh, an error occurred!

                                    Toast.makeText(context, "Uh-oh, an error occurred!", Toast.LENGTH_LONG);
                                }
                            });
                    dismissProgress();
                    Toast.makeText(context, "Successful", Toast.LENGTH_LONG).show();
                }
            });

        } catch (Exception e) {
            Log.i("WSX", "upload: failed");
            e.printStackTrace();
        }


    }
    private void dismissProgress(){
        if(progressDialog != null){
            progressDialog.dismiss();
            progressDialog  = null;
        }
    }
    private void showProgress(String title, String msg,android.content.Context c) {

        if (progressDialog == null) {
            View parent = LayoutInflater.from(c).inflate(R.layout.progress_layout, null, false);
            TextView ttvTitle = parent.findViewById(R.id.ttv_title);
            ttvMsg = parent.findViewById(R.id.ttv_msg);
            ttvTitle.setText(title);
            ttvMsg.setText(msg);
            progressDialog = new ProgressDialog(c);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
            progressDialog.setContentView(parent);

        }
    }
    public void createImgRefLocationAndUpload(String fileName, String path, android.content.Context context){
        Log.i("WSX", "createImgRefLocationAndUpload: called");
        if(storage == null){
            Log.i("WSX", "createImgRefLocationAndUpload: IS NOT INITIATED");
            return;
        }


        StorageReference storageRef = storage.getReference();
        StorageReference imagesRef  = storageRef.child("images/" + fileName);
        Log.i("WSX", "createImgRefLocationAndUpload: called "+imagesRef+ " filename "+fileName);
        //String fileFullPath = imagesRef.getPath();

        upload(imagesRef, path, context, "image/jpg");
    }

    public void createAudioRefLocationAndUpload(String path, android.content.Context context){
        Log.i("WSX", "createImgRefLocationAndUpload: called");
        String fileName = "Audio-"+ UUID.randomUUID().toString()+".3gp";
        if(storage == null){
            Log.i("WSX", "createImgRefLocationAndUpload: IS NOT INITIATED");
            return;
        }


        StorageReference storageRef = storage.getReference();
        StorageReference imagesRef  = storageRef.child("audio/" + fileName);
        Log.i("WSX", "createImgRefLocationAndUpload: called "+imagesRef+ " filename "+fileName);
        //String fileFullPath = imagesRef.getPath();

        upload(imagesRef, path, context, "audio/.3gp");
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


       try{
           for(int i = 0 ; i < keys.length ; i++){
               values.put(keys[i].toString(), dataMap.get(keys[i].toString()).toString());
           }

       }catch (Exception e){
           Log.i("WSX", "toContentValues: failed");
           notifyAllObservers();
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
            observer.update(MainActivity.RESTART);
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
