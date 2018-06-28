package com.example.developer.fullpatrol;

import android.graphics.PointF;

import com.google.firebase.firestore.GeoPoint;

public class PatrolPoint {

    public final String pointDescription;
    public final String pointId;
    public final GeoPoint location;
    public boolean isScanned;
    public boolean isStarting;


    public PatrolPoint(GeoPoint pLocation, String pPointDescription, String pPointId, boolean pIsScanned){

        this.location = pLocation;
        this.pointDescription = pPointDescription;
        this.pointId = pPointId;
        this.isScanned = pIsScanned;
        isStarting = false;
    }



}
