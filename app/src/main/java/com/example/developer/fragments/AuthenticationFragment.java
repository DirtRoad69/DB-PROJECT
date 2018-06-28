package com.example.developer.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.developer.fullpatrol.R;

public class AuthenticationFragment extends KioskFragment {
    public static final String TITLE = "Authentication Fragment";

    @Override
    protected void proccessCommand(String command) {

    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.authentication_layout,container, false);

        return parentView;
    }
}
