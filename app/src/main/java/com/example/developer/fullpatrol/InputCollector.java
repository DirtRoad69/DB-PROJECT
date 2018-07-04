package com.example.developer.fullpatrol;

import android.app.Activity;
import android.content.Intent;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Map;

public class InputCollector extends LockableActivity implements View.OnClickListener, KeyboardView.OnKeyboardActionListener, View.OnFocusChangeListener {
    public static final String EXTRA_USERNAME = "EXTRA_USERNAME";
    public static final String EXTRA_PASSWORD = "EXTRA_PASSWORD";
    public static final String ACCESS_TYPE_CONTROL = "TYPE_CONTROL", ACCESS_TYPE_ADMIN = "TYPE_ADMIN";
    public static final String ACCESS_TYPE = "ACCESS_TYPE";
    public static final String EXTRA_LOGGED_IN = "LOGGED_IN";
    private EditText edtUsername, edtPassword, selectedEdt;
    private LinearLayout linProgess;
    private boolean isCaps;
    private Keyboard keyboard;
    private KeyboardView keyboardView;
    private RelativeLayout relTemp;


    //Auth stuff
    private FirebaseAuth mAuth;
    private FirebaseManager firebaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.Lock();
        this.setLockBackButton(false);

        //firebase init
        firebaseManager = FirebaseManager.getInstance();
        mAuth = FirebaseAuth.getInstance();



        setContentView(R.layout.authentication_layout);

        this.relTemp = this.findViewById(R.id.tmp_rel);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                relTemp.setVisibility(View.VISIBLE);

            }
        }, 500);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        this.edtUsername = this.findViewById(R.id.edt_username);
        this.edtPassword = this.findViewById(R.id.edt_password);
        this.linProgess = this.findViewById(R.id.lin_progress);
        this.selectedEdt = edtUsername;
        edtUsername.requestFocus();

        this.edtUsername.setOnFocusChangeListener(this);
        this.edtPassword.setOnFocusChangeListener(this);
        this.findViewById(R.id.btn_done).setOnClickListener(this);
        this.keyboardView = this.findViewById(R.id.kbv_input);
        this.keyboard = new Keyboard(this, R.xml.qwerty);
        this.keyboardView.setPreviewEnabled(false);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);

        isCaps = false;
    }

    @Override
    protected void onResume() {
        Interpol.getInstance().setOutOfMainActivity(true);
        super.onResume();
    }

    @Override
    public boolean onSupportNavigateUp() {

        Interpol.getInstance().setOutOfMainActivity(false);
        this.finish();
        return super.onSupportNavigateUp();
    }

    public void checkInputAndSignIn(){
        String email = edtUsername.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();


        if(email.isEmpty()){
            edtUsername.setHint("email required");
            return;
        }
        if(password.isEmpty()){
            edtPassword.setHint("password required");
            return;
        }
        signInUser(email, password);
    }

    public void signInUser(String email, String password){

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if(task.isSuccessful()){
                    //go to main

                    if(getIntent().getStringExtra(ACCESS_TYPE).equals(ACCESS_TYPE_ADMIN)){
                        final String userId = mAuth.getCurrentUser().getUid();
                        firebaseManager.getData("users", "owner", new FirebaseManager.DataCallback() {
                            @Override
                            public void onDataUpdated(Map<String, Object> data) {

                            }

                            @Override
                            public void onDataReceived(Map<String, Object> data) {
                                String ownerId = data.get("userId").toString();
                                if(userId.equals(ownerId)){
                                    Toast.makeText(InputCollector.this, "Authentication Successful", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent();
                                    intent.putExtra(EXTRA_LOGGED_IN, true);
                                    setResult(Activity.RESULT_OK, intent);

                                    InputCollector.this.Unlock();
                                    Interpol.getInstance().setOutOfMainActivity(false);
                                    InputCollector.this.finish();
                                }else{
                                    Toast.makeText(InputCollector.this, "Authentication Failed", Toast.LENGTH_SHORT).show();
                                }
                                linProgess.setVisibility(View.INVISIBLE);
                                relTemp.setVisibility(View.VISIBLE);

                            }

                            @Override
                            public void onDataReceived(List<Map<String, Object>> data) {

                            }
                        });
                    }else {
                        Toast.makeText(InputCollector.this, "LOGIN SUCCESSFUL", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent();
                        intent.putExtra(EXTRA_LOGGED_IN, true);
                        setResult(Activity.RESULT_OK, intent);

                        InputCollector.this.Unlock();
                        Interpol.getInstance().setOutOfMainActivity(false);

                        InputCollector.this.finish();
                        linProgess.setVisibility(View.INVISIBLE);
                        relTemp.setVisibility(View.VISIBLE);

                    }



                }else{
                    linProgess.setVisibility(View.INVISIBLE);
                    relTemp.setVisibility(View.VISIBLE);
                    Toast.makeText(InputCollector.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        });

    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_done:
                this.done();
                break;
        }
    }


    @Override
    protected void onPause() {
        Interpol.getInstance().setOutOfMainActivity(false);
        super.onPause();
    }

    @Override
    public void onPress(int primaryCode) {
        Log.i("RCF", "s");
    }

    @Override
    public void onRelease(int primaryCode) {
        Log.i("RCF", "b");

    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        if (selectedEdt != null) {
            switch (primaryCode) {
                case Keyboard.KEYCODE_DELETE:
                    String text = selectedEdt.getText().toString();
                    if(!text.isEmpty()){
                        int pos = selectedEdt.getSelectionStart(), end = selectedEdt.getSelectionEnd();
                        if (pos > 0){
                            String textNew = selectedEdt.getText().delete(pos - 1, end).toString();
                            selectedEdt.setText(textNew);
                            selectedEdt.setSelection(pos - 1);
                        }else{
                            String textNew = selectedEdt.getText().delete(0, end).toString();
                            selectedEdt.setText(textNew);
                            selectedEdt.setSelection(0);
                        }
                    }
                    break;
                case Keyboard.KEYCODE_SHIFT:
                    isCaps = !isCaps;
                    keyboard.setShifted(isCaps);
                    keyboardView.invalidateAllKeys();
                    break;
                case Keyboard.KEYCODE_DONE:
                    this.done();
                    break;
                default:
                    char code = (char) primaryCode;
                    if (Character.isLetter(code) && isCaps) {
                        code = Character.toUpperCase(code);
                    }
                    String textToInsert = String.valueOf(code);
                    int start = Math.max(selectedEdt.getSelectionStart(), 0);
                    int end = Math.max(selectedEdt.getSelectionEnd(), 0);
                    selectedEdt.getText().replace(Math.min(start, end), Math.max(start, end),
                            textToInsert, 0, textToInsert.length());
            }


        }
    }

        @Override
        public void onText (CharSequence text){


        }

        @Override
        public void swipeLeft () {

        }

        @Override
        public void swipeRight () {

        }

        @Override
        public void swipeDown () {

        }

        @Override
        public void swipeUp () {

        }


        private void done () {
            relTemp.setVisibility(View.GONE);
            linProgess.setVisibility(View.VISIBLE);
            checkInputAndSignIn();
        }


    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        switch(v.getId()){
            case R.id.edt_username:
                selectedEdt = edtUsername;
                break;
            case R.id.edt_password:
                selectedEdt = edtPassword;
                break;
        }
    }
}