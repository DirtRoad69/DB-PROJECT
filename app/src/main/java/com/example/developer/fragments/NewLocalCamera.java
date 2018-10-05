package com.example.developer.fragments;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;


import com.example.developer.fullpatrol.FirebaseManager;
import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.fullpatrol.R;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;


public class NewLocalCamera extends KioskFragment {

    private Camera mCamera;
    private CameraPreview mPreview;
    private boolean fileUploaded;
    public String mCurrentPhotoPath;
    private boolean pressed;
    private FirebaseManager firebaseManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.camera_preview, container, false);
        fileUploaded = false;
        pressed = false;
        openCamera();
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(getContext(), mCamera);
        FrameLayout preview = view.findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        this.firebaseManager = FirebaseManager.getInstance();


        //take picture
        Button captureButton = view.findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //
                        // get an image from the camera
                        if(mCamera != null){
                            if(!pressed) {
                                mCamera.takePicture(null, null, mPicture);
                                fileUploaded = true;
                                firebaseManager.sendEventType(MainActivity.eventsCollection, "Picture Taken", 65, "");
                                pressed = true;
                            }else{
                                if(mCamera != null){
                                    Log.i("WSX", "onClick: camera and pressed ");
                                    mCamera.startPreview();
                                    pressed = false;
                                }
                            }
                        }else {
                            Log.i("WSX", "onClick: null camera");
                            openCamera();
                        }
                    }
                }
        );

        Button upload = view.findViewById(R.id.uploadImgToCloud);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImg();
            }
        });
        return view;
    }

    private void openCamera(){
        //boolean bool = checkCameraHardware(getContext());
        //if(bool){
            mCamera = getCameraInstance();
            if(mCamera != null){
                mCamera.setDisplayOrientation(90);
            }
        //}
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            openCamera();
        }
        else {
            if(mCamera != null){
                mCamera.release();
                Log.i("WSX", "Released: ");
            }

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mCamera != null){
           // mCamera.release();
        }
    }



    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {


        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile();
            if (pictureFile == null){
                Log.d("WSX", "Error creating media file, check storage permissions");
                return;

            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (Exception e) {
                Log.d("WSX", "File not found: " + e.getMessage());
            }
        }

        private File getOutputMediaFile() {
            File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/", "image" + new Date().getTime() + ".jpg");
            mCurrentPhotoPath = file.getAbsolutePath();
            Log.i("WSX", "createImageFile: "+mCurrentPhotoPath);
            return  file;
        }
    };
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
           c = Camera.open();
        }catch (Exception e){
            Log.i("WSX", "getCameraInstance: NO CAMERA WE FAILED");
        }
        return c;
    }
    private  boolean checkCameraHardware(Context context){
        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            return true;
        }else{
            return false;
        }
    }

    public String getImagePath() {
        return mCurrentPhotoPath;
    }

    //takes care of uploading the data to the server
    private void uploadImg() {
        if(fileUploaded && !getImagePath().isEmpty()){
            fileUploaded = false;

            Toast.makeText(getContext(), "Uploading Image To server...", Toast.LENGTH_LONG).show();
            Log.i("WSX", "uploadImg: "+getImagePath());
            String[] pathArray   = getImagePath().split("/");

            ((MainActivity)getActivity()).firebaseClientManager
                    .createImgRefLocationAndUpload(pathArray[pathArray.length-1], getImagePath(), getActivity().getApplicationContext());
        }else {
            Toast.makeText(getContext(), "NO IMAGE TO UPLOAD", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public String getTitle() {
        return "NewLocalCamera";
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.removeSelf();
    }
}
