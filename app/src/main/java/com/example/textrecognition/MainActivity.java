package com.example.textrecognition;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.IOException;
import java.util.ArrayList;
import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import java.lang.String;


public class MainActivity extends AppCompatActivity {


    private MaterialButton inputImageBtn;
    private MaterialButton recognizeTextBtn;
    private ShapeableImageView imageIv;
    private EditText recognizedTextEt;

    private static final string TAG ="MAIN_TAB";

    private Uri imageUri = null;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;

    private string[] cameraPermissions;
    private string[] storagePermissions;

    private ProgressDialog progressDialog;

    private TextRecognizer textRecognizer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recognizeTextBtn = findViewById(R.id.recognizeTextBtn);
        inputImageBtn = findViewById(R.id.inputImageBtn);
        imageIv = findViewById(R.id.imageIv);
        recognizedTextEt = findViewById(R.id.recognizedTextEt);

        cameraPermissions = new string[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new string[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please Wait");
        progressDialog.setCanceledOnTouchOutside(false);

        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        inputImageBtn.OnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showInputImageDialog();
            }
        });
        recognizeTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (imageUri == null) {
                    Toast.makeText(MainActivity.this, "Pick image first...", Toast.LENGTH_SHORT).show();
                } else {

                    recognizeTextFromImage();
                }
            }
        });
    }
   private void recognizeTextFromImage() {

        Log.d(TAG, "recognizeTextFromImage: ");
        progressDialog.setMessage("Pepraring image...");
        progressDialog.show();

        try {
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);

            progressDialog.setTitle("Recognizing text...");

            Task<Text> textTaskResult = textRecognizer.process(inputImage)
                    .addOnCanceledListener(new OnSuccessListener<Text>(){
                        @Override
                        public void onSuccess(Text text){

                            progressDialog.dismiss();
                            string recognizedText = text.getText();
                            Log.d(TAG, "onSuccess: recognizedText: "+recognizedText);


                            recognizedTextEt.setText(recognizedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            progressDialog.dismiss();
                            Log.e(TAG, "onFailure: ", e);
                            Toast.makeText(MainActivity.this, "Failed recognizing text due to"+e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });
        } catch (Exception e) {
            progressDialog.dismiss();
            Log.e(TAG, "recognizeTextFromImage: ", e);
            Toast.makeText(this, "Failed preparing image due to"+e.getMessage(), Toast.LENGTH_SHORT).show();

        }
    }
    private void showInputImageDialog() {

        PopupMenu popupMenu = new popupMenu(this, inputImageBtn);

        popupMenu.getMenu().add(Menu.NONE, 1 , 1 ,"CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2 , 2 ,"GALLERY");

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id==1){
                    Log.d(TAG, "onMenuItemClick: camera CLicked ...");
                    if (checkcameraPermission()){

                        pickImageCamera();

                    }
                    else{

                        repuestcameraPermission();

                    }

                }
                else if (id==2){

                    Log.d(TAG, "onMenuItemClick: Gallery Clicked");

                    if (checkStoragePermission()){

                        pickImageGallery();
                    }
                    else {

                        requestStoragePermission();
                    }
                }
                return true;
            }
        });


    }
    private void pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ");
        Intent intent = new Intent(Intent.ACTION_PICK);

        intent.setType("image/*");
        GalleryActivityResultLauncher.launch(intent);
    }
    private ActivityResultLauncher<Intent> GalleryActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        imageUri = data.getData();
                        imageIv.setImageURI(imageUri);


                    }
                    else{
                        Log.d(TAG, "onActivityResult: Cancelled");
                        Toast.makeText(MainActivity.this, "canceller..." , Toast.LENGTH_SHORT).show();

                    }
                    }
                });

    private void pickImageCamera(){

        Log.d(TAG, "pickImageCamera: ");
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);


    }
    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "onActivityResult: imageUri" + imageUri);
                        imageIv.setImageURI(imageUri);

                    }
                    else {
                        Log.d(TAG, "onActivityResult: Cancelled");
                        Toast.makeText(MainActivity.this, "cacelled", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private boolean checkStoragePermission(){

        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result;
        }
        private void requestStoragePermission() {
    ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
        }
    private boolean checkcameraPermission(){
        boolean cameraresult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean Storageresult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return cameraresult && Storageresult;
    }
    private void repuestcameraPermission(){

        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length>0){

                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean StorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && StorageAccepted){
                        pickImageCamera();
                    }
                    else{
                        Toast.makeText(this, "Camera & Storage permission are required",Toast.LENGTH_SHORT ).show();

                    }

                }
                else
                    Toast.makeText(this, "cancelled", Toast.LENGTH_SHORT).show();
            }
            break;
            case STORAGE_REQUEST_CODE:{

                if (grantResults.length>0){

                    boolean StorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (StorageAccepted){

                        pickImageGallery();
                    }
                    else{
                        Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }

    }
}




