package com.example.developer.objects;

import com.google.firebase.firestore.GeoPoint;

public class MyPoint {

    private String pointDescription, pointId;
    private GeoPoint location;


    public MyPoint(String pointDescription, GeoPoint geoPoint, String pointId){
        this.location = geoPoint;
        this.pointDescription = pointDescription;
        this.pointId = pointId;
    }

    public String getPointId() {
        return pointId;
    }

    public GeoPoint getGeoPoint() {
        return location;
    }

    public String getPointDescription() {
        return pointDescription;
    }
}
