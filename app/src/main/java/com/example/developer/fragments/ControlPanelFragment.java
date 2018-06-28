package com.example.developer.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.developer.fullpatrol.R;

public class ControlPanelFragment extends KioskFragment {
    public static final String TITLE = "Control Panel Fragment";

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
        View parentView = inflater.inflate(R.layout.activity_control_panel,container, false);

        return parentView;
    }
}
