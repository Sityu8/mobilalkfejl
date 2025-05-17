package com.example.myapplication;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView userNameTextView, userEmailTextView;
    private TextView bookingTitleTextView, addressTextView, roomCapacityTextView, priceTextView, bookingDatesTextView, bookingDates2TextView;
    private ImageView editUserInfo, editBookingInfo, saveToCalendarButton;

    private EditText userNameEdit, userEmailEdit;
    private Button saveProfileButton, cancelButton;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userNameTextView = findViewById(R.id.userName);
        userEmailTextView = findViewById(R.id.userEmail);

        bookingTitleTextView = findViewById(R.id.bookingTitle);
        addressTextView = findViewById(R.id.adress);
        roomCapacityTextView = findViewById(R.id.roomCapacity);
        priceTextView = findViewById(R.id.price);
        bookingDatesTextView = findViewById(R.id.bookingDates);
        bookingDates2TextView = findViewById(R.id.bookingDates2);

        // EditText-ek inicializálása
        userNameEdit = findViewById(R.id.userNameEdit);
        userEmailEdit = findViewById(R.id.userEmailEdit);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        cancelButton = findViewById(R.id.cancelButton);

        // ImageView-k (szerkesztő ikonok)
        editUserInfo = findViewById(R.id.editUserInfo);
        editBookingInfo = findViewById(R.id.deleteBookingButton);
        saveToCalendarButton = findViewById(R.id.calendarSaveButton);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            loadUserInfo();
            loadBookingInfo();
        } else {
            Toast.makeText(this, "Nincs bejelentkezett felhasználó!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Szerkesztő ikonokra kattintás kezelése
        editUserInfo.setOnClickListener(v -> enableUserInfoEditing());
        editBookingInfo.setOnClickListener(v -> enableBookingInfoEditing());
        saveToCalendarButton.setOnClickListener(v -> addBookingToCalendar());


        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(ProfileActivity.this, "Sikeres kijelentkezés!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });


        Button deleteProfileButton = findViewById(R.id.deleteProfileButton);
        deleteProfileButton.setOnClickListener(v -> {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String uid = user.getUid();

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .collection("foglalasok")
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (querySnapshot.isEmpty()) {
                                // NINCS foglalás → törölhető a fiók
                                new AlertDialog.Builder(this)
                                        .setTitle("Fiók törlése")
                                        .setMessage("Biztosan törölni szeretnéd a profilodat? Ez nem visszavonható!")
                                        .setPositiveButton("Igen", (dialog, which) -> {
                                            user.delete()
                                                    .addOnSuccessListener(unused -> {
                                                        Toast.makeText(ProfileActivity.this, "Fiók törölve.", Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(ProfileActivity.this, "Hiba a fiók törlésekor: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                    });
                                        })
                                        .setNegativeButton("Mégse", null)
                                        .show();
                            } else {
                                // VAN foglalás → nem lehet törölni
                                Toast.makeText(ProfileActivity.this, "Nem törölheted a fiókodat, amíg aktív foglalásod van!", Toast.LENGTH_LONG).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(ProfileActivity.this, "Hiba a foglalások ellenőrzésekor.", Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }

    private void addBookingToCalendar() {
        if (currentUser == null) return;

        db.collection("users")
                .document(currentUser.getUid())
                .collection("foglalasok")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String checkin = doc.getString("checkin");
                        String checkout = doc.getString("checkout");

                        if (checkin != null && checkout != null) {
                            try {
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                                long startMillis = sdf.parse(checkin).getTime();
                                long endMillis = sdf.parse(checkout).getTime();

                                Intent intent = new Intent(Intent.ACTION_INSERT)
                                        .setData(android.provider.CalendarContract.Events.CONTENT_URI)
                                        .putExtra(android.provider.CalendarContract.Events.TITLE, "Szállás foglalás: Mediterrán Apartman")
                                        .putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, "Boldogtanya, Petőfi sétány 8.")
                                        .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Foglalás a Mediterrán Apartmanban")
                                        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
                                        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
                                        .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);

                                startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(this, "Hiba a dátum feldolgozásakor", Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        } else {
                            Toast.makeText(this, "Hiányzó foglalási dátumok.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Nincs menthető foglalás.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Hiba a foglalás lekérdezésekor.", Toast.LENGTH_SHORT).show());
    }

    private void loadUserInfo() {
        String displayName = currentUser.getDisplayName();
        String email = currentUser.getEmail();

        if (displayName != null && !displayName.isEmpty()) {
            userNameTextView.setText(displayName);
        } else {
            userNameTextView.setText("Névtelen felhasználó");
        }

        userEmailTextView.setText(email != null ? email : "Nincs e-mail");
    }

    private void loadBookingInfo() {
        String uid = currentUser.getUid();

        db.collection("users")
                .document(uid)
                .collection("foglalasok")
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        Map<String, Object> data = doc.getData();

                        if (data != null) {
                            String checkin = (String) data.get("checkin");
                            String checkout = (String) data.get("checkout");
                            Long price = (Long) data.get("price");

                            bookingTitleTextView.setText("Mediterrán Apartman");
                            addressTextView.setText("Apartman címe: Boldogtanya, Petőfi sétány 8.");
                            roomCapacityTextView.setText("Foglalt férőhelyek: 2 fő");
                            priceTextView.setText("Fizetendő: " + (price != null ? price : "Ismeretlen") + " Ft");
                            bookingDatesTextView.setText("Érkezés: " + (checkin != null ? checkin : "-"));
                            bookingDates2TextView.setText("Távozás: " + (checkout != null ? checkout : "-"));

                            editBookingInfo.setVisibility(View.VISIBLE);

                        }
                    } else {
                        showNoBookingInfo();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hiba a foglalás lekérdezésénél.", Toast.LENGTH_SHORT).show();
                    showNoBookingInfo();
                });
    }

    private void showNoBookingInfo() {
        bookingTitleTextView.setText("Nincs jelenleg foglalásod");
        addressTextView.setText("");
        roomCapacityTextView.setText("");
        priceTextView.setText("");
        bookingDatesTextView.setText("");
        bookingDates2TextView.setText("");
        editBookingInfo.setVisibility(View.GONE);
    }


    private void enableUserInfoEditing() {
        userNameTextView.setVisibility(View.GONE);
        userEmailTextView.setVisibility(View.GONE);
        userNameEdit.setVisibility(View.VISIBLE);
        userEmailEdit.setVisibility(View.VISIBLE);
        saveProfileButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);

        userNameEdit.setText(userNameTextView.getText().toString());
        userEmailEdit.setText(userEmailTextView.getText().toString());

        userNameEdit.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(userNameEdit, InputMethodManager.SHOW_IMPLICIT);

        saveProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelEditing();
            }
        });
    }

    private void saveChanges() {
        // Adatok betöltése
        String newName = userNameEdit.getText().toString().trim();
        String newEmail = userEmailEdit.getText().toString().trim();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Kérlek, add meg a neved!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            Toast.makeText(this, "Kérlek, adj meg egy érvényes e-mail címet!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Adatok mentése
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "User profile updated.");
                                userNameTextView.setText(newName);
                            }
                        }
                    });

            if (!user.getEmail().equals(newEmail)) {
                user.updateEmail(newEmail)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "User email address updated.");
                                    userEmailTextView.setText(newEmail);
                                } else {
                                    //TODO: Handle
                                }
                            }
                        });
            }
        }

        // UI frissítése
        userNameTextView.setText(newName);
        userEmailTextView.setText(newEmail);

        userNameTextView.setVisibility(View.VISIBLE);
        userEmailTextView.setVisibility(View.VISIBLE);
        userNameEdit.setVisibility(View.GONE);
        userEmailEdit.setVisibility(View.GONE);
        saveProfileButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);

        Toast.makeText(this, "A változások sikeresen mentve!", Toast.LENGTH_SHORT).show();
    }

    private void cancelEditing() {
        userNameTextView.setVisibility(View.VISIBLE);
        userEmailTextView.setVisibility(View.VISIBLE);
        userNameEdit.setVisibility(View.GONE);
        userEmailEdit.setVisibility(View.GONE);
        saveProfileButton.setVisibility(View.GONE);
        cancelButton.setVisibility(View.GONE);
    }

    private void enableBookingInfoEditing() {
        new AlertDialog.Builder(this)
                .setTitle("Foglalás törlése")
                .setMessage("Biztosan törölni szeretnéd a foglalást?")
                .setPositiveButton("Igen", (dialog, which) -> {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        String uid = currentUser.getUid();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        db.collection("users")
                                .document(uid)
                                .collection("foglalasok")
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                                        document.getReference().delete()
                                                .addOnSuccessListener(unused -> {
                                                    Toast.makeText(ProfileActivity.this, "Foglalás sikeresen törölve.", Toast.LENGTH_SHORT).show();

                                                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                    startActivity(intent);
                                                    finish();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(ProfileActivity.this, "Hiba történt a törlés során.", Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(ProfileActivity.this, "Nem sikerült lekérni a foglalást.", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Mégse", null)
                .show();
    }
}