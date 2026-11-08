package com.example.notesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private NotesAdapter notesAdapter;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        RecyclerView recyclerViewNotes = findViewById(R.id.recyclerViewNotes);
        FloatingActionButton fabAddNote = findViewById(R.id.fabAddNote);
        CardView cardProfile = findViewById(R.id.cardProfile);
        TextView navAccount = findViewById(R.id.navAccount);
        Button btnOpenArchive = findViewById(R.id.btnOpenArchive);

        recyclerViewNotes.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        notesAdapter = new NotesAdapter(new ArrayList<>());
        recyclerViewNotes.setAdapter(notesAdapter);

        notesAdapter.setOnNoteClickListener(note -> {
            Intent intent = new Intent(this, EditNoteActivity.class);
            intent.putExtra("note_id", note.getId());
            intent.putExtra("note_title", note.getTitle());
            intent.putExtra("note_content", note.getContent());
            intent.putExtra("note_category", note.getCategory());
            startActivity(intent);
        });

        btnOpenArchive.setOnClickListener(v ->
                startActivity(new Intent(this, ArchiveActivity.class)));
        fabAddNote.setOnClickListener(v ->
                startActivity(new Intent(this, AddNoteActivity.class)));
        cardProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        navAccount.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        SharedPreferences prefs = getSharedPreferences("UserThemePrefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", "1");
        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(NoteSyncScheduler.workName(userId))
                .observe(this, workInfos -> {
                    if (workInfos != null && hasSuccessfulSync(workInfos)) {
                        loadLocalNotes();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLocalNotes();
        NoteSyncScheduler.enqueue(this, userId);
    }

    private boolean hasSuccessfulSync(List<WorkInfo> workInfos) {
        for (WorkInfo workInfo : workInfos) {
            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                return true;
            }
        }
        return false;
    }

    private void loadLocalNotes() {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            List<Note> notes = database.getNotes(userId, "active");
            List<Note> displayNotes = new ArrayList<>();
            for (Note note : notes) {
                String date = note.getDate();
                if (date != null && date.length() >= 10) {
                    date = date.substring(5, 10);
                }
                displayNotes.add(new Note(note.getId(), note.getTitle(), note.getContent(),
                        date, note.getCategory(), note.getStatus(), note.getUserId(),
                        note.isSynced(), note.isDeleted(), note.getRevision()));
            }
            notesAdapter.setNotes(displayNotes);
        } catch (SQLiteException e) {
            Toast.makeText(this, "Could not load notes from this device.", Toast.LENGTH_LONG).show();
        }
    }
}
