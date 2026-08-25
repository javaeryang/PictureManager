package com.fun.picturemanager;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Delete;

import java.util.List;

@Dao
public interface DateImageDao {
    @Query("SELECT * FROM date_images ORDER BY date ASC")
    List<DateImage> getAll();

    @Query("SELECT * FROM date_images WHERE date = :date LIMIT 1")
    DateImage getByDate(String date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DateImage dateImage);

    @Delete
    void delete(DateImage dateImage);
    
    @Query("DELETE FROM date_images WHERE date = :date")
    void deleteByDate(String date);
}
