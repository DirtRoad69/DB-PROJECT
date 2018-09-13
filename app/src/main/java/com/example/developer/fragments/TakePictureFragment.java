package com.example.developer.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.developer.fullpatrol.MainActivity;
import com.example.developer.fullpatrol.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TakePictureFragment extends KioskFragment implements View.OnClickListener {

    static final int REQUEST_IMAGE_CAPTURE = 555;
    private ImageView mImageView;
    String mCurrentPhotoPath;
    private Uri mImageUri;
    private Boolean fileUploaded;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.take_picture_fragment, container, false);
        mImageView = view.findViewById(R.id.iv_imagePreview);
        fileUploaded = false;
        view.findViewById(R.id.take_picture).setOnClickListener(this);
        view.findViewById(R.id.uploadImgToCloud).setOnClickListener(this);
        return view;
    }

    @Override
    public String getTitle() {
        return "TakePictureFragment";
    }

    private void dispatchTakePictureIntent() {
        Log.i("WSX", "dispatchTakePictureIntent: take picture called");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(((MainActivity)getActivity()).getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.i("WSX", "create file to take picture!");
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.i("WSX", "Can't create file to take picture!");
                Toast.makeText(getContext(), "Please check SD card! Image shot is impossible!", Toast.LENGTH_LONG).show();

            }
            if (photoFile != null) {
                Log.i("WSX", "create file to take picture photoURI!");
                mImageUri = Uri.fromFile(photoFile);/*FileProvider.getUriForFile(getContext(),
                        "com.example.developer.controldialogs.fileprovider",
                        photoFile)*/
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, setImageUri());
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = ((MainActivity)getActivity()).getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        File tempDir = Environment.getExternalStorageDirectory();
        tempDir = new File(tempDir.getAbsolutePath()+"/.temp/");
        if(!tempDir.exists())
        {
            tempDir.mkdirs();
        }
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                tempDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
       // mCurrentPhotoPath = image.getAbsolutePath();

        return image;
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.take_picture:
                Log.i("WSX", "onClick: take picture");
                dispatchTakePictureIntent();
                break;
            case R.id.uploadImgToCloud:
                uploadImg();
                break;
        }
    }

    private void uploadImg() {
        if(fileUploaded){
            fileUploaded = false;

            Toast.makeText(getContext(), "Uploading Image To server...", Toast.LENGTH_LONG).show();
            Log.i("WSX", "uploadImg: "+getImagePath());
            String[] pathArray = getImagePath().split("/");

            ((MainActivity)getActivity()).firebaseClientManager
                    .createImgRefLocationAndUpload(pathArray[pathArray.length-1], getImagePath(), getActivity().getApplicationContext());
        }else {
            Toast.makeText(getContext(), "NO IMAGE TO UPLOAD", Toast.LENGTH_LONG).show();
        }
    }



    public Bitmap decodeFile(String path) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            // The new size we want to scale to
            final int REQUIRED_SIZE = 70;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE)
                scale *= 2;

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeFile(path); //.decodeFile(path, o2);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;

    }

    public Uri setImageUri() {
        // Store image in dcim
        File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/", "image" + new Date().getTime() + ".jpg");
        Uri imgUri = Uri.fromFile(file);
        this.mCurrentPhotoPath = file.getAbsolutePath();
        Log.i("WSX", "createImageFile: "+mCurrentPhotoPath);
        return imgUri;
    }


    public String getImagePath() {
        return mCurrentPhotoPath;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            //Uri extras = data.getData();

            Log.i("WSX", "onActivityResult: take picture");
            //Bitmap imageBitmap = (Bitmap) extras.get(MediaStore.EXTRA_OUTPUT);
            mImageView.setImageBitmap(decodeFile(getImagePath()));
            fileUploaded = true;

        }else{
            Log.i("WSX", "onActivityResult: take FAILED");
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
