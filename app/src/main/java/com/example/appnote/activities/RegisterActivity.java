package com.example.appnote.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appnote.R;
import com.example.appnote.database.SubThread;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText edEmail, edPassword;
    private TextView tvLogin;
    private Button btnRegister;
    private FirebaseAuth auth;
    private DatabaseReference database;
    private ProgressBar loadingRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        init();
        gotoLogin();
        register();

    }

    private void register() {
        btnRegister.setOnClickListener(view -> {
            if (checkInfo()) {
                loadingRegister.setVisibility(View.VISIBLE);
                btnRegister.setEnabled(false);
                String email = edEmail.getText().toString().trim();
                String password = edPassword.getText().toString();

                Handler handler = new Handler(Looper.getMainLooper());
                Boolean isWork = SubThread.checkNetworking(getApplicationContext(), () -> database.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        handler.post(() -> {
                            String emailKey = email.replace(".", "_dot_").replace("@", "_at_");
                            if (snapshot.hasChild(emailKey)) {
                                Toast.makeText(RegisterActivity.this, "Email is already registered", Toast.LENGTH_SHORT).show();
                                loadingRegister.setVisibility(View.GONE);
                                btnRegister.setEnabled(true);
                            } else {
                                auth.createUserWithEmailAndPassword(email, password)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {

                                                database.child(emailKey).child("email").setValue(email);

                                                Toast.makeText(RegisterActivity.this, "Account Created",
                                                        Toast.LENGTH_SHORT).show();
                                            } else {
                                                Log.e("register loi firebase", task.getException().getMessage());
                                                Toast.makeText(RegisterActivity.this, "Email đã được sử dụng",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                            loadingRegister.setVisibility(View.GONE);
                                            btnRegister.setEnabled(true);
                                        });
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        handler.post(() -> {
                            loadingRegister.setVisibility(View.GONE);
                            btnRegister.setEnabled(true);
                        });
                    }
                }));
                if(!isWork){
                    loadingRegister.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                }

            }
        });
    }

    private void gotoLogin() {
        tvLogin.setOnClickListener(view -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void init() {
        edEmail = findViewById(R.id.edEmail);
        edPassword = findViewById(R.id.edPassword);
        tvLogin = findViewById(R.id.tvLogin);
        btnRegister = findViewById(R.id.btnRegister);
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReferenceFromUrl("https://mynote-4dd35-default-rtdb.firebaseio.com");
        loadingRegister = findViewById(R.id.loadingRegister);
    }

    private boolean checkInfo() {
        String email = edEmail.getText().toString().trim();
        String password = edPassword.getText().toString();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), "Email is empty", Toast.LENGTH_SHORT).show();
            edEmail.setError("Email is empty");
            edEmail.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), "Password is empty", Toast.LENGTH_SHORT).show();
            edPassword.setError("Password is empty");
            edPassword.requestFocus();
            return false;
        }

        if (!isEmail(email)) {
            Toast.makeText(getApplicationContext(), "Not email format", Toast.LENGTH_SHORT).show();
            edEmail.setError("Not email format");
            edEmail.requestFocus();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(getApplicationContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            edPassword.setError("Password must be at least 6 characters");
            edPassword.requestFocus();
            return false;
        }

        if (password.contains(" ")) {
            Toast.makeText(getApplicationContext(), "Password contains spaces", Toast.LENGTH_SHORT).show();
            edPassword.setError("Password contains spaces");
            edPassword.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isEmail(String email) {
        String EMAIL_PATTERN =
                "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@" +
                        "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}