package com.example.notesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            int initialLeft = mainView.getPaddingLeft();
            int initialTop = mainView.getPaddingTop();
            int initialRight = mainView.getPaddingRight();
            int initialBottom = mainView.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets insetsValues = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(
                        initialLeft + insetsValues.left,
                        initialTop + insetsValues.top,
                        initialRight + insetsValues.right,
                        initialBottom + insetsValues.bottom
                );
                return insets;
            });
        }

        // I-link ang mga views mula sa XML
        ImageView ivBack = findViewById(R.id.ivBack);
        TextView tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        TextView tvProfileName = findViewById(R.id.tvProfileName);
        TextView tvProfileEmail = findViewById(R.id.tvProfileEmail);
        TextView tvAccountName = findViewById(R.id.tvAccountName);
        TextView tvAccountEmail = findViewById(R.id.tvAccountEmail);
        Button btnGoToEditProfile = findViewById(R.id.btnGoToEditProfile);
        Button btnLogout = findViewById(R.id.btnLogout);

        // Kunin ang user data mula sa SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserThemePrefs", MODE_PRIVATE);
        String name = prefs.getString("user_name", "User");
        String email = prefs.getString("user_email", "user@example.com");

        tvProfileName.setText(name);
        tvProfileEmail.setText(email);
        tvAccountName.setText(name);
        tvAccountEmail.setText(email);
        tvAvatarInitials.setText(getInitials(name));

        // Pindutan pabalik sa Dashboard
        ivBack.setOnClickListener(v -> finish());

        // Pindutan papunta sa Edit Profile screen
        btnGoToEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        // Pindutan para mag-log out (babalik sa MainActivity / Login)
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
            // I-clear ang activity stack para hindi na makabalik sa dashboard nang walang login
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "U"; // Default kung walang pangalan
        }

        String[] words = fullName.trim().split("\\s+");
        if (words.length == 1) {
            // Kung isang salita lang, kunin ang unang letra
            return String.valueOf(words[0].charAt(0)).toUpperCase();
        } else {
            // Kung may apelyido o higit sa isang salita, kunin ang unang letra ng una at huling salita
            String firstLetter = String.valueOf(words[0].charAt(0));
            String lastLetter = String.valueOf(words[words.length - 1].charAt(0));
            return (firstLetter + lastLetter).toUpperCase();
        }
    }
}
