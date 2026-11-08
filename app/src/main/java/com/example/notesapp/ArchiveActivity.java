package com.example.notesapp;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.List;

public class ArchiveActivity extends AppCompatActivity {

    private ArchiveAdapter archiveAdapter;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive);

        RecyclerView recyclerViewArchive = findViewById(R.id.recyclerViewArchive);
        recyclerViewArchive.setLayoutManager(new LinearLayoutManager(this));
        archiveAdapter = new ArchiveAdapter(new ArrayList<>(),
                new ArchiveAdapter.OnArchiveActionListener() {
                    @Override
                    public void onRestore(Note note) {
                        updateStatus(note, "active");
                    }

                    @Override
                    public void onDeletePermanent(Note note) {
                        deleteNote(note);
                    }
                });
        recyclerViewArchive.setAdapter(archiveAdapter);

        SharedPreferences prefs = getSharedPreferences("UserThemePrefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", "1");
        WorkManager.getInstance(this)
                .getWorkInfosForUniqueWorkLiveData(NoteSyncScheduler.workName(userId))
                .observe(this, workInfos -> {
                    if (workInfos != null) {
                        for (WorkInfo workInfo : workInfos) {
                            if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                loadArchivedNotes();
                                break;
                            }
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadArchivedNotes();
        NoteSyncScheduler.enqueue(this, userId);
    }

    private void loadArchivedNotes() {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            List<Note> notes = database.getNotes(userId, "archived");
            archiveAdapter.setNotes(notes);
        } catch (SQLiteException e) {
            Toast.makeText(this, "Could not load archived notes from this device.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus(Note note, String status) {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            database.setNoteStatus(note.getId(), status);
            loadArchivedNotes();
            NoteSyncScheduler.enqueue(this, userId);
        } catch (SQLiteException e) {
            Toast.makeText(this, "Could not update note on this device.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void deleteNote(Note note) {
        try (DatabaseHelper database = new DatabaseHelper(this)) {
            database.markNoteDeleted(note.getId());
            loadArchivedNotes();
            NoteSyncScheduler.enqueue(this, userId);
        } catch (SQLiteException e) {
            Toast.makeText(this, "Could not delete note from this device.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
