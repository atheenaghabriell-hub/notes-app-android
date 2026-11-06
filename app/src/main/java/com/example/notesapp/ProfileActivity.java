package com.example.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

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
        Button btnGoToEditProfile = findViewById(R.id.btnGoToEditProfile);
        Button btnLogout = findViewById(R.id.btnLogout);

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
}