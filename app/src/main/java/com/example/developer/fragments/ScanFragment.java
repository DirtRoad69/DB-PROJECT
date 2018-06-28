package com.example.developer.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.developer.fullpatrol.Display;
import com.example.developer.fullpatrol.R;
import com.google.zxing.Result;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScanFragment extends KioskFragment implements ZXingScannerView.ResultHandler {
    public static final String TITLE = "Scan Fragment";
    private FrameLayout contentFrame;
    private ZXingScannerView scannerView;
    private Dialog epicDialog;
    private ImageView closePopupNegativeImg;
    private Button btnRetry;
    private TextView titleTv;
    private TextView messageTv;
    private ImageView closePopupPositiveImg;
    private Button btnAccept;

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
        View parentView = inflater.inflate(R.layout.activity_scanner,container, false);
        this.contentFrame =  parentView.findViewById(R.id.content_frame);

        scannerView = new ZXingScannerView(getContext());
        scannerView.setFlash(!scannerView.getFlash());
        contentFrame.addView(scannerView);
        scannerView.setResultHandler(this);

        //Popup vars initialization
        epicDialog = new Dialog(getContext());

        return parentView;
    }

    public void showNegativePopup() {

        epicDialog.setContentView(R.layout.epic_popup_negative);
        closePopupNegativeImg = (ImageView)epicDialog.findViewById(R.id.closePopupNegativeImg);
        btnRetry = (Button) epicDialog.findViewById(R.id.btnRetry);
        closePopupNegativeImg.setImageResource(R.drawable.img_x);
        titleTv = (TextView)epicDialog.findViewById(R.id.titleTv);
        messageTv = (TextView)epicDialog.findViewById(R.id.messageTv);

        closePopupNegativeImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                epicDialog.dismiss();
                close(Activity.RESULT_CANCELED, null);
            }
        });
        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close(Activity.RESULT_CANCELED, null);
            }
        });

        epicDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        epicDialog.show();

    }

    private void close(int resultCode, Intent intent){
        this.returnToFragment(PatrolFragment.TITLE, resultCode, intent);
    }

    public void showPositivePopup(final Intent intent) {
        //epicDialog.setContentView(R.layout.epic_popup_positive);
        //epicDialog.setCancelable(false);
        View parentView = LayoutInflater.from(getContext()).inflate(R.layout.epic_popup_positive, null, false);
        closePopupPositiveImg = (ImageView)parentView.findViewById(R.id.closePopupPositiveImg);
        closePopupPositiveImg.setImageResource(R.drawable.img_x);
        titleTv = (TextView)parentView.findViewById(R.id.titleTv);
        messageTv = (TextView)parentView.findViewById(R.id.messageTv);
        btnAccept = (Button) parentView.findViewById(R.id.btnAccept);

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //epicDialog.dismiss();
                close(Activity.RESULT_OK, intent);
            }
        });

        closePopupPositiveImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // epicDialog.dismiss();
                close(Activity.RESULT_OK, intent);
            }
        });

        //epicDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        //epicDialog.show();

        this.contentFrame.addView(parentView);
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.i("RFC", "Reg");
        //IntentFilter filter = new IntentFilter(Display.BROD_KILL_SCANNER);
        //this.registerReceiver(broadcastReceiver, filter);
        scannerView.startCamera();
    }

    @Override
    public void onPause() {
        scannerView.stopCamera();
//        if(this.broadcastReceiver != null){
//            Log.i("RFC", "unReg");
//            this.unregisterReceiver(this.broadcastReceiver);
//        }
        super.onPause();
    }


    @Override
    public void handleResult(Result rawResult) {

        final String scan_Result = rawResult.getText();

        //Call back data to main activity
        final Intent intent = new Intent();

        intent.putExtra(PatrolFragment.CONTENT, rawResult.getText());


        Log.i("RFC", "scanned");
        if(scan_Result != null){
            Log.i("RFC", "scanned true");
            showPositivePopup(intent);
        }else{
            showNegativePopup();
            // finish();
        }

    }

}