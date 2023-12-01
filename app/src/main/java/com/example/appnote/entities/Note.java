package com.example.appnote.entities;

import androidx.annotation.Nullable;
//import androidx.room.ColumnInfo;
//import androidx.room.Entity;
//import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

//@Entity(tableName = "notes")
public class Note implements Serializable {
//    @PrimaryKey(autoGenerate = true)
//    private int id;

//    @ColumnInfo(name = "genkey")
    private String genkey;

//    @ColumnInfo(name = "title")
    private String title;

//    @ColumnInfo(name = "date-_time")
    private String datetime;

//    @ColumnInfo(name = "subtitle")
    private String subtitle;

//    @ColumnInfo(name = "note_text")
    private String noteText;

//    @ColumnInfo(name = "image_path")
    private String imagePath;

//    @ColumnInfo(name = "color")
    private String color;

//    @ColumnInfo(name = "web_link")
    private String webLink;

//    public int getId() {
//        return id;
//    }

//    public void setId(int id) {
//        this.id = id;
//    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getWebLink() {
        return webLink;
    }

    public void setWebLink(String webLink) {
        this.webLink = webLink;
    }

    public String getGenkey() {
        return genkey;
    }

    public void setGenkey(String genkey) {
        this.genkey = genkey;
    }

    @Nullable
    @Override
    public String toString() {
        return title + " : " + datetime;
    }

}
