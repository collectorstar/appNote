package com.example.appnote.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appnote.R;
import com.example.appnote.appsettings.Setting;
import com.example.appnote.threads.SubThread;
import com.example.appnote.entities.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends AppCompatActivity {

    private EditText edEmail, edPassword;
    private TextView tvFogotPass, tvRegister;
    private Button btnLogin;
    private FirebaseAuth auth;
    private DatabaseReference database;
    private ProgressBar loadingLogin;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            User.Email = currentUser.getEmail();
            User.EmailKey = User.Email.replace(".", "_dot_").replace("@", "_at_");
            User.Name = currentUser.getDisplayName();
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        init();
        fogotPass();
        gotoRegister();
        login();

    }

    private void gotoRegister() {
        tvRegister.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void init() {
        edEmail = findViewById(R.id.edEmail);
        edPassword = findViewById(R.id.edPassword);
        tvFogotPass = findViewById(R.id.tvFogotPass);
        tvRegister = findViewById(R.id.tvRegister);
        btnLogin = findViewById(R.id.btnLogin);
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReferenceFromUrl(Setting.LinkDB);
        loadingLogin = findViewById(R.id.loadingLogin);
    }

    private void fogotPass() {
        tvFogotPass.setOnClickListener(view -> {

            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_fogot_password, null);
            EditText emailBox = dialogView.findViewById(R.id.emailBox);

            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            dialogView.findViewById(R.id.btnReset).setOnClickListener(view1 -> {
                String userEmail = emailBox.getText().toString();

                if (TextUtils.isEmpty(userEmail) && !Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                    Toast.makeText(LoginActivity.this, "Enter your registered email", Toast.LENGTH_SHORT).show();
                    emailBox.setError("Enter your registered email");
                    emailBox.requestFocus();
                    return;
                }

                dialogView.findViewById(R.id.btnReset).setEnabled(false);
                dialogView.findViewById(R.id.loadingReset).setVisibility(View.VISIBLE);

                Handler handler = new Handler(Looper.getMainLooper());

                boolean isWork = SubThread.runSubThread(getApplicationContext(), () -> auth.sendPasswordResetEmail(userEmail)
                        .addOnCompleteListener(task -> handler.post(() -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Check your email", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(LoginActivity.this, "Unable to send, failed!", Toast.LENGTH_SHORT).show();
                            }
                            dialogView.findViewById(R.id.btnReset).setEnabled(true);
                            dialogView.findViewById(R.id.loadingReset).setVisibility(View.GONE);
                        })));
                if (!isWork) {
                    dialogView.findViewById(R.id.btnReset).setEnabled(true);
                    dialogView.findViewById(R.id.loadingReset).setVisibility(View.GONE);
                }
            });

            dialogView.findViewById(R.id.btnCancel).setOnClickListener(view12 -> dialog.dismiss());

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            dialog.show();
        });
    }

    private void login() {
        btnLogin.setOnClickListener(view -> {
            if (checkInfo()) {
                loadingLogin.setVisibility(View.VISIBLE);
                btnLogin.setEnabled(false);
                String email = edEmail.getText().toString().trim();
                String password = edPassword.getText().toString();

                Handler handler = new Handler(Looper.getMainLooper());
                boolean iswork = SubThread.runSubThread(getApplicationContext(), () -> database.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String emailKey = email.replace(".", "_dot_").replace("@", "_at_");
                        if (snapshot.hasChild(emailKey)) {
                            auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener(task -> handler.post(() -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(getApplicationContext(), "Login successful",
                                                    Toast.LENGTH_SHORT).show();
                                            User.Email = auth.getCurrentUser().getEmail();
                                            User.EmailKey = User.Email.replace(".", "_dot_").replace("@", "_at_");
                                            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Log.e("Login firebase error", task.getException().getMessage());
                                            Toast.makeText(getApplicationContext(), "Something is wrong",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        loadingLogin.setVisibility(View.GONE);
                                        btnLogin.setEnabled(true);
                                    }));
                        } else {
                            handler.post(() -> {
                                Toast.makeText(getApplicationContext(), "Email or Password is incorrect!",
                                        Toast.LENGTH_SHORT).show();
                                loadingLogin.setVisibility(View.GONE);
                                btnLogin.setEnabled(true);
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        handler.post(() -> {
                            loadingLogin.setVisibility(View.GONE);
                            btnLogin.setEnabled(true);
                        });
                    }
                }));

                if (!iswork) {
                    loadingLogin.setVisibility(View.GONE);
                    btnLogin.setEnabled(true);
                }
            }
        });
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