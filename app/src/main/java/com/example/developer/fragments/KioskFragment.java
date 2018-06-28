package com.example.developer.fragments;


import android.content.Intent;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.example.developer.fullpatrol.MainActivity;

public abstract class KioskFragment extends Fragment {
    public int requestCode = -1;
    public final void startFragment(String fragmentId){
        ((MainActivity)this.getActivity()).startFragment(fragmentId);
    }

    public final void startFragmentForResult(String fragmentId, int requestCode){
        Log.i("PatrolFragment", "startFragmentForResult: " + requestCode);

        ((MainActivity)this.getActivity()).startFragment(fragmentId, requestCode);
    }

    public final boolean sendCommand(String command, String fragmentId){
        return false;
    }

    protected abstract void proccessCommand(String command);

    public final void returnToFragment(String fragmentId, int resultCode,Intent intent){
        Log.i("PatrolFragment", "startFragmentForResult: " + requestCode + "|" + resultCode);
        ((MainActivity)this.getActivity()).returnToFragment(fragmentId, requestCode, resultCode, intent);
    }

    public final void returnToFragment(){
        ((MainActivity)this.getActivity()).returnToFragment();
    }

    public void onFragmentReturn(int requestCode, int resultCode, Intent intent){

    }

    public abstract String getTitle();

    public void closeFragment(String title) {
        ((MainActivity)getActivity()).closeFragment(title);
    }
}
