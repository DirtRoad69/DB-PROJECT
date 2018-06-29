package com.example.developer.fragments;

import android.app.Activity;
import android.content.Intent;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.developer.fullpatrol.FirebaseManager;
import com.example.developer.fullpatrol.InputCollector;
import com.example.developer.fullpatrol.Interpol;
import com.example.developer.fullpatrol.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Map;

public class AuthenticationFragment extends KioskFragment implements View.OnFocusChangeListener, View.OnClickListener, KeyboardView.OnKeyboardActionListener {
    public static final String TITLE = "Authentication Fragment";
    public static final String ACCESS_TYPE_CONTROL = "TYPE_CONTROL", ACCESS_TYPE_ADMIN = "TYPE_ADMIN";
    public static final String ACCESS_TYPE = "Access Type";
    public static final String EXTRA_LOGGED_IN = "logged in";

    private FirebaseManager firebaseManager;
    private FirebaseAuth mAuth;
    private RelativeLayout relTemp;
    private EditText edtUsername,  edtPassword;
    private LinearLayout linProgess;
    private EditText selectedEdt;
    private KeyboardView keyboardView;
    private Keyboard keyboard;
    private boolean isCaps;


    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.firebaseManager = FirebaseManager.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View parentView = inflater.inflate(R.layout.authentication_layout,container, false);

        this.relTemp = parentView.findViewById(R.id.tmp_rel);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                relTemp.setVisibility(View.VISIBLE);

            }
        }, 700);


        this.edtUsername = parentView.findViewById(R.id.edt_username);
        this.edtPassword = parentView.findViewById(R.id.edt_password);
        this.linProgess = parentView.findViewById(R.id.lin_progress);
        this.selectedEdt = edtUsername;
        edtUsername.requestFocus();

        this.edtUsername.setOnFocusChangeListener(this);
        this.edtPassword.setOnFocusChangeListener(this);
        parentView.findViewById(R.id.btn_done).setOnClickListener(this);
        this.keyboardView = parentView.findViewById(R.id.kbv_input);
        this.keyboard = new Keyboard(getContext(), R.xml.qwerty);
        this.keyboardView.setPreviewEnabled(false);
        keyboardView.setKeyboard(keyboard);
        keyboardView.setOnKeyboardActionListener(this);

        isCaps = false;

        return parentView;
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
                    String accessType = AuthenticationFragment.this.getArguments().getString(ACCESS_TYPE);
                    if(accessType.equals(ACCESS_TYPE_ADMIN)){
                        final String userId = mAuth.getCurrentUser().getUid();
                        firebaseManager.getData("users", "owner", new FirebaseManager.DataCallback() {
                            @Override
                            public void onDataReceived(Map<String, Object> data) {
                                String ownerId = data.get("userId").toString();
                                if(userId.equals(ownerId)){
                                    Toast.makeText(getContext(), "Authentication Successful", Toast.LENGTH_SHORT).show();
                                    Bundle extraData = new Bundle();
                                    extraData.putBoolean(EXTRA_LOGGED_IN, true);

                                    AuthenticationFragment.this.removeSelf(Activity.RESULT_OK, extraData);
                                }else{
                                    Toast.makeText(getContext(), "Authentication Failed", Toast.LENGTH_SHORT).show();
                                }
                                linProgess.setVisibility(View.INVISIBLE);
                                relTemp.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onDataReceived(List<Map<String, Object>> data) {

                            }
                        });
                    }else {
                        Toast.makeText(getContext(), "Authentication SUCCESSFUL", Toast.LENGTH_SHORT).show();
                        Bundle extraData = new Bundle();
                        extraData.putBoolean(EXTRA_LOGGED_IN, true);

                        AuthenticationFragment.this.removeSelf(Activity.RESULT_OK, extraData);
                    }



                }else{
                    linProgess.setVisibility(View.INVISIBLE);
                    relTemp.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.removeSelf();
    }
}
