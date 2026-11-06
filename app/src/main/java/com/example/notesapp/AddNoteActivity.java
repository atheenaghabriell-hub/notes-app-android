package com.example.notesapp;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AddNoteActivity extends AppCompatActivity {

    private EditText etNoteTitle;
    private EditText etNoteContent;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final String BASE_URL = "http://10.0.2.2/notes_app/";

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

        etNoteTitle = findViewById(R.id.etNoteTitle);
        etNoteContent = findViewById(R.id.etNoteContent);
        Button btnSaveNote = findViewById(R.id.btnSaveNote);

        btnSaveNote.setOnClickListener(v -> {
            String title = etNoteTitle.getText().toString().trim();
            String content = etNoteContent.getText().toString().trim();

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(AddNoteActivity.this, "Please enter both title and content", Toast.LENGTH_SHORT).show();
                return;
            }

            saveNoteToServer("1", title, content);
        });
    }

    private void saveNoteToServer(String userId, String title, String content) {
        executorService.execute(() -> {
            String postData;
            try {
                postData = "user_id=" + URLEncoder.encode(userId, StandardCharsets.UTF_8.name())
                        + "&title=" + URLEncoder.encode(title, StandardCharsets.UTF_8.name())
                        + "&content=" + URLEncoder.encode(content, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                postData = "user_id=&title=&content=";
            }

            String result = performPostRequest(BASE_URL + "add_note.php", postData);

            runOnUiThread(() -> {
                try {
                    JSONObject jsonResponse = new JSONObject(result);
                    String status = jsonResponse.optString("status");
                    String message = jsonResponse.optString("message", result);
                    Toast.makeText(AddNoteActivity.this, message, Toast.LENGTH_SHORT).show();

                    if ("success".equals(status)) {
                        finish();
                    }
                } catch (JSONException e) {
                    Toast.makeText(AddNoteActivity.this, "Note saved!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        });
    }

    private String performPostRequest(String urlString, String postData) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(postData.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

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