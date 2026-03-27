package com.example.supermand.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_templates")
public class WorkoutTemplate {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String name;
    public String description;

    public WorkoutTemplate(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
