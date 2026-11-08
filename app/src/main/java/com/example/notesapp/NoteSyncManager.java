package com.example.notesapp;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class NoteSyncManager {

    private static final String TAG = "NoteSyncManager";
    private static final String BASE_URL = "http://10.0.2.2/notes_app/";
    private static final int MAX_SYNC_PASSES = 3;

    private NoteSyncManager() {
    }

    public static boolean sync(Context context, String userId) {
        try (DatabaseHelper database = new DatabaseHelper(context)) {
            for (int pass = 0; pass < MAX_SYNC_PASSES; pass++) {
                List<Note> pendingNotes = database.getPendingNotes(userId);
                if (pendingNotes.isEmpty()) {
                    return refreshFromServer(database, userId);
                }

                List<Note> notesToUpsert = new ArrayList<>();
                for (Note note : pendingNotes) {
                    if (note.isDeleted()) {
                        if (!syncNote(database, note)) {
                            return false;
                        }
                    } else {
                        notesToUpsert.add(note);
                    }
                }
                if (!notesToUpsert.isEmpty() && !syncNotes(database, notesToUpsert)) {
                    return false;
                }
            }
            return database.getPendingNotes(userId).isEmpty()
                    && refreshFromServer(database, userId);
        }
    }

    private static boolean syncNote(DatabaseHelper database, Note note) {
        try {
            if (note.isDeleted()) {
                String serverId = note.getId();
                if (serverId.startsWith("local:")) {
                    serverId = createServerNote(note);
                    if (serverId.isEmpty()) {
                        return false;
                    }
                    database.remapId(note.getId(), serverId);
                }
                JSONObject response = post(
                        form("note_id", serverId));
                if (isSuccess(response)) {
                    return false;
                }
                database.completeDelete(serverId, note.getRevision());
                return true;
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync note " + note.getId(), e);
            return false;
        }
    }

    private static String createServerNote(Note note) throws Exception {
        JSONObject item = new JSONObject();
        item.put("id", note.getId());
        item.put("user_id", note.getUserId());
        item.put("title", note.getTitle());
        item.put("content", note.getContent());
        item.put("category", note.getCategory());
        item.put("status", note.getStatus());
        if (note.getDate() != null && !note.getDate().isEmpty()) {
            item.put("created_at", note.getDate());
        }

        JSONArray payload = new JSONArray();
        payload.put(item);
        JSONObject response = postJson(payload);
        if (isSuccess(response)) {
            return "";
        }
        JSONArray syncedNotes = response.getJSONArray("notes");
        return syncedNotes.length() == 1
                ? syncedNotes.getJSONObject(0).optString("note_id", "") : "";
    }

    private static boolean syncNotes(DatabaseHelper database, List<Note> notes) {
        try {
            JSONArray requestNotes = new JSONArray();
            for (Note note : notes) {
                JSONObject item = new JSONObject();
                item.put("id", note.getId());
                item.put("user_id", note.getUserId());
                item.put("title", note.getTitle());
                item.put("content", note.getContent());
                item.put("category", note.getCategory());
                item.put("status", note.getStatus());
                if (note.getDate() != null && !note.getDate().isEmpty()) {
                    item.put("created_at", note.getDate());
                }
                requestNotes.put(item);
            }

            JSONObject response = postJson(requestNotes);
            if (isSuccess(response)) {
                return false;
            }

            JSONArray syncedNotes = response.getJSONArray("notes");
            if (syncedNotes.length() != notes.size()) {
                return false;
            }

            for (int i = 0; i < syncedNotes.length(); i++) {
                JSONObject result = syncedNotes.getJSONObject(i);
                String originalId = result.optString("client_id", "");
                if (originalId.isEmpty()) {
                    originalId = result.optString("id", "");
                }
                String serverId = result.optString("note_id", "");
                Note original = findById(notes, originalId);
                if (original == null || serverId.isEmpty()) {
                    return false;
                }
                database.completeSync(original.getId(), serverId, original.getRevision());
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync notes batch", e);
            return false;
        }
    }

    private static Note findById(List<Note> notes, String id) {
        for (Note note : notes) {
            if (note.getId().equals(id)) {
                return note;
            }
        }
        return null;
    }

    private static boolean refreshFromServer(DatabaseHelper database, String userId) {
        for (String status : new String[]{"active", "archived"}) {
            try {
                String encodedUserId = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    encodedUserId = URLEncoder.encode(userId, StandardCharsets.UTF_8);
                }
                String encodedStatus = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    encodedStatus = URLEncoder.encode(status, StandardCharsets.UTF_8);
                }
                JSONObject response = get("fetch_notes.php?user_id=" + encodedUserId
                        + "&status=" + encodedStatus);
                if (isSuccess(response)) {
                    return false;
                }

                JSONArray notes = response.getJSONArray("notes");
                for (int i = 0; i < notes.length(); i++) {
                    JSONObject row = notes.getJSONObject(i);
                    database.mergeServerNote(new Note(
                            row.optString("id"),
                            row.optString("title"),
                            row.optString("content"),
                            row.optString("create_at"),
                            row.optString("category", "Personal"),
                            status,
                            userId,
                            true,
                            false,
                            0), userId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to refresh " + status + " notes", e);
                return false;
            }
        }
        return true;
    }

    private static boolean isSuccess(JSONObject response) {
        return !"success".equals(response.optString("status"));
    }

    private static JSONObject get(String path) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static JSONObject post(List<String> fields) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(BASE_URL + "delete_note_permanent.php").openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            String body = String.join("&", fields);
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body.getBytes(StandardCharsets.UTF_8));
            }
            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static JSONObject postJson(JSONArray payload) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(BASE_URL + "sync_notes.php").openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            try (OutputStream output = connection.getOutputStream()) {
                output.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            return readResponse(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static JSONObject readResponse(HttpURLConnection connection) throws Exception {
        int responseCode = connection.getResponseCode();
        InputStream input = (responseCode >= 200 && responseCode < 300)
                ? connection.getInputStream() : connection.getErrorStream();
        if (input == null) {
            throw new IllegalStateException("Server returned HTTP " + responseCode);
        }

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return new JSONObject(body.toString());
    }

    private static List<String> form(String... pairs) {
        List<String> fields = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                fields.add(URLEncoder.encode(pairs[i], StandardCharsets.UTF_8)
                        + "=" + URLEncoder.encode(pairs[i + 1], StandardCharsets.UTF_8));
            }
        }
        return fields;
    }
}
