package com.example.appnote.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.appnote.R;
import com.example.appnote.adapters.NavAdapter;
import com.example.appnote.adapters.NotesAdapter;
import com.example.appnote.appsettings.Setting;
import com.example.appnote.threads.SubThread;
import com.example.appnote.entities.Note;
import com.example.appnote.entities.User;
import com.example.appnote.listeners.NotesListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {

    public static final int REQUEST_CODE_ADD_NOTE = 1;
    public static final int REQUEST_CODE_UPDATE_NOTE = 2;
    public static final int REQUEST_CODE_SHOW_NOTES = 3;
    private RecyclerView notesRecyclerView, navsRecyclerView;
    private List<Note> noteList;
    private NotesAdapter notesAdapter;
    private int noteClickedPosition = -1;
    private AlertDialog dialogAddURL,dialogChangePassword;
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private NavAdapter navAdapter;
    private FirebaseAuth auth;
    private ActivityResultLauncher<Intent> callbackCreate, callbackUpdate, callbackSelectImg;
    private ActivityResultLauncher<String> callbackPermission;
    private DatabaseReference database;
    private ProgressBar loadingListNote;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        auth = FirebaseAuth.getInstance();
        setupNav();
        setupCallback();
        loadingListNote = findViewById(R.id.loadingListNote);
        database = FirebaseDatabase.getInstance().getReferenceFromUrl(Setting.LinkDB);

        ImageView imageAddNoteMain = findViewById(R.id.imageAddNoteMain);
        imageAddNoteMain.setOnClickListener(view -> callbackCreate.launch(new Intent(getApplicationContext(), CreateNoteActivity.class)));

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        notesRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        );

        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, getApplicationContext(), this);
        notesRecyclerView.setAdapter(notesAdapter);

        getNotes(REQUEST_CODE_SHOW_NOTES, false);

        EditText inputSearch = findViewById(R.id.inputSearch);
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable e) {
                if (noteList.size() != 0) {
                    notesAdapter.searchNotes(e.toString());
                }
            }
        });

        findViewById(R.id.imageAddNote).setOnClickListener(view -> callbackCreate.launch(new Intent(getApplicationContext(), CreateNoteActivity.class)));

        findViewById(R.id.imageAddImage).setOnClickListener(view -> {
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

        findViewById(R.id.imageAddWebLink).setOnClickListener(view -> showAddURLDialog());


    }

    private void setupCallback() {
        callbackCreate = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                getNotes(REQUEST_CODE_ADD_NOTE, false);
            }
        });

        callbackUpdate = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                boolean isNoteDeleted = result.getData().getBooleanExtra("isNoteDeleted", false);
                getNotes(REQUEST_CODE_UPDATE_NOTE, isNoteDeleted);
            }
        });

        callbackSelectImg = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Uri selectedImageUri = result.getData().getData();
                if (selectedImageUri != null) {
                    try {
                        String selectedImagePath = getPathFromUri(selectedImageUri);
                        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                        intent.putExtra("isFromQuickActions", true);
                        intent.putExtra("quickActionType", "image");
                        intent.putExtra("imagePath", selectedImagePath);
                        callbackCreate.launch(intent);
                    } catch (Exception ex) {
                        Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
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

    public void setupNav() {
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawerLayout);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        toolbar.setNavigationOnClickListener(view -> drawerLayout.openDrawer(GravityCompat.START));

        navsRecyclerView = findViewById(R.id.navsRecyclerView);
        navAdapter = new NavAdapter(this, (position, itemName) -> {
            switch (itemName) {
                case "Account Info":
                    break;
                case "Change Password":
                    changePassword();
                    break;
                case "Logout":
                    auth.signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                    break;
            }
        });
        navsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        navsRecyclerView.setAdapter(navAdapter);
    }

    private void changePassword() {
        if(dialogChangePassword == null){
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View view = LayoutInflater.from(this).inflate(
                    R.layout.layout_change_pasword,
                    findViewById(R.id.layoutChangePasswordContainer)
            );
            builder.setView(view);

            dialogChangePassword = builder.create();
            if (dialogChangePassword.getWindow() != null) {
                dialogChangePassword.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            EditText inputNewPassword = view.findViewById(R.id.inputNewPassword);
            EditText inputOldPassword = view.findViewById(R.id.inputOldPassword);
            inputNewPassword.requestFocus();

            view.findViewById(R.id.textChange).setOnClickListener(view12 -> {
                if (inputNewPassword.getText().toString().length() < 6) {
                    Toast.makeText(getApplicationContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                    inputNewPassword.setError("Password must be at least 6 characters");
                    inputNewPassword.requestFocus();
                } else if (inputNewPassword.getText().toString().contains(" ")) {
                    Toast.makeText(getApplicationContext(), "Password contains spaces", Toast.LENGTH_SHORT).show();
                    inputNewPassword.setError("Password contains spaces");
                    inputNewPassword.requestFocus();
                }else {
                    Handler handler = new Handler(Looper.getMainLooper());
                    SubThread.runSubThread(getApplicationContext(), () -> {
                        FirebaseUser user = auth.getCurrentUser();
                        AuthCredential credential = EmailAuthProvider.getCredential(User.Email,inputOldPassword.getText().toString());
                        user.reauthenticate(credential).addOnCompleteListener(task -> {
                            if(task.isSuccessful()){
                                runOnUiThread(() -> {
                                    dialogChangePassword.dismiss();
                                    dialogChangePassword = null;
                                    Toast.makeText(getApplicationContext(),"Request Change Password is sending",Toast.LENGTH_SHORT).show();
                                });
                                user.updatePassword(inputNewPassword.getText().toString()).addOnCompleteListener(task1 -> handler.post(() -> {
                                    if(task1.isSuccessful()){
                                        Toast.makeText(getApplicationContext(),"Update password successfully!",Toast.LENGTH_SHORT).show();
                                    }else {
                                        Toast.makeText(getApplicationContext(),"Update password is wrong!",Toast.LENGTH_SHORT).show();
                                        Log.e("update password", task1.getException().getMessage());
                                    }
                                }));
                            }else {
                              runOnUiThread(() -> {
                                  Toast.makeText(getApplicationContext(),"Old passwrod is not correct!",Toast.LENGTH_SHORT).show();
                                  inputOldPassword.setError("Old passwrod is not correct!");
                                  inputOldPassword.requestFocus();
                              });
                            }
                        });

                    });
                }

            });

            view.findViewById(R.id.textCancel).setOnClickListener(view1 -> {
                dialogChangePassword.dismiss();
                dialogChangePassword = null;
            });

            dialogChangePassword.setOnCancelListener(dialog -> dialogChangePassword = null);

            dialogChangePassword.show();
        }
    }
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (intent.resolveActivity(getPackageManager()) != null) {
            callbackSelectImg.launch(intent);
        }
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

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra("isViewOrUpdate", true);
        intent.putExtra("note", note);
        callbackUpdate.launch(intent);
    }

    private void getNotes(final int requestCode, boolean isNoteDeleted) {
        try {
            Handler handler = new Handler(Looper.getMainLooper());
            SubThread.runSubThread(getApplicationContext(), () -> {
                List<Note> notes = new ArrayList<>();

                this.setLoadingListNote(true);
                database.child(User.EmailKey).child("notes").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot noteSnapshot : snapshot.getChildren()) {
                            Note note = noteSnapshot.getValue(Note.class);
                            notes.add(0, note);
                        }
                        handler.post(() -> {
                            if (requestCode == REQUEST_CODE_SHOW_NOTES) {
                                noteList.addAll(notes);
                                notesAdapter.notifyDataSetChanged();
                            } else if (requestCode == REQUEST_CODE_ADD_NOTE) {
                                noteList.add(0, notes.get(0));
                                notesAdapter.notifyItemInserted(0);
                                notesRecyclerView.smoothScrollToPosition(0);
                            } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                                noteList.remove(noteClickedPosition);
                                if (isNoteDeleted) {
                                    notesAdapter.notifyItemRemoved(noteClickedPosition);
                                } else {
                                    noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                                    notesAdapter.notifyItemChanged(noteClickedPosition);
                                }
                            }
                            setLoadingListNote(false);
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        handler.post(() -> Toast.makeText(getApplicationContext(), "Co loi xay ra", Toast.LENGTH_SHORT).show());
                    }
                });

            });
        } catch (Exception ex) {
            Log.e("loi comeback", ex.getMessage());
        }

    }

    private void setLoadingListNote(boolean loading) {

        runOnUiThread(() -> {
            if (loading) {
                notesRecyclerView.setVisibility(View.GONE);
                loadingListNote.setVisibility(View.VISIBLE);
            } else {
                notesRecyclerView.setVisibility(View.VISIBLE);
                loadingListNote.setVisibility(View.GONE);
            }
        });


    }

    private void showAddURLDialog() {
        if (dialogAddURL == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
                    Toast.makeText(MainActivity.this, "Enter URL", Toast.LENGTH_SHORT).show();
                } else if (!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()) {
                    Toast.makeText(MainActivity.this, "Enter valid URL", Toast.LENGTH_SHORT).show();
                } else {

                    dialogAddURL.dismiss();
                    dialogAddURL = null;

                    Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                    intent.putExtra("isFromQuickActions", true);
                    intent.putExtra("quickActionType", "URL");
                    intent.putExtra("URL", inputURL.getText().toString());
                    callbackCreate.launch(intent);
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