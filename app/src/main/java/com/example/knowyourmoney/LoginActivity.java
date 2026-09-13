package com.example.knowyourmoney;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class LoginActivity extends Activity {

    private EditText emailInput;
private EditText passwordInput;
private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
setContentView(R.layout.activity_login);

mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);

        Button loginButton = findViewById(R.id.loginButton);
        Button createAccount = findViewById(R.id.createAccount);

        loginButton.setOnClickListener(v -> login());

        createAccount.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void login() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter email and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener(task -> {

            if (task.isSuccessful()) {

                Intent intent =
                        new Intent(LoginActivity.this, MainActivity.class);

                startActivity(intent);
                finish();

            } else {

                Toast.makeText(this,
                        "Invalid email or password",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }

            return hex.toString();

        } catch (Exception e) {
            return "";
        }
    }
}
