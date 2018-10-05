package com.example.developer.fragments.controlPanelFragments.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.developer.fragments.KioskFragment;
import com.example.developer.fragments.NewLocalCamera;
import com.example.developer.fragments.TakeAudioFragment;
import com.example.developer.fragments.TakePictureFragment;
import com.example.developer.fullpatrol.AlarmReceiver;
import com.example.developer.fullpatrol.FirebaseManager;
import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.fullpatrol.R;

import java.util.List;

public class ControlPanelFragment extends KioskFragment implements View.OnClickListener {

    private static final String TAG = "ZAQ";
    private static KioskFragment durationInfoFragment;
    public static ViewPager mViewPager;
    private MainActivity.MyAdapter mMyadpter;
    private String title = "ControlPanelFragment";
    public static KioskFragment pointInfoFragment;
    private static KioskFragment siteDataFragment;
    private  KioskFragment pointsObj, durationObj, siteDataObj;
    public  MainActivity.MyAdapter adapter;
    private FirebaseManager firebaseManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.firebaseManager = FirebaseManager.getInstance();


        mMyadpter = new MainActivity.MyAdapter(getChildFragmentManager());
        Log.i(TAG, "onCreateView: Started");
        View view = inflater.inflate(R.layout.activity_control_panel, container, false);




        view.findViewById(R.id.onComplete).setOnClickListener(this);
        mViewPager =  view.findViewById(R.id.frame_container);
        Log.i(TAG, "onCreateView: Started");

        setupmViewPager(mViewPager);

        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseManager.sendEventType(MainActivity.eventsCollection, "New Patrol Setup", 16, "");
                Toast.makeText(getContext(), "DONE", Toast.LENGTH_LONG).show();
                close();
                Intent i = new Intent(AlarmReceiver.ACTION_END_TIME);
                getContext().sendBroadcast(i);
            }
        });
        mViewPager.setCurrentItem(1, true);

        return view;

    }

    public static KioskFragment getSiteDataFragment() {
        return siteDataFragment;
    }

    public static void setSiteDataFragment(KioskFragment siteDataFragment) {
        ControlPanelFragment.siteDataFragment = siteDataFragment;
    }

    public static KioskFragment getPointInfoFragment() {
        if(pointInfoFragment == null)

            return null;
        return pointInfoFragment;
    }
    public static KioskFragment getDurationInfoFragment() {
        if(durationInfoFragment == null)
            return null;
        return durationInfoFragment;
    }


    public void setDurationInfoFragment(KioskFragment durationInfoFragment) {
        this.durationInfoFragment = durationInfoFragment;
    }

    public void setPointInfo(KioskFragment fragment){
        this.pointInfoFragment = fragment;

    }

    public void close(){
        ControlPanelFragment.this.removeSelf();
    }

    private void  setupmViewPager(ViewPager viewPager){

        adapter = new MainActivity.MyAdapter(getChildFragmentManager());

        durationObj = new DurationInfo();
        pointsObj = new PointsInfo();
        siteDataObj = new SiteDataFragment();
        adapter.addFragment(new DurationInfo(), "DurationInfo");
        adapter.addFragment(new PointsInfo(), "Points Info");
        adapter.addFragment(new SiteDataFragment(), new SiteDataFragment().getTitle());
        adapter.addFragment(new TakePictureFragment(), new TakePictureFragment().getTitle());
        adapter.addFragment(new TakeAudioFragment(), new TakeAudioFragment().getTitle());
        adapter.addFragment(new NewLocalCamera(), new NewLocalCamera().getTitle());



        setDurationInfoFragment((KioskFragment)adapter.instantiateItem(mViewPager, 0));
        setPointInfo((KioskFragment)adapter.instantiateItem(mViewPager, 1));
        setSiteDataFragment((KioskFragment)adapter.instantiateItem(mViewPager, 2));

        viewPager.setAdapter(adapter);
    }


    public static void setViewPager(int fragmentNumber) {
        mViewPager.setCurrentItem(fragmentNumber);
    }

    @Override
    public String getTitle() {

        return title;
    }


    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.onComplete:
                close();
                break;
        }
    }
}

