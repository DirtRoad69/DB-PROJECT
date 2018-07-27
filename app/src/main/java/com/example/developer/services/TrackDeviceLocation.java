package com.example.developer.services;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TrackDeviceLocation implements LocationListener {

    public static final String TAG = "ZAQ";
    public static final String ACTION_LOCATION_BROADCAST = "com.example.broadcast.LOCATION_OBJECT";
    private final Context context;

    private String update;
    private String cityName;

    private Geocoder geocoder;

    List<Address> addresses;
    private GeoPoint geopoint;

    public TrackDeviceLocation(Context context) {
        this.context = context;
    }


    @Override
    public void onLocationChanged(Location location) {


        GeoPoint point =  new GeoPoint(location.getLatitude(), location.getLongitude());

//        Log.i(TAG, "onLocationChanged: "+point.getLatitude()+" | "+point.getLatitude());
//        Toast.makeText(context, point.getLatitude()+" | "+point.getLatitude(), Toast.LENGTH_SHORT).show();
        setGeopoint(point);
        Log.i("ZAQ", "onReceive: "+ "onLocationChanged");
        Intent intent = new Intent();

        intent.putExtra("latitude", location.getLatitude());
        intent.putExtra("longitude", location.getLongitude());
        intent.putExtra("timestamp", getTime());
        intent.setAction(ACTION_LOCATION_BROADCAST);
        context.sendBroadcast(intent);

    }


    public static String getTime(){
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();


        return formatter.format(date);


    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    public void setGeopoint(GeoPoint geopoint) {
        this.geopoint = geopoint;
    }

    public void getGeopoint(OnlocationReceivedCallBack geopointCallBack) {
        geopointCallBack.OnlocationReceived(geopoint);
    }

    public interface OnlocationReceivedCallBack{
        void OnlocationReceived(GeoPoint geoPoint);
    }
    public interface OnlocationSendCallBack{
        void OnlocationSend(GeoPoint point);
    }
}
