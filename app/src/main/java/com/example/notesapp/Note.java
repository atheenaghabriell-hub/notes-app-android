package com.example.notesapp;

public class Note {
    private final String id;
    private final String title;
    private final String content;
    private final String date;
    private final String category;
    private final String status;
    private final String userId;
    private final boolean synced;
    private final boolean deleted;
    private final int revision;

    public Note(String id, String title, String content, String date, String category) {
        this(id, title, content, date, category, "active", "", true, false, 0);
    }

    public Note(String id, String title, String content, String date, String category,
                String status, String userId, boolean synced, boolean deleted, int revision) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.category = (category != null && !category.isEmpty()) ? category : "Personal";
        this.status = (status != null && !status.isEmpty()) ? status : "active";
        this.userId = userId;
        this.synced = synced;
        this.deleted = deleted;
        this.revision = revision;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getDate() { return date; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public String getUserId() { return userId; }
    public boolean isSynced() { return synced; }
    public boolean isDeleted() { return deleted; }
    public int getRevision() { return revision; }
}
