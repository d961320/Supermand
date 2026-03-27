package com.example.supermand.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "exercises")
public class Exercise {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public boolean isCustom;
    public String type; // "WEIGHT" or "DISTANCE_TIME"

    public Exercise(String name, boolean isCustom, String type) {
        this.name = name;
        this.isCustom = isCustom;
        this.type = type;
    }
}
