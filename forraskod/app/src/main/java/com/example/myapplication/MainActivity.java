package com.example.myapplication;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.Manifest;


public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "booking_channel";

    private TextView userNameTextView;
    private Button kivalsztButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createNotificationChannel();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            }
        });

        db = FirebaseFirestore.getInstance();

        userNameTextView = findViewById(R.id.loggedInUserName);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String userName = "Ismeretlen felhasználó";
        if (user != null) {
            if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                userName = user.getDisplayName();
            } else if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                userName = user.getEmail();
            }
        }
        userNameTextView.setText(userName);

        userNameTextView.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        kivalsztButton = findViewById(R.id.kivalaszt_button);
        kivalsztButton.setOnClickListener(v -> showDateDialog());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1001);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Tartózkodási hely engedélyezve.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Tartózkodási hely hozzáférés megtagadva.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Foglalás értesítések";
            String description = "Értesítések a foglalásokról";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showDateDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.activity_date, null);

        Button checkInButton = dialogView.findViewById(R.id.btn_checkin);
        Button checkOutButton = dialogView.findViewById(R.id.btn_checkout);
        TextView priceText = dialogView.findViewById(R.id.text_total_price);

        final Calendar[] checkinDate = {null};
        final Calendar[] checkoutDate = {null};

        checkInButton.setOnClickListener(v -> showDatePickerDialog(date -> {
            checkinDate[0] = date;
            checkInButton.setText(formatDate(date));
            updatePriceText(checkinDate[0], checkoutDate[0], priceText);
        }));

        checkOutButton.setOnClickListener(v -> showDatePickerDialog(date -> {
            checkoutDate[0] = date;
            checkOutButton.setText(formatDate(date));
            updatePriceText(checkinDate[0], checkoutDate[0], priceText);
        }));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Dátum kiválasztása")
                .setView(dialogView)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser == null) {
                        Toast.makeText(MainActivity.this, "Be kell jelentkezned!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (checkinDate[0] == null || checkoutDate[0] == null) {
                        Toast.makeText(MainActivity.this, "Válassz dátumokat!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = currentUser.getUid();
                    long now = System.currentTimeMillis();

                    db.collection("users").document(uid).collection("foglalasok")
                            .whereGreaterThan("checkinMillis", now)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    Toast.makeText(MainActivity.this, "Egyszerre csak 1 jövőbeli foglalásod lehet!", Toast.LENGTH_LONG).show();
                                } else {
                                    long diff = checkoutDate[0].getTimeInMillis() - checkinDate[0].getTimeInMillis();
                                    long nights = diff / (1000 * 60 * 60 * 24);

                                    if (nights <= 0) {
                                        Toast.makeText(MainActivity.this, "Érvénytelen dátumok!", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    long totalPrice = nights * 40000;

                                    Map<String, Object> foglalas = new HashMap<>();
                                    foglalas.put("checkin", formatDate(checkinDate[0]));
                                    foglalas.put("checkout", formatDate(checkoutDate[0]));
                                    foglalas.put("checkinMillis", checkinDate[0].getTimeInMillis());
                                    foglalas.put("price", totalPrice);

                                    db.collection("users").document(uid).collection("foglalasok")
                                            .add(foglalas)
                                            .addOnSuccessListener(documentReference -> {
                                                showBookingNotification();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(MainActivity.this, "Hiba a foglalás mentésekor!", Toast.LENGTH_SHORT).show();
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MainActivity.this, "Hiba az ellenőrzés során!", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Mégse", null)
                .create();

        dialog.show();
    }

    private void showBookingNotification() {
        Log.d("MainActivity", "showBookingNotification called");

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("A foglalás sikeresen rögzítettük a rendszerünkben!")
                .setContentText("Köszönjük, hogy nálunk foglaltál!")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1001, builder.build());
    }

    private void showDatePickerDialog(OnDateSelectedListener listener) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog picker = new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.set(y, m, d);
            listener.onDateSelected(selectedDate);
        }, year, month, day);

        picker.show();
    }

    private void updatePriceText(Calendar checkin, Calendar checkout, TextView priceText) {
        if (checkin != null && checkout != null) {
            long diff = checkout.getTimeInMillis() - checkin.getTimeInMillis();
            long nights = diff / (1000 * 60 * 60 * 24);

            if (nights > 0) {
                long total = nights * 40000;
                priceText.setText("Fizetendő: " + total + " Ft");
            } else {
                priceText.setText("Fizetendő: 0 Ft");
            }
        }
    }

    private String formatDate(Calendar date) {
        return date.get(Calendar.YEAR) + "-" + (date.get(Calendar.MONTH) + 1) + "-" + date.get(Calendar.DAY_OF_MONTH);
    }

    interface OnDateSelectedListener {
        void onDateSelected(Calendar date);
    }
}
