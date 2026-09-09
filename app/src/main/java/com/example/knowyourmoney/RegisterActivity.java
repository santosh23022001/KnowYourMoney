package com.example.knowyourmoney;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class RegisterActivity extends Activity {

    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);

        Button registerButton = findViewById(R.id.registerButton);

        registerButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirmPassword =
                confirmPasswordInput.getText().toString();

        if (name.isEmpty() || email.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()) {

            Toast.makeText(this, "Please fill all fields",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this,
                    "Password must be at least 6 characters",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences("KnowYourMoney", MODE_PRIVATE);

        prefs.edit()
                .putString("name", name)
                .putString("email", email)
                .putString("passwordHash", hashPassword(password))
                .apply();

        Toast.makeText(this,
                "Account created successfully!",
                Toast.LENGTH_SHORT).show();

        Intent intent =
                new Intent(RegisterActivity.this, LoginActivity.class);

        startActivity(intent);
        finish();
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);

                if (h.length() == 1) {
                    hex.append('0');
                }

                hex.append(h);
            }

            return hex.toString();

        } catch (Exception e) {
            return "";
        }
    }
}
