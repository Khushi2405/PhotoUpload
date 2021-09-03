package com.example.photoupload;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    public static final int cameraCaptureCode = 200;
    public static final int galleryRequestCode = 300;
    public static final int allPermsCode = 1000;
    ImageView img;
    TextView capture, browse, upload;
    String currentPhotoPath;
    StorageReference storageReference;
    Uri contentUri;
    String fileName;
    Bitmap bitmap;
    ProgressDialog p;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = (ImageView)findViewById(R.id.image);
        capture = (TextView)findViewById(R.id.Capture);
        browse = (TextView)findViewById(R.id.Browse);
        upload = (TextView)findViewById(R.id.Upload);


        storageReference = FirebaseStorage.getInstance().getReference();

        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkCameraPermission();
            }
        });
        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //select picture from gallery from https://stackoverflow.com/questions/10473823/android-get-image-from-gallery-into-imageview
                Intent gallery = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, galleryRequestCode);
            }
        });
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*p =ProgressDialog.show(MainActivity.this," ","wait",true);
                Thread t = new Thread(){
                    public void run(){
                        super.run();
                        try{
                            uploadToFireBase(fileName,contentUri);
                        }
                        catch (Exception e){}
                        finally{
                            p.dismiss();
                        }
                    }
                };
                t.start();

                 */
                Toast.makeText(MainActivity.this, "Uploading Image please wait....", Toast.LENGTH_LONG).show();
                uploadToFireBase(fileName,contentUri);

            }
        });
    }


    //    check whether camera permission granted from https://developer.android.com/training/permissions/requesting
    private void checkCameraPermission() {
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        // index 0 = camera, index 1 = readStorage , index 2 = write Storage

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[0]) == PackageManager.PERMISSION_GRANTED

                && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[1]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this.getApplicationContext(), permissions[2]) == PackageManager.PERMISSION_GRANTED) {

            dispatchTakePictureIntent();
        }
        else {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, allPermsCode);
        }
    }

    // if user denies permission request permission from https://developer.android.com/training/permissions/requesting
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case allPermsCode:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    dispatchTakePictureIntent();
                } else {
                    Toast.makeText(this, "Camera Permission is Required to Use camera.", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if request code is cameraCaptureCode i.e. Capture button is pressed
        if (requestCode == cameraCaptureCode && resultCode == RESULT_OK) {
            File f = new File(currentPhotoPath);
            img.setImageURI(Uri.fromFile(f));
            Log.d("tag", "Absolute Url of Image is " + Uri.fromFile(f));

            //add captured picture to gallery from https://developer.android.com/training/camera/photobasics
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            contentUri = Uri.fromFile(f);
            fileName = f.getName();
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
        }
        //if request code is galleryCaptureCode i.e. Browse button is pressed
        else if(requestCode == galleryRequestCode && resultCode == RESULT_OK){
            contentUri = data.getData();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp +"."+getFileExt(contentUri);
            Log.d("tag", "onActivityResult: Gallery Image Uri:  " +  imageFileName);
            img.setImageURI(contentUri);
            fileName = imageFileName;

        }

    }

    private void uploadToFireBase(String name, Uri uri){

        //compress image from https://developer.android.com/reference/android/graphics/Bitmap#compress(android.graphics.Bitmap.CompressFormat,%20int,%20java.io.OutputStream)
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        StorageReference str = storageReference.child("images/" + name);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 0, bytes);
        String path = MediaStore.Images.Media.insertImage(this.getContentResolver(),bitmap,name,null);
        uri = Uri.parse(path);

        //upload image URL to database from
        str.putFile(uri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                str.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Log.d("tag" , "On Success : URL is " + uri.toString());
                        Toast.makeText(MainActivity.this, "Retrieving image from Firebase....", Toast.LENGTH_LONG).show();
                        Picasso.get().load(uri).into(img);
                    }
                });
                Toast.makeText(MainActivity.this, "Upload Success", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener((new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Upload Failed", Toast.LENGTH_SHORT).show();
            }
        }));

    }

    //to determine the type of file from https://stackoverflow.com/questions/8589645/how-to-determine-mime-type-of-file-in-android
    private String getFileExt(Uri contentUri) {
        ContentResolver c = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalCacheDir();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(this, "Error in creating file", Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.photoupload.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, cameraCaptureCode);
            }
        }
    }




}


