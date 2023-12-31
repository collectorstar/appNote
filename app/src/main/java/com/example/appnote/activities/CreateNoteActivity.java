package com.example.appnote.activities;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.appnote.R;
import com.example.appnote.appsettings.Setting;
import com.example.appnote.threads.SubThread;
import com.example.appnote.entities.Note;
import com.example.appnote.entities.User;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity {
    private EditText inputNoteTitle, inputNoteSubtitle, inputNoteText;
    private View viewSubtitleIndicator;
    private ImageView imageNote,imageBack,imageSave,imageRemoveImage,imageRemoveWebURL;
    private TextView textWebURL,textMicellaneous,textDateTime;
    private LinearLayout layoutWebURL;
    private String selectedNoteColor,selectedImagePath,KeyNote;

    private AlertDialog dialogAddURL;
    private AlertDialog dialogDeleteNote;
    private Note alreadyAvailableNote;
    private ActivityResultLauncher<Intent> callbackSelectImg;
    private ActivityResultLauncher<String> callbackPermission;
    private DatabaseReference database;
    private StorageReference storage;
    private ConstraintLayout layoutSaveNote;
    private ProgressBar loadingSaveNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
        setupCallback();

        database = FirebaseDatabase.getInstance().getReferenceFromUrl(Setting.LinkDB);
        storage = FirebaseStorage.getInstance().getReference();

        imageBack = findViewById(R.id.imageBack);
        imageBack.setOnClickListener(view -> finish());

        inputNoteTitle = findViewById(R.id.inputNoteTitle);
        inputNoteSubtitle = findViewById(R.id.inputNoteSubTitle);
        inputNoteText = findViewById(R.id.inputNote);
        textDateTime = findViewById(R.id.textDateTime);
        viewSubtitleIndicator = findViewById(R.id.viewSubtitleIndicator);
        imageNote = findViewById(R.id.imageNote);
        textWebURL = findViewById(R.id.textWebURL);
        layoutWebURL = findViewById(R.id.layoutWebURL);
        layoutSaveNote = findViewById(R.id.layoutSaveNote);
        loadingSaveNote = findViewById(R.id.loadingSaveNote);


        textDateTime.setText(
                new SimpleDateFormat("EEEE, dd,MMMM yyyy HH:mm a", Locale.getDefault())
                        .format(new Date())
        );
        imageSave = findViewById(R.id.imageSave);
        imageSave.setOnClickListener(view -> saveNote());

        selectedNoteColor = "#333333";
        selectedImagePath = "";

        if (getIntent().getBooleanExtra("isViewOrUpdate", false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra("note");
            KeyNote = alreadyAvailableNote.getGenkey()+"";
            setViewOrUpdateNote();
        } else {
            SubThread.runSubThread(this, () -> KeyNote = database.child(User.EmailKey).child("notes").push().getKey());
        }
        imageRemoveWebURL = findViewById(R.id.imageRemoveWebURL);
        imageRemoveWebURL.setOnClickListener(view -> {
            textWebURL.setText(null);
            layoutWebURL.setVisibility(View.GONE);
        });

        imageRemoveImage = findViewById(R.id.imageRemoveImage);
        imageRemoveImage.setOnClickListener(view -> {
            imageNote.setImageBitmap(null);
            imageNote.setVisibility(View.GONE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
            selectedImagePath = "";
        });

        if (getIntent().getBooleanExtra("isFromQuickActions", false)) {
            String type = getIntent().getStringExtra("quickActionType");
            if (type != null) {
                if (type.equals("image")) {
                    selectedImagePath = getIntent().getStringExtra("imagePath");
                    imageNote.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath));
                    imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                } else if (type.equals("URL")) {
                    textWebURL.setText(getIntent().getStringExtra("URL"));
                    layoutWebURL.setVisibility(View.VISIBLE);
                }
            }
        }

        initMiscellaneous();
        setSubtitleIndicatorColor();
    }

    private void setupCallback() {
        callbackSelectImg = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                if (result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {
                            Glide.with(this).load(selectedImageUri).into(imageNote);
                            imageNote.setVisibility(View.VISIBLE);
                            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);

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

    private void setViewOrUpdateNote() {
        inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        inputNoteText.setText(alreadyAvailableNote.getNoteText());
        textDateTime.setText(alreadyAvailableNote.getDatetime());
        if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {

            Glide.with(getApplicationContext()).load(alreadyAvailableNote.getImagePath()).into(imageNote);
            imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            selectedImagePath = alreadyAvailableNote.getImagePath();
        }

        if (alreadyAvailableNote.getWebLink() != null && !alreadyAvailableNote.getWebLink().trim().isEmpty()) {
            textWebURL.setText(alreadyAvailableNote.getWebLink());
            layoutWebURL.setVisibility(View.VISIBLE);
        }
    }

    private void saveNote() {
        if (inputNoteTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note title can't be empty", Toast.LENGTH_SHORT).show();
            return;
        } else if (inputNoteSubtitle.getText().toString().trim().isEmpty()
                && inputNoteText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        final Note note = new Note();
        note.setTitle(inputNoteTitle.getText().toString());
        note.setSubtitle(inputNoteSubtitle.getText().toString());
        note.setNoteText(inputNoteText.getText().toString());
        note.setDatetime(textDateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);
        note.setGenkey(KeyNote);

        if (layoutWebURL.getVisibility() == View.VISIBLE) {
            note.setWebLink(textWebURL.getText().toString());
        }

        Handler handler = new Handler(Looper.getMainLooper());
        SubThread.runSubThread(this, () -> {
            setLoadingSaveNote(true);
            String path = User.EmailKey + "/" + KeyNote + "/image.jpg";
            if (note.getImagePath() != null && !note.getImagePath().equals("")) {
                StorageReference imageRef = storage.child(path);
                if (note.getImagePath().startsWith("http") || note.getImagePath().startsWith("https")) {
                    database.child(User.EmailKey).child("notes").child(KeyNote).setValue(note).addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            handler.post(() -> Toast.makeText(getApplicationContext(), "Something wrongs", Toast.LENGTH_SHORT).show());
                        } else {
                            Intent intent = new Intent();
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    });
                } else {
                    imageRef.putFile(Uri.fromFile(new File(note.getImagePath())))
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    imageRef.getDownloadUrl().addOnCompleteListener(task1 -> handler.post(() -> {
                                        if (task1.isSuccessful()) {
                                            note.setImagePath(task1.getResult().toString());

                                            database.child(User.EmailKey).child("notes").child(KeyNote).setValue(note).addOnCompleteListener(task2 -> {
                                                if (!task2.isSuccessful()) {
                                                    handler.post(() -> Toast.makeText(getApplicationContext(), "Something wrongs", Toast.LENGTH_SHORT).show());
                                                } else {
                                                    Intent intent = new Intent();
                                                    setResult(RESULT_OK, intent);
                                                    finish();
                                                }
                                            });
                                        } else {
                                            handler.post(() -> Toast.makeText(getApplicationContext(), "Upload image faild", Toast.LENGTH_SHORT).show());
                                            setLoadingSaveNote(false);
                                        }
                                    }));
                                } else {
                                    Log.e("loi upload image", task.getException().getMessage()+"");
                                    setLoadingSaveNote(false);
                                    handler.post(() -> Toast.makeText(getApplicationContext(), task.getException().getMessage()+"", Toast.LENGTH_SHORT).show());
                                }
                            });
                }
            } else {
                if (alreadyAvailableNote != null) {
                    if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().equals("")) {
                        storage.child(path).delete().addOnCompleteListener(task -> handler.post(() -> {
                            if (!task.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), "Something wrongs", Toast.LENGTH_SHORT).show();
                                setLoadingSaveNote(false);
                            }
                        }));
                    }
                }
                database.child(User.EmailKey).child("notes").child(KeyNote).setValue(note).addOnCompleteListener(task2 -> handler.post(() -> {
                    if (!task2.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), "Something wrongs", Toast.LENGTH_SHORT).show();
                        setLoadingSaveNote(false);
                    } else {
                        Log.e("tai notes", "tu trang create or update");
                        Intent intent = new Intent();
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }));
            }
        });
    }

    private void initMiscellaneous() {
        final LinearLayout layoutMiscellaneous = findViewById(R.id.layoutMiscellaneous);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous);
        textMicellaneous = layoutMiscellaneous.findViewById(R.id.textMicellaneous);
        textMicellaneous.setOnClickListener(view -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });

        final ImageView imageColor1 = layoutMiscellaneous.findViewById(R.id.imageColor1);
        final ImageView imageColor2 = layoutMiscellaneous.findViewById(R.id.imageColor2);
        final ImageView imageColor3 = layoutMiscellaneous.findViewById(R.id.imageColor3);
        final ImageView imageColor4 = layoutMiscellaneous.findViewById(R.id.imageColor4);
        final ImageView imageColor5 = layoutMiscellaneous.findViewById(R.id.imageColor5);

        layoutMiscellaneous.findViewById(R.id.viewColor1).setOnClickListener(view -> {
            selectedNoteColor = "#333333";
            imageColor1.setImageResource(R.drawable.ic_done);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMiscellaneous.findViewById(R.id.viewColor2).setOnClickListener(view -> {
            selectedNoteColor = "#FDBE3B";
            imageColor1.setImageResource(0);
            imageColor2.setImageResource(R.drawable.ic_done);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMiscellaneous.findViewById(R.id.viewColor3).setOnClickListener(view -> {
            selectedNoteColor = "#FF4842";
            imageColor1.setImageResource(0);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(R.drawable.ic_done);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMiscellaneous.findViewById(R.id.viewColor4).setOnClickListener(view -> {
            selectedNoteColor = "#3A52FC";
            imageColor1.setImageResource(0);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(R.drawable.ic_done);
            imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        layoutMiscellaneous.findViewById(R.id.viewColor5).setOnClickListener(view -> {
            selectedNoteColor = "#000000";
            imageColor1.setImageResource(0);
            imageColor2.setImageResource(0);
            imageColor3.setImageResource(0);
            imageColor4.setImageResource(0);
            imageColor5.setImageResource(R.drawable.ic_done);
            setSubtitleIndicatorColor();
        });

        if (alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()) {
            switch (alreadyAvailableNote.getColor()) {
                case "#FDBE3B":
                    layoutMiscellaneous.findViewById(R.id.viewColor2).performClick();
                    break;
                case "#FF4842":
                    layoutMiscellaneous.findViewById(R.id.viewColor3).performClick();
                    break;
                case "#3A52FC":
                    layoutMiscellaneous.findViewById(R.id.viewColor4).performClick();
                    break;
                case "#000000":
                    layoutMiscellaneous.findViewById(R.id.viewColor5).performClick();
                    break;
            }
        }

        layoutMiscellaneous.findViewById(R.id.layoutAddImage).setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

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
        layoutMiscellaneous.findViewById(R.id.layoutAddUrl).setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddURLDialog();
        });

        if (alreadyAvailableNote != null) {
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(view -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteDialog();
            });
        }
    }

    private void showDeleteDialog() {
        if (dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_delete_note,
                    findViewById(R.id.layoutDeleteNoteContainer)
            );
            builder.setView(view);
            dialogDeleteNote = builder.create();
            if (dialogDeleteNote.getWindow() != null) {
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            view.findViewById(R.id.textDeleteNote).setOnClickListener(view12 -> {

                Handler handler = new Handler(Looper.getMainLooper());
                SubThread.runSubThread(getApplicationContext(), () -> {
                    String path = User.EmailKey + "/" + KeyNote + "/image.jpg";
                    storage.child(path).delete();
                    database.child(User.EmailKey).child("notes").child(alreadyAvailableNote.getGenkey()).removeValue().addOnCompleteListener(task -> handler.post(() -> {
                        if (task.isSuccessful()) {
                            handler.post(() -> {
                                Intent intent = new Intent();
                                intent.putExtra("isNoteDeleted", true);
                                setResult(RESULT_OK, intent);
                                finish();
                            });
                        } else {
                            Toast.makeText(getApplicationContext(), "co loi xay ra", Toast.LENGTH_SHORT).show();
                        }
                    }));
                });

            });

            view.findViewById(R.id.textCancel).setOnClickListener(view1 -> {
                dialogDeleteNote.dismiss();
                dialogDeleteNote = null;
            });

            dialogDeleteNote.setOnCancelListener(dialog -> dialogDeleteNote = null);

        }
        dialogDeleteNote.show();


    }

    private void setSubtitleIndicatorColor() {
        GradientDrawable gradientDrawable = (GradientDrawable) viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        callbackSelectImg.launch(intent);
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

    private void setLoadingSaveNote(boolean isLoading){
        runOnUiThread(() -> {
            if(isLoading){
                layoutSaveNote.setEnabled(false);
                loadingSaveNote.setVisibility(View.VISIBLE);
                imageSave.setEnabled(false);
                imageBack.setEnabled(false);
                imageRemoveImage.setEnabled(false);
                imageRemoveWebURL.setEnabled(false);
                inputNoteTitle.setEnabled(false);
                inputNoteSubtitle.setEnabled(false);
                inputNoteText.setEnabled(false);
                textMicellaneous.setEnabled(false);
            }else {
                layoutSaveNote.setEnabled(true);
                loadingSaveNote.setVisibility(View.GONE);
                imageSave.setEnabled(true);
                imageBack.setEnabled(true);
                imageRemoveImage.setEnabled(true);
                imageRemoveWebURL.setEnabled(true);
                inputNoteTitle.setEnabled(true);
                inputNoteSubtitle.setEnabled(true);
                inputNoteText.setEnabled(true);
                textMicellaneous.setEnabled(true);
            }
        });
    }

    private void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_add_url,
                    findViewById(R.id.layoutAddUrlContainer)
            );
            builder.setView(view);

            dialogAddURL = builder.create();
            if (dialogAddURL.getWindow() != null) {
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(view12 -> {
                if (inputURL.getText().toString().trim().isEmpty()) {
                    Toast.makeText(CreateNoteActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                    Toast.makeText(CreateNoteActivity.this, "Enter valid URL", Toast.LENGTH_SHORT).show();
                } else {
                    textWebURL.setText(inputURL.getText().toString());
                    layoutWebURL.setVisibility(View.VISIBLE);
                    dialogAddURL.dismiss();
                    dialogAddURL = null;
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(view1 -> {

                dialogAddURL.dismiss();
                dialogAddURL = null;
            });

            dialogAddURL.setOnCancelListener(dialog -> dialogAddURL = null);

            dialogAddURL.show();
        }
    }
}