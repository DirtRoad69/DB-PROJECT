package com.example.developer.objects;

import com.google.firebase.firestore.GeoPoint;

public class PatrolPointConfig {

    public final String pointDescription;
    public final String pointId;
    public final GeoPoint location;
    public boolean isScanned;
    public boolean isStarting;


    public PatrolPointConfig(GeoPoint pLocation, String pPointDescription, String pPointId){

        this.location = pLocation;
        this.pointDescription = pPointDescription;
        this.pointId = pPointId;

        isStarting = false;
    }



}
