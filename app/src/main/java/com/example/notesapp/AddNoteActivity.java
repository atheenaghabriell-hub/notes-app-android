package com.example.notesapp;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class AddNoteActivity extends AppCompatActivity {

    private EditText etNoteTitle;
    private EditText etNoteContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_note);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            int initialLeft = mainView.getPaddingLeft();
            int initialTop = mainView.getPaddingTop();
            int initialRight = mainView.getPaddingRight();
            int initialBottom = mainView.getPaddingBottom();

            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets values = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
                v.setPadding(initialLeft + values.left, initialTop + values.top,
                        initialRight + values.right, initialBottom + values.bottom);
                return insets;
            });
        }

        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        Button btnSaveNote = findViewById(R.id.btnSaveNote);

        btnSaveNote.setOnClickListener(v -> {
            String title = etNoteTitle.getText().toString().trim();
            String content = etNoteContent.getText().toString().trim();
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Please enter both title and content", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("UserThemePrefs", MODE_PRIVATE);
            String userId = prefs.getString("user_id", "1");
            String noteId = "local:" + UUID.randomUUID();
            String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(new Date());

            try (DatabaseHelper database = new DatabaseHelper(this)) {
                database.insertLocalNote(noteId, title, content, "active",
                        "Personal", userId, createdAt);
                NoteSyncScheduler.enqueue(this, userId);
                Toast.makeText(this, "Note saved on this device; it will sync when online.",
                        Toast.LENGTH_SHORT).show();
                finish();
            } catch (SQLiteException e) {
                Toast.makeText(this, "Could not save note on this device.", Toast.LENGTH_LONG).show();
            }
        });
    }
}
