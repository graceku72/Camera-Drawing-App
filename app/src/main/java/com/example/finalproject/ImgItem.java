package com.example.finalproject;

public class ImgItem {
    byte[] img;
    String tags;
    String dateTime;
    int fromDrawings;

    ImgItem(byte[] img, String tags, String dateTime, int fromDrawings) {
        this.img = img;
        this.tags = tags;
        this.dateTime = dateTime;
        this.fromDrawings = fromDrawings;
    }
}