package com.example.appnote.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.appnote.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText edEmail,edPassword;
    private TextView tvLogin;
    private Button btnRegister;
    private FirebaseAuth auth;

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
            if(checkInfo()){
                String email = edEmail.getText().toString().trim();
                String password = edPassword.getText().toString();



                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(RegisterActivity.this, "Account Created",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Log.e("register loi firebase",task.getException().getMessage());
                                Toast.makeText(RegisterActivity.this, "Email đã được sử dụng",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    private void gotoLogin() {
        tvLogin.setOnClickListener(view -> {
            Intent intent = new Intent(RegisterActivity.this,LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void init(){
        edEmail = findViewById(R.id.edEmail);
        edPassword = findViewById(R.id.edPassword);
        tvLogin = findViewById(R.id.tvLogin);
        btnRegister = findViewById(R.id.btnRegister);
        auth = FirebaseAuth.getInstance();
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