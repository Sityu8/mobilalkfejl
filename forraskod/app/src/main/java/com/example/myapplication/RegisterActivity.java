package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText fullNameEditText, emailEditText, passwordEditText, confirmPasswordEditText;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);

        TextView existingUserText = findViewById(R.id.existingUserText);
        existingUserText.setOnClickListener(v -> navigateToLogin());
    }

    // Regisztrációs gomb kezelés
    public void register(View view) {
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Ellenőrzés, hogy a mezők ki vannak-e töltve
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Minden mezőt ki kell tölteni!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ellenőrzés, hogy az email cím megfelelő formátumban van-e
        if (!isValidEmail(email)) {
            Toast.makeText(this, "Érvénytelen email cím!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ellenőrizzük, hogy a jelszavak megegyeznek-e
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "A jelszavak nem egyeznek meg!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Regisztráció megvalósítása
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sikeres
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(RegisterActivity.this, "Sikeres regisztráció!", Toast.LENGTH_SHORT).show();

                        navigateToLogin();
                    } else {
                        // Sikertelen
                        Toast.makeText(RegisterActivity.this, "Regisztráció sikertelen: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Vissza a bejelentkezésre
    public void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // Email formátum ellenőrzése
    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}";
        Pattern pattern = Pattern.compile(emailPattern);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}