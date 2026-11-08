package com.example.notesapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeActivity extends AppCompatActivity {

    private ImageView ivBack;
    private Button btnLightMode, btnDarkMode;
    private View cardSunset, cardForest, cardOcean, cardBerry, cardRoyal, cardEmerald, cardLavender, cardCosmic, cardAmber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // I-load ang tema bago i-set ang content view para iwas bug o flickering
        loadThemePreference();

        setContentView(R.layout.activity_theme);

        ivBack = findViewById(R.id.ivBack);
        btnLightMode = findViewById(R.id.btnLightMode);
        btnDarkMode = findViewById(R.id.btnDarkMode);

        cardSunset = findViewById(R.id.cardSunset);
        cardForest = findViewById(R.id.cardForest);
        cardOcean = findViewById(R.id.cardOcean);
        cardBerry = findViewById(R.id.cardBerry);
        cardRoyal = findViewById(R.id.cardRoyal);
        cardEmerald = findViewById(R.id.cardEmerald);
        cardLavender = findViewById(R.id.cardLavender);
        cardCosmic = findViewById(R.id.cardCosmic);
        cardAmber = findViewById(R.id.cardAmber);

        ivBack.setOnClickListener(v -> finish());

        // Light Mode Listener
        btnLightMode.setOnClickListener(v -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            saveThemePreference("light");
        });

        // Dark Mode Listener
        btnDarkMode.setOnClickListener(v -> {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            saveThemePreference("dark");
        });

        // Gradient Card Listeners
        cardSunset.setOnClickListener(v -> saveThemePreference("sunset"));
        cardForest.setOnClickListener(v -> saveThemePreference("forest"));
        cardOcean.setOnClickListener(v -> saveThemePreference("ocean"));
        cardBerry.setOnClickListener(v -> saveThemePreference("berry"));
        cardRoyal.setOnClickListener(v -> saveThemePreference("royal"));
        cardEmerald.setOnClickListener(v -> saveThemePreference("emerald"));
        cardLavender.setOnClickListener(v -> saveThemePreference("lavender"));
        cardCosmic.setOnClickListener(v -> saveThemePreference("cosmic"));
        cardAmber.setOnClickListener(v -> saveThemePreference("amber"));
    }

    private void saveThemePreference(String themeName) {
        SharedPreferences prefs = getSharedPreferences("UserThemePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("selected_theme", themeName);
        editor.apply();

        Toast.makeText(this, getString(R.string.theme_applied, themeName), Toast.LENGTH_SHORT).show();
        recreate(); // I-refresh agad ang activity para makita ang pagbabago
    }

    private void loadThemePreference() {
        SharedPreferences prefs = getSharedPreferences("UserThemePrefs", MODE_PRIVATE);
        String theme = prefs.getString("selected_theme", "light");

        if ("dark".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
