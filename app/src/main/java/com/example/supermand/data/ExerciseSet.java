package com.example.supermand.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "exercise_sets",
    foreignKeys = {
        @ForeignKey(entity = WorkoutSession.class, parentColumns = "id", childColumns = "sessionId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Exercise.class, parentColumns = "id", childColumns = "exerciseId", onDelete = ForeignKey.CASCADE)
    }
)
public class ExerciseSet {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int sessionId;
    public int exerciseId;
    public double weight; // Also used for Distance (km)
    public int reps;      // Also used for total seconds (h*3600 + m*60 + s)
    public int setOrder;

    public ExerciseSet(int sessionId, int exerciseId, double weight, int reps, int setOrder) {
        this.sessionId = sessionId;
        this.exerciseId = exerciseId;
        this.weight = weight;
        this.reps = reps;
        this.setOrder = setOrder;
    }
}
