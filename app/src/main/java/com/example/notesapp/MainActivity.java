package com.example.notesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
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

public class MainActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final String BASE_URL = "http://10.0.2.2/notes_app/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
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

        // 1. Link views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoToRegister = findViewById(R.id.btnGoToRegister);
        TextView tvShowHide = findViewById(R.id.tvShowHide);

        // Show/Hide Password listener
        tvShowHide.setOnClickListener(v -> {
            if (etPassword.getInputType() == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                tvShowHide.setText(R.string.action_hide);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                tvShowHide.setText(R.string.action_show);
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // 2. Pumunta sa Register Screen
        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // 3. Login Button Click Listener
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            loginUser(email, password);
        });
    }

    // 4. Function para sa pag-login papunta sa server
    private void loginUser(String email, String password) {
        executorService.execute(() -> {
            String postData;
            try {
                postData = "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8.name())
                        + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.name());
            } catch (Exception e) {
                postData = "email=&password=";
            }

            String result = performPostRequest(BASE_URL + "login.php", postData);

            runOnUiThread(() -> {
                try {
                    JSONObject jsonResponse = new JSONObject(result);
                    String status = jsonResponse.optString("status");
                    String message = jsonResponse.optString("message", result);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();

                    if ("success".equals(status)) {
                        // --- DITO NATIN IDINAGDAG ANG PAG-SAVE NG THEME PREFERENCE AT USER ID ---
                        String themePref = jsonResponse.optString("theme_preference", "light");
                        String userId = jsonResponse.optString("user_id", "1");
                        String userName = jsonResponse.optString("fullname", jsonResponse.optString("name", "User"));
                        String userEmail = jsonResponse.optString("email", email);

                        if (userId.isEmpty() && jsonResponse.has("user")) {
                            JSONObject userObj = jsonResponse.optJSONObject("user");
                            if (userObj != null) {
                                userId = userObj.optString("id", "1");
                                userName = userObj.optString("fullname", userObj.optString("name", userName));
                                userEmail = userObj.optString("email", userEmail);
                            }
                        }

                        SharedPreferences prefs = getSharedPreferences("UserThemePrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("selected_theme", themePref);
                        editor.putString("user_id", userId);
                        editor.putString("user_name", userName);
                        editor.putString("user_email", userEmail);
                        editor.apply();
                        // --------------------------------------------------------------------

                        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } catch (JSONException e) {
                    Toast.makeText(MainActivity.this, "Response: " + result, Toast.LENGTH_LONG).show();
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