package com.example.developer.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.example.developer.fullpatrol.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class KioskFragmentAdapter extends FragmentStatePagerAdapter {
    ///private final List<KioskFragment> kioskFragments;

    public KioskFragmentAdapter(FragmentManager fm) {
        super(fm);
       // this.kioskFragments = new ArrayList<>();
    }

    @Override
    public Fragment getItem(int position) {
        return null;//kioskFragments.get(position);
    }

    @Override
    public int getCount() {
        return 0;//kioskFragments.size();
    }



}
