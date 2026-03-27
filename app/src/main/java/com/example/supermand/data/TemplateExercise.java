package com.example.supermand.data;

import androidx.room.Entity;
import androidx.room.ForeignKey;

@Entity(
    tableName = "template_exercises",
    primaryKeys = {"templateId", "exerciseId"},
    foreignKeys = {
        @ForeignKey(entity = WorkoutTemplate.class, parentColumns = "id", childColumns = "templateId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Exercise.class, parentColumns = "id", childColumns = "exerciseId", onDelete = ForeignKey.CASCADE)
    }
)
public class TemplateExercise {
    public int templateId;
    public int exerciseId;
    public int displayOrder;

    public TemplateExercise(int templateId, int exerciseId, int displayOrder) {
        this.templateId = templateId;
        this.exerciseId = exerciseId;
        this.displayOrder = displayOrder;
    }
}
