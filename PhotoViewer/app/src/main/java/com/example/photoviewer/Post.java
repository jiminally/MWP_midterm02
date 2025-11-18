package com.example.photoviewer;

import android.graphics.Bitmap;
import java.io.Serializable;

public class Post implements Serializable {
    private int id;
    private String title;
    private String text;
    private String author;
    private String createdDate;
    private String imageUrl;
    private transient Bitmap imageBitmap;  // Bitmap은 직렬화 안됨

    public Post(int id, String title, String text, String author, String createdDate, String imageUrl, Bitmap imageBitmap) {
        this.id = id;
        this.title = title;
        this.text = text;
        this.author = author;
        this.createdDate = createdDate;
        this.imageUrl = imageUrl;
        this.imageBitmap = imageBitmap;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getText() { return text; }
    public String getAuthor() { return author; }
    public String getCreatedDate() { return createdDate; }
    public String getImageUrl() { return imageUrl; }
    public Bitmap getImageBitmap() { return imageBitmap; }

    // Setters
    public void setImageBitmap(Bitmap imageBitmap) {
        this.imageBitmap = imageBitmap;
    }
}