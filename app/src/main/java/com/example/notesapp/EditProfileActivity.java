package com.example.notesapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etEditFullName;
    private EditText etEditEmail;
    private EditText etEditBio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

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
        TextView tvCancel = findViewById(R.id.tvCancel);
        TextView tvSave = findViewById(R.id.tvSave);
        etEditFullName = findViewById(R.id.etEditFullName);
        etEditEmail = findViewById(R.id.etEditEmail);
        etEditBio = findViewById(R.id.etEditBio);
        Button btnSaveChanges = findViewById(R.id.btnSaveChanges);

        // Kanselahin ang pag-edit at bumalik sa nakaraang screen
        tvCancel.setOnClickListener(v -> finish());

        // I-save ang mga pagbabago (Save button sa taas)
        tvSave.setOnClickListener(v -> saveProfileChanges());

        // I-save ang mga pagbabago (Save changes button sa ibaba)
        btnSaveChanges.setOnClickListener(v -> saveProfileChanges());
    }

    private void saveProfileChanges() {
        String fullName = etEditFullName.getText().toString().trim();
        String email = etEditEmail.getText().toString().trim();
        String bio = etEditBio != null ? etEditBio.getText().toString().trim() : "";

        if (fullName.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Dito mo ilalagay ang code para i-update sa database (PHP backend) kung kinakailangan
        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
        finish(); // Bumalik sa Profile screen pagkatapos i-save
    }
}