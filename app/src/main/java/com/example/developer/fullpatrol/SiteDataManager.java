package com.example.developer.fullpatrol;

import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SiteDataManager {
    private static SiteDataManager instance;
    private final HashMap<String, Object> dataField;


    private SiteDataManager(){
        this.dataField = new HashMap<>();
    }


    public void setData(Map<String, Object> data){

        dataField.putAll(data);
    }

    public Object get(String key){
        return dataField.get(key);
    }

    public int getInt(String key){
        return  Integer.valueOf(dataField.get(key).toString());
    }

    public Long getLong(String key){
        return  Long.valueOf(dataField.get(key).toString());
    }

    public void compareAndUpdate(Map<String, Object> data, CompareCallback compareCallback){

        compareCallback.onCompareFinished();
    }

    public void put(String key, Object val){
        this.dataField.put(key, val);
    }



    public static SiteDataManager getInstance(){
        if(instance == null)
            instance = new SiteDataManager();

        return instance;
    }

    public interface CompareCallback{
        void onCompareFinished();
    }
}
