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
        return  (int)dataField.get(key);
    }

    public Long getLong(String key){
        return  (Long) dataField.get(key);
    }

    public void compareAndUpdate(Map<String, Object> data, CompareCallback compareCallback){
//        Object[] keyCollection = dataField.keySet().toArray();
//        for(int pos = 0; pos < data.size(); pos++){
//            String key = keyCollection[pos].toString();
//            Log.i("RFC", key + "|" + data.get(key) + "|" + dataField.get(key));
//
//            if(!data.containsKey(key))
//                continue;
//
//            String newValue = data.get(key).toString(), oldValue = dataField.get(key).toString();
//            if(!newValue.equals(oldValue)){
//                dataField.put(key, data.get(key));
//                compareCallback.valueChanged(keyCollection[pos].toString());
//
//            }
//        }
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
        void valueChanged(String key);
    }
}
