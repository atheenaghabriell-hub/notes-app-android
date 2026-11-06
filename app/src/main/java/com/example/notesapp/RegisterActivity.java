package com.example.notesapp;

import android.content.Intent;
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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Palitan ng IP address ng iyong PC kung physical phone ang gamit, o 10.0.2.2 kung emulator
    private static final String BASE_URL = "http://10.0.2.2/notes_app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

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
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            // Validation
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Objects.equals(password, confirmPassword)) {
                Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUser(fullName, email, password);
        });
    }

    private void registerUser(String fullName, String email, String password) {
        executorService.execute(() -> {
            String postData;
            try {
                postData = "fullname=" + URLEncoder.encode(fullName, StandardCharsets.UTF_8.name())
                        + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8.name())
                        + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                postData = "fullname=&email=&password=";
            }

            String result = performPostRequest(BASE_URL + "register.php", postData);

            runOnUiThread(() -> {
                try {
                    JSONObject jsonResponse = new JSONObject(result);
                    String status = jsonResponse.optString("status");
                    String message = jsonResponse.optString("message", result);
                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();

                    if ("success".equals(status)) {
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } catch (JSONException e) {
                    Toast.makeText(RegisterActivity.this, "Response: " + result, Toast.LENGTH_LONG).show();
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