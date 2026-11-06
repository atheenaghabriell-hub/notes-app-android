package com.example.notesapp;

import android.content.Intent;
import android.os.Bundle;





import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity {

    private NotesAdapter notesAdapter;
    private List<Note> noteList;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final String BASE_URL = "http://10.0.2.2/notes_app/";
    private static final String DEFAULT_USER_ID = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        RecyclerView recyclerViewNotes = findViewById(R.id.recyclerViewNotes);
        FloatingActionButton fabAddNote = findViewById(R.id.fabAddNote);
        CardView cardProfile = findViewById(R.id.cardProfile);
        TextView navAccount = findViewById(R.id.navAccount);

        // Itakda ang StaggeredGridLayout na may 2 columns para sa modernong itsura
        recyclerViewNotes.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList);
        recyclerViewNotes.setAdapter(notesAdapter);

        // Kapag pinindot ang (+) Floating Action Button, pumunta sa AddNoteActivity
        fabAddNote.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddNoteActivity.class);
            startActivity(intent);
        });

        // Kapag pinindot ang Profile Card o Account tab, pumunta sa ProfileActivity
        cardProfile.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        navAccount.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // Kunin ang mga notes mula sa server kapag binuksan ang dashboard
        fetchNotes(DEFAULT_USER_ID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // I-refresh ang listahan kapag bumalik mula sa AddNoteActivity
        fetchNotes(DEFAULT_USER_ID);
    }

    private void fetchNotes(String userId) {
        executorService.execute(() -> {
            String result = performGetRequest(BASE_URL + "fetch_notes.php?user_id=" + userId);

            runOnUiThread(() -> {
                try {
                    JSONObject jsonResponse = new JSONObject(result);
                    String status = jsonResponse.optString("status");

                    if ("success".equals(status)) {
                        JSONArray notesArray = jsonResponse.getJSONArray("notes");
                        List<Note> fetchedNotes = new ArrayList<>();

                        for (int i = 0; i < notesArray.length(); i++) {
                            JSONObject obj = notesArray.getJSONObject(i);
                            String id = obj.optString("id");
                            String title = obj.optString("title");
                            String content = obj.optString("content");
                            String date = obj.optString("create_at"); // Petsa mula sa database

                            // I-format nang bahagya ang date kung mahaba (kunin ang petsa lang)
                            if (!date.isEmpty() && date.length() >= 10) {
                                date = date.substring(5, 10); // Halimbawa: "11-05"
                            }

                            fetchedNotes.add(new Note(id, title, content, date, "Work"));
                        }
                        notesAdapter.setNotes(fetchedNotes);
                    } else {
                        String message = jsonResponse.optString("message", "Failed to load notes");
                        Toast.makeText(DashboardActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    Toast.makeText(DashboardActivity.this, "Parsing error: " + result, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    private String performGetRequest(String urlString) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int responseCode = conn.getResponseCode();
            InputStream is = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    return sb.toString();
                }
            } else {
                return "{\"status\":\"error\",\"message\":\"Server returned HTTP " + responseCode + "\"}";
            }
        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"" + (e.getMessage() != null ? e.getMessage() : "Network error") + "\"}";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}