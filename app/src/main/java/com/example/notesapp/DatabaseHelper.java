package com.example.notesapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notes_offline.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_NOTES = "notes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_IS_SYNCED = "is_synced";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_IS_DELETED = "is_deleted";
    private static final String COLUMN_REVISION = "revision";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NOTES + " ("
                + COLUMN_ID + " TEXT PRIMARY KEY,"
                + COLUMN_TITLE + " TEXT NOT NULL,"
                + COLUMN_CONTENT + " TEXT NOT NULL,"
                + COLUMN_STATUS + " TEXT NOT NULL DEFAULT 'active',"
                + COLUMN_CATEGORY + " TEXT NOT NULL DEFAULT 'Personal',"
                + COLUMN_USER_ID + " TEXT NOT NULL,"
                + COLUMN_IS_SYNCED + " INTEGER NOT NULL DEFAULT 0,"
                + COLUMN_CREATED_AT + " TEXT NOT NULL DEFAULT '',"
                + COLUMN_IS_DELETED + " INTEGER NOT NULL DEFAULT 0,"
                + COLUMN_REVISION + " INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX notes_user_status_idx ON " + TABLE_NOTES
                + "(" + COLUMN_USER_ID + ", " + COLUMN_STATUS + ", " + COLUMN_IS_DELETED + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN "
                    + COLUMN_CREATED_AT + " TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN "
                    + COLUMN_IS_DELETED + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_NOTES + " ADD COLUMN "
                    + COLUMN_REVISION + " INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE INDEX notes_user_status_idx ON " + TABLE_NOTES
                    + "(" + COLUMN_USER_ID + ", " + COLUMN_STATUS + ", " + COLUMN_IS_DELETED + ")");
        }
    }

    public void insertLocalNote(String id, String title, String content, String status,
                                String category, String userId, String createdAt) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, id);
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_CONTENT, content);
        values.put(COLUMN_STATUS, status);
        values.put(COLUMN_CATEGORY, category);
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_IS_SYNCED, 0);
        values.put(COLUMN_CREATED_AT, createdAt);
        values.put(COLUMN_IS_DELETED, 0);
        values.put(COLUMN_REVISION, 0);
        getWritableDatabase().insertOrThrow(TABLE_NOTES, null, values);
    }

    public boolean updateLocalNote(String id, String title, String content, String category) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.execSQL("UPDATE " + TABLE_NOTES + " SET " + COLUMN_TITLE + "=?, "
                            + COLUMN_CONTENT + "=?, " + COLUMN_CATEGORY + "=?, "
                            + COLUMN_IS_SYNCED + "=0, " + COLUMN_REVISION + "=" + COLUMN_REVISION
                            + "+1 WHERE " + COLUMN_ID + "=? AND " + COLUMN_IS_DELETED + "=0",
                    new Object[]{title, content, category, id});
            try (Cursor cursor = db.rawQuery("SELECT changes()", null)) {
                return cursor.moveToFirst() && cursor.getInt(0) > 0;
            }
        } finally {
            db.close();
        }
    }

    public void setNoteStatus(String id, String status) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_NOTES + " SET " + COLUMN_STATUS + "=?, "
                        + COLUMN_IS_SYNCED + "=0, " + COLUMN_REVISION + "=" + COLUMN_REVISION
                        + "+1 WHERE " + COLUMN_ID + "=? AND " + COLUMN_IS_DELETED + "=0",
                new Object[]{status, id});
        db.close();
    }

    public void markNoteDeleted(String id) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.execSQL("UPDATE " + TABLE_NOTES + " SET " + COLUMN_IS_DELETED + "=1, "
                            + COLUMN_IS_SYNCED + "=0, " + COLUMN_REVISION + "=" + COLUMN_REVISION
                            + "+1 WHERE " + COLUMN_ID + "=?",
                    new Object[]{id});
        } finally {
            db.close();
        }
    }

    public List<Note> getNotes(String userId, String statusFilter) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_NOTES, null,
                COLUMN_USER_ID + "=? AND " + COLUMN_STATUS + "=? AND " + COLUMN_IS_DELETED + "=0",
                new String[]{userId, statusFilter}, null, null, COLUMN_CREATED_AT + " DESC, rowid DESC")) {
            while (cursor.moveToNext()) {
                notes.add(readNote(cursor));
            }
        } finally {
            db.close();
        }
        return notes;
    }

    public List<Note> getPendingNotes(String userId) {
        List<Note> notes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor cursor = db.query(TABLE_NOTES, null,
                COLUMN_USER_ID + "=? AND " + COLUMN_IS_SYNCED + "=0",
                new String[]{userId}, null, null, "rowid ASC")) {
            while (cursor.moveToNext()) {
                notes.add(readNote(cursor));
            }
        } finally {
            db.close();
        }
        return notes;
    }

    public void completeSync(String oldId, String serverId, int syncedRevision) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (!oldId.equals(serverId)) {
                ContentValues idValues = new ContentValues();
                idValues.put(COLUMN_ID, serverId);
                db.update(TABLE_NOTES, idValues, COLUMN_ID + "=?", new String[]{oldId});
            }

            try (Cursor cursor = db.query(TABLE_NOTES, new String[]{COLUMN_REVISION},
                    COLUMN_ID + "=?", new String[]{serverId}, null, null, null)) {
                if (cursor.moveToFirst() && cursor.getInt(0) == syncedRevision) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_IS_SYNCED, 1);
                    db.update(TABLE_NOTES, values, COLUMN_ID + "=?", new String[]{serverId});
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void remapId(String oldId, String serverId) {
        if (oldId.equals(serverId)) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(COLUMN_ID, serverId);
            values.put(COLUMN_IS_SYNCED, 0);
            db.update(TABLE_NOTES, values, COLUMN_ID + "=?", new String[]{oldId});
        } finally {
            db.close();
        }
    }

    public void completeDelete(String id, int syncedRevision) {
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor cursor = db.query(TABLE_NOTES, new String[]{COLUMN_REVISION},
                COLUMN_ID + "=?", new String[]{id}, null, null, null)) {
            if (cursor.moveToFirst() && cursor.getInt(0) == syncedRevision) {
                db.delete(TABLE_NOTES, COLUMN_ID + "=?", new String[]{id});
            }
        } finally {
            db.close();
        }
    }

    public void mergeServerNote(Note note, String userId) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            try (Cursor cursor = db.query(TABLE_NOTES,
                    new String[]{COLUMN_IS_SYNCED, COLUMN_IS_DELETED},
                    COLUMN_ID + "=?", new String[]{note.getId()}, null, null, null)) {
                if (cursor.moveToFirst() && (cursor.getInt(0) == 0 || cursor.getInt(1) != 0)) {
                    return;
                }
            }

            ContentValues values = new ContentValues();
            values.put(COLUMN_ID, note.getId());
            values.put(COLUMN_TITLE, note.getTitle());
            values.put(COLUMN_CONTENT, note.getContent());
            values.put(COLUMN_STATUS, note.getStatus());
            values.put(COLUMN_CATEGORY, note.getCategory());
            values.put(COLUMN_USER_ID, userId);
            values.put(COLUMN_IS_SYNCED, 1);
            values.put(COLUMN_CREATED_AT, note.getDate());
            values.put(COLUMN_IS_DELETED, 0);
            values.put(COLUMN_REVISION, 0);
            db.insertWithOnConflict(TABLE_NOTES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } finally {
            db.close();
        }
    }

    private Note readNote(Cursor cursor) {
        return new Note(
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTENT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_USER_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_SYNCED)) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_DELETED)) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REVISION)));
    }
}
