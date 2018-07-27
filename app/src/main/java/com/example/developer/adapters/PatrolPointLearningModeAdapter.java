package com.example.developer.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import com.example.developer.fullpatrol.R;
import com.example.developer.objects.PatrolPointConfig;

import java.util.List;

public class PatrolPointLearningModeAdapter extends ArrayAdapter<PatrolPointConfig> {
    public PatrolPointLearningModeAdapter(@NonNull Context context, int resource, @NonNull List<PatrolPointConfig> objects) {
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
        PatrolPointConfig patrolPoint = this.getItem(position);

        //ttvName.setText((patrolPoint.pointDescription.isEmpty() ? patrolPoint.pointId : patrolPoint.pointDescription) + (patrolPoint.isStarting? " ( Starting Point )" : ""));
        ttvUID.setText(patrolPoint.pointId);

        ttvName.setText("NOT SET");


//        if(patrolPoint.isScanned){
//            convertView.setBackgroundResource(R.color.colorAccent);
//        }else{
//            convertView.setBackgroundColor(Color.TRANSPARENT);
//        }
        return convertView;

    }
}
