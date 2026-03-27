package com.example.supermand.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_sessions")
public class WorkoutSession {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public long startTime;
    public long endTime;
    public String templateName;

    public WorkoutSession(long startTime, String templateName) {
        this.startTime = startTime;
        this.templateName = templateName;
    }
}
