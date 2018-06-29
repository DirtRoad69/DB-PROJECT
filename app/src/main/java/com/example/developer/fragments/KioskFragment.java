package com.example.developer.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.example.developer.fullpatrol.MainActivity;

public abstract class KioskFragment extends Fragment {
    private int requestCode = Integer.MAX_VALUE;

    public final void startFragment(KioskFragment fragment){
        ((MainActivity)this.getActivity()).addFragment(fragment);
    }

    public final void startFragmentForResult(KioskFragment fragment, int requestCode){
        this.requestCode = requestCode;
        ((MainActivity)this.getActivity()).addFragment(fragment);
    }


    public void removeSelf() {
        ((MainActivity)this.getActivity()).removeFragment(this.getTitle());
    }

    public void removeSelf(int resultCode, Bundle extraData) {
        ((MainActivity)this.getActivity()).removeFragment(this.getTitle(),resultCode, extraData);
    }


    public final void onResult(int resultCode, Bundle extraData) {
        this.onFragmentResult(this.requestCode, resultCode, extraData);
        this.requestCode = Integer.MAX_VALUE;
    }

    

    protected void onFragmentResult(int requestCode, int resultCode, Bundle extraData) {
    }


    public abstract String getTitle();

    public void onBackPressed(){ }

}
