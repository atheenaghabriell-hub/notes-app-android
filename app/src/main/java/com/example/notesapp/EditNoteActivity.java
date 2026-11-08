package com.example.notesapp;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EditNoteActivity extends AppCompatActivity {

    private EditText etEditTitle;
    private EditText etEditContent;
    private EditText etEditCategory;
    private String noteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        etEditTitle = findViewById(R.id.etEditTitle);
        etEditContent = findViewById(R.id.etEditContent);
        etEditCategory = findViewById(R.id.etEditCategory);
        Button btnUpdateNote = findViewById(R.id.btnUpdateNote);

        noteId = getIntent().getStringExtra("note_id");
        etEditTitle.setText(getIntent().getStringExtra("note_title"));
        etEditContent.setText(getIntent().getStringExtra("note_content"));
        String category = getIntent().getStringExtra("note_category");
        etEditCategory.setText(category == null ? "Personal" : category);

        btnUpdateNote.setOnClickListener(v -> {
            String title = etEditTitle.getText().toString().trim();
            String content = etEditContent.getText().toString().trim();
            String categoryValue = etEditCategory.getText().toString().trim();
            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Please fill in title and content", Toast.LENGTH_SHORT).show();
                return;
            }
            if (categoryValue.isEmpty()) {
                categoryValue = "Personal";
            }
            saveLocally(title, content, categoryValue);
        });
    }

    private void saveLocally(String title, String content, String category) {
        if (noteId == null || noteId.isEmpty()) {
            Toast.makeText(this, "Note could not be found.", Toast.LENGTH_LONG).show();
            return;
        }

        try (DatabaseHelper database = new DatabaseHelper(this)) {
            if (!database.updateLocalNote(noteId, title, content, category)) {
                Toast.makeText(this, "Note could not be updated.", Toast.LENGTH_LONG).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences("UserThemePrefs", MODE_PRIVATE);
            String userId = prefs.getString("user_id", "1");
            NoteSyncScheduler.enqueue(this, userId);
            Toast.makeText(this, "Note saved on this device; it will sync when online.",
                    Toast.LENGTH_SHORT).show();
            finish();
        } catch (SQLiteException e) {
            Toast.makeText(this, "Could not save note on this device.", Toast.LENGTH_LONG).show();
        }
    }
}
