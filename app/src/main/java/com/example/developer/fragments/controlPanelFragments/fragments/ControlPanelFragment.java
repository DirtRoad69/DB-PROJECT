package com.example.developer.fragments.controlPanelFragments.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.developer.fragments.KioskFragment;
import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.fullpatrol.R;

public class ControlPanelFragment extends KioskFragment {

    private static final String TAG = "ZAQ";
    private static KioskFragment durationInfoFragment;
    public static ViewPager mViewPager;
    private MainActivity.MyAdapter mMyadpter;
    private String title = "ControlPanelFragment";
    public static KioskFragment pointInfoFragment;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {



        mMyadpter = new MainActivity.MyAdapter(getChildFragmentManager());
        Log.i(TAG, "onCreateView: Started");
        View view = inflater.inflate(R.layout.activity_control_panel, container, false);





        mViewPager =  view.findViewById(R.id.frame_container);
        Log.i(TAG, "onCreateView: Started");

        setupmViewPager(mViewPager);
        mViewPager.setCurrentItem(1, true);


        return view;

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
    private void  setupmViewPager(ViewPager viewPager){

        MainActivity.MyAdapter adapter = new MainActivity.MyAdapter(getChildFragmentManager());

        adapter.addFragment(new DurationInfo(), "DurationInfo");
        adapter.addFragment(new PointsInfo(), "Points Info");
        adapter.addFragment(new SiteDataFragment(), new SiteDataFragment().getTitle());



        setDurationInfoFragment((KioskFragment)adapter.instantiateItem(mViewPager, 0));
        setPointInfo((KioskFragment)adapter.instantiateItem(mViewPager, 1));
        viewPager.setAdapter(adapter);
    }

    public static void setViewPager(int fragmentNumber) {
        mViewPager.setCurrentItem(fragmentNumber);
    }

    @Override
    public String getTitle() {

        return title;
    }
}

