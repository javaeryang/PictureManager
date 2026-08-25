package com.fun.picturemanager;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "date_images")
public class DateImage {
    @PrimaryKey
    @NonNull
    public String date; // yyyy-MM-dd
    public String imagePath;

    public DateImage(@NonNull String date, String imagePath) {
        this.date = date;
        this.imagePath = imagePath;
    }
}
