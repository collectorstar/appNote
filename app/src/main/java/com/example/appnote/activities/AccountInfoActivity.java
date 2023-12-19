package com.example.appnote.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.appnote.R;
import com.example.appnote.appsettings.Setting;
import com.example.appnote.entities.User;
import com.example.appnote.threads.SubThread;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.makeramen.roundedimageview.RoundedImageView;

import java.io.File;

public class AccountInfoActivity extends AppCompatActivity {

    private EditText edName,edEmail;
    private ImageView accountInfoSave,imageBack;
    private RoundedImageView image;
    private FirebaseAuth auth;
    private DatabaseReference database;
    private StorageReference storage;
    private FirebaseUser user;
    private ActivityResultLauncher<Intent> callbackSelectImg;
    private ActivityResultLauncher<String> callbackPermission;
    private String selectedImagePath;
    private String path = User.EmailKey + "/" + "avatar.jpg";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_info);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        database = FirebaseDatabase.getInstance().getReferenceFromUrl(Setting.LinkDB);
        storage = FirebaseStorage.getInstance().getReference();
        edName = findViewById(R.id.edName);
        edName.setText(user.getDisplayName());
        edEmail = findViewById(R.id.Email);
        edEmail.setText(User.Email);
        image = findViewById(R.id.image);
        if (user.getPhotoUrl() == null){
            selectedImagePath = "";
        }else {
            selectedImagePath = user.getPhotoUrl().toString();
            Glide.with(this).load(selectedImagePath).into(image);
        }
        accountInfoSave = findViewById(R.id.accountInfoSave);
        imageBack = findViewById(R.id.imageBack);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

        accountInfoSave.setOnClickListener(view -> {
            String name = edName.getText().toString().trim();

            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();

            SubThread.runSubThread(getApplicationContext(), () -> {
                if(selectedImagePath.startsWith("http") || selectedImagePath.startsWith("https") || selectedImagePath == ""){
                    SubThread.runSubThread(getApplicationContext(), () -> user.updateProfile(profileUpdates).addOnCompleteListener(task -> runOnUiThread(() -> {
                        if(task.isSuccessful()){
                            Toast.makeText(getApplicationContext(),"update account info successfully!",Toast.LENGTH_SHORT).show();
                        }else {
                            Toast.makeText(getApplicationContext(),"update account fail!",Toast.LENGTH_SHORT).show();
                        }
                    })));
                }else {
                    storage.child(path).putFile(Uri.fromFile(new File(selectedImagePath))).addOnCompleteListener(task -> {
                        if(task.isSuccessful()){
                            storage.child(path).getDownloadUrl().addOnCompleteListener(task12 -> runOnUiThread(() -> {
                                if(task12.isSuccessful()){
                                    UserProfileChangeRequest profileUpdates1 = new UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .setPhotoUri(Uri.parse(task12.getResult().toString()))
                                            .build();

                                    database.child(User.EmailKey).child("avatar").setValue(task12.getResult().toString()).addOnCompleteListener(task13 -> {
                                        if(task13.isSuccessful()){
                                            user.updateProfile(profileUpdates1).addOnCompleteListener(task1 -> {
                                                if(task1.isSuccessful()){
                                                    Toast.makeText(getApplicationContext(),"update account info successfully!",Toast.LENGTH_SHORT).show();
                                                }else {
                                                    Toast.makeText(getApplicationContext(),"update account fail!",Toast.LENGTH_SHORT).show();
                                                    Log.e("update avatar",task1.getException().getMessage());
                                                }
                                            });
                                        }else {
                                            Toast.makeText(getApplicationContext(),"update account fail!",Toast.LENGTH_SHORT).show();
                                            Log.e("update avatar",task13.getException().getMessage());
                                        }
                                    });
                                }else {
                                    Toast.makeText(getApplicationContext(),"update account fail!",Toast.LENGTH_SHORT).show();
                                    Log.e("update avatar",task12.getException().getMessage());
                                }
                            }));
                        }else {
                            Toast.makeText(getApplicationContext(),"update account fail!",Toast.LENGTH_SHORT).show();
                            Log.e("update avatar",task.getException().getMessage());
                        }
                    });
                }
            });

        });

        setupCallback();

        imageBack.setOnClickListener(view -> finish());

        image.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT < 33) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    callbackPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                } else {
                    selectImage();
                }
            } else {
                if (
                        ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED
                ) {
                    callbackPermission.launch(Manifest.permission.READ_MEDIA_IMAGES);
                } else {
                    selectImage();
                }
            }
        });
    }

    private void setupCallback() {
        callbackSelectImg = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                if (result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {
                            Glide.with(this).load(selectedImageUri).into(image);
                            selectedImagePath = getPathFromUri(selectedImageUri);

                        } catch (Exception ex) {
                            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });

        callbackPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                selectImage();
            } else {
                Toast.makeText(getApplicationContext(), "Please accept permission", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver()
                .query(contentUri, null, null, null, null);
        if (cursor == null) {
            filePath = contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            callbackSelectImg.launch(intent);
        }
    }
}