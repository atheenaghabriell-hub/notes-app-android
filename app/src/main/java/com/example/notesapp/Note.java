package com.example.notesapp;

public class Note {
    private final String id;
    private final String title;
    private final String content;
    private final String date;
    private final String category;

    public Note(String id, String title, String content, String date, String category) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.date = date;
        this.category = (category != null && !category.isEmpty()) ? category : "Personal";
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getDate() { return date; }
    public String getCategory() { return category; }
}