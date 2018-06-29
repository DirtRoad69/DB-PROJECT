package com.example.developer.fullpatrol;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.FrameLayout;

import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;

/**
 * Created by ASANDA on 2018/06/13.
 * for Kiosk
 */
public class LockableActivity extends AppCompatActivity {
    private HomeLocker homeLocker;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.homeLocker = new HomeLocker();
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
    }

    protected void onInterceptBackPress(){

    }

    public final void Lock(){
        this.homeLocker.Lock(this);
    }


    public void  setLockBackButton(boolean pLockBackButton){
        this.homeLocker.setLockBackButton(pLockBackButton);
    }

    public final void Unlock(){
        this.homeLocker.unLock();
    }

    @Override
    public void onBackPressed() {
        Log.i("RFC", "Bp");
    }

    private class HomeLocker {
        private OverlayDialog mOverlayDialog;
        private boolean isLocked = false;
        private boolean lockBackButton = true;

        public void  setLockBackButton(boolean pLockBackButton){
            this.lockBackButton = pLockBackButton;
        }

        public void Lock(Activity activity) {
            if (mOverlayDialog == null && !this.isLocked) {

                mOverlayDialog = new OverlayDialog(activity);
                if(!mOverlayDialog.isShowing())
                    mOverlayDialog.show();

                isLocked = true;
                Log.i("RECO", "LOCKED");
            }
        }

        public void unLock() {
            if(true)
                return;
            if (mOverlayDialog != null && isLocked) {
                mOverlayDialog.dismiss();
                mOverlayDialog = null;
                isLocked = false;
                Log.i("RECO", "UNLOCKED");
            }
        }

        public class OverlayDialog extends AlertDialog {

            public OverlayDialog(Activity activity) {
                super(activity, R.style.AppTheme);
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.type = TYPE_SYSTEM_ERROR;
                params.dimAmount = 0.0F; // transparent
                params.width = 0;
                params.height = 0;
                params.gravity = Gravity.BOTTOM;
                getWindow().setAttributes(params);
                getWindow().setFlags(FLAG_SHOW_WHEN_LOCKED | FLAG_NOT_TOUCH_MODAL, 0xffffff);
                setOwnerActivity(activity);
                setCancelable(false);

            }

            @Override
            public void onWindowFocusChanged(boolean hasFocus) {
                super.onWindowFocusChanged(hasFocus);
                if(!hasFocus) {
                    Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    this.getContext().sendBroadcast(closeDialog);
                }
            }

            @Override
            public boolean onKeyDown(int keyCode, KeyEvent event) {
                switch(keyCode) {
                    case KeyEvent.KEYCODE_VOLUME_UP:
                    case KeyEvent.KEYCODE_VOLUME_DOWN:
                    case KeyEvent.KEYCODE_VOLUME_MUTE:
                        return true;
                    case KeyEvent.KEYCODE_POWER:
                        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                        this.getContext().sendBroadcast(closeDialog);
                        return true;
                    case KeyEvent.KEYCODE_BACK:
                        LockableActivity lockedAct = (LockableActivity) this.getOwnerActivity();
                        lockedAct.onInterceptBackPress();
                        return false;

                }
                Log.i("RFC", "keyD");
                return super.onKeyDown(keyCode, event);
            }

            public final boolean dispatchTouchEvent(MotionEvent motionevent) {
                return true;
            }

            protected final void onCreate(Bundle bundle) {
                super.onCreate(bundle);
                FrameLayout framelayout = new FrameLayout(getContext());
                framelayout.setBackgroundColor(0);
                setContentView(framelayout);
            }
        }
    }
}
