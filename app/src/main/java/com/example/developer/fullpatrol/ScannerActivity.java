package com.example.developer.fullpatrol;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.Result;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class ScannerActivity  extends LockableActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView scannerView;
    private Calendar calendar;
    private BroadcastReceiver broadcastReceiver;
    public static final String TAG = "FIRESTORE";
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    ViewGroup contentFrame;
    //Popup var
    Dialog epicDialog;
    Button positivePopupBtn, negativePopupBtn, btnAccept, btnRetry;
    TextView titleTv, messageTv;
    ImageView closePopupPositiveImg, closePopupNegativeImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.Lock();
        this.setLockBackButton(false);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_scanner);
        this.broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(Display.BROD_KILL_SCANNER)){
                    ScannerActivity.this.scannerView.stopCamera();
                    //ScannerActivity.this.close();
                }
            }
        };


         this.contentFrame = (ViewGroup) findViewById(R.id.content_frame);

        scannerView = new ZXingScannerView(this);
        scannerView.setFlash(!scannerView.getFlash());
        contentFrame.addView(scannerView);

        //Popup vars initialization
        epicDialog = new Dialog(this);




    }


    @Override
    public void handleResult(Result result) {

    }
}
