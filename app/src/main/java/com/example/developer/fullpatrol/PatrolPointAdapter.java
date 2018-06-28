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

public class PatrolPointAdapter extends ArrayAdapter<PatrolPoint> {
    public PatrolPointAdapter(@NonNull Context context, int resource, @NonNull List<PatrolPoint> objects) {
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
        PatrolPoint patrolPoint = this.getItem(position);

        ttvName.setText((patrolPoint.pointDescription.isEmpty() ? patrolPoint.pointId : patrolPoint.pointDescription) + (patrolPoint.isStarting? " ( Starting Point )" : ""));
        ttvUID.setText(patrolPoint.pointId);

        if(patrolPoint.isScanned){
            convertView.setBackgroundResource(R.color.colorAccent);
        }else{
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }
        return convertView;

    }
}
