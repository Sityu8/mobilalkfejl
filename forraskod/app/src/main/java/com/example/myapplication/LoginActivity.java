package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;
    private CheckBox rememberPasswordCheckBox;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    private static final String PREFS_NAME = "MyPrefsFile";
    private static final String PREF_REMEMBER_ME = "rememberMe";
    private static final String PREF_EMAIL = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        boolean rememberMe = sharedPreferences.getBoolean(PREF_REMEMBER_ME, false);

        if (currentUser != null && rememberMe) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        rememberPasswordCheckBox = findViewById(R.id.rememberPasswordCheckBox);

        // CheckBox állapotának és email mezőnek beállítása a mentett adatok alapján
        // (ha a felhasználó nem lett automatikusan beléptetve)
        boolean lastRememberMeState = sharedPreferences.getBoolean(PREF_REMEMBER_ME, false);
        rememberPasswordCheckBox.setChecked(lastRememberMeState);
        if (lastRememberMeState) {
            emailEditText.setText(sharedPreferences.getString(PREF_EMAIL, ""));
        }

        loginButton.setOnClickListener(v -> login());
        registerButton.setOnClickListener(v -> navigateToRegister());

    }

    // Bejelentkezés
    private void login() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Üres mezők része
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Kérjük, töltse ki az összes mezőt!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Bejelentkeztetés
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sikeres bejelentkezés
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        if (rememberPasswordCheckBox.isChecked()) {
                            // "Jelszó megjegyzése" be van pipálva
                            editor.putBoolean(PREF_REMEMBER_ME, true);
                            editor.putString(PREF_EMAIL, email);
                            Toast.makeText(LoginActivity.this, "Sikeres bejelentkezés. Adatok megjegyezve.", Toast.LENGTH_SHORT).show();
                        } else {
                            // "Jelszó megjegyzése" nincs bepipálva
                            editor.putBoolean(PREF_REMEMBER_ME, false);
                            editor.remove(PREF_EMAIL);
                            Toast.makeText(LoginActivity.this, "Sikeres bejelentkezés.", Toast.LENGTH_SHORT).show();
                        }
                        editor.apply();

                        navigateToMain();
                    } else {
                        // Sikertelen bejelentkezés
                        Toast.makeText(LoginActivity.this, "Hibás e-mail cím vagy jelszó!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Navigálás a MainActivity-re
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // LoginActivity bezárása, hogy ne lehessen visszanavigálni rá
    }

    // Regisztrációra navigálás
    private void navigateToRegister() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }
}