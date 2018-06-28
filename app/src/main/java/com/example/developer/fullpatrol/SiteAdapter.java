package com.example.developer.fullpatrol;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class SiteAdapter extends ArrayAdapter<SiteAdapter.SiteObject> {

    public SiteAdapter(@NonNull Context context, int resource, @NonNull List<SiteObject> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.site_item, parent, false);
        }

        TextView ttvName = convertView.findViewById(R.id.ttv_name),
                ttvUID = convertView.findViewById(R.id.ttv_area);
        SiteObject siteObject = this.getItem(position);

        ttvName.setText(siteObject.Name);
        ttvUID.setText(siteObject.DocID);
        if(siteObject.isSelected){
            convertView.setBackgroundResource(R.color.colorAccent);
        }else{
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }
        return convertView;

    }

    public static class SiteObject{
        public final String Name, Area, DocID;
        public boolean isSelected;

        public  SiteObject(String name, String area, String docID){
            this.Name = name;
            this.Area = area;
            this.DocID = docID;
        }


    }
}
