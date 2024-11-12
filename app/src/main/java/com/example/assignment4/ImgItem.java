package com.example.assignment4;

public class ImgItem {
    byte[] img;
    String tags;
    String dateTime;
    ImgItem(byte[] img, String tags, String dateTime) {
        this.img = img;
        this.tags = tags;
        this.dateTime = dateTime;
    }
}