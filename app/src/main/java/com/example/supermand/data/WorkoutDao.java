package com.example.supermand.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import java.util.List;

@Dao
public interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertExercise(Exercise exercise);

    @Update
    void updateExercise(Exercise exercise);

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    Exercise getExerciseByName(String name);

    @Delete
    void deleteExercise(Exercise exercise);

    @Query("SELECT * FROM exercises")
    List<Exercise> getAllExercises();

    @Query("SELECT * FROM exercises WHERE id = :id")
    Exercise getExerciseById(int id);

    @Insert
    long insertSession(WorkoutSession session);

    @Query("UPDATE workout_sessions SET endTime = :endTime WHERE id = :sessionId")
    void endSession(int sessionId, long endTime);

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    void deleteSession(int sessionId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSet(ExerciseSet set);

    @Update
    void updateSet(ExerciseSet set);

    @Query("DELETE FROM exercise_sets WHERE sessionId = :sessionId AND exerciseId = :exerciseId AND setOrder = :setOrder")
    void deleteSet(int sessionId, int exerciseId, int setOrder);

    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId")
    List<ExerciseSet> getSetsForSession(int sessionId);

    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId AND exerciseId = :exerciseId ORDER BY setOrder ASC")
    List<ExerciseSet> getSetsForExerciseInSession(int sessionId, int exerciseId);

    @Transaction
    @Query("SELECT * FROM exercises WHERE id IN (SELECT DISTINCT exerciseId FROM exercise_sets WHERE sessionId = :sessionId)")
    List<Exercise> getExercisesForSession(int sessionId);

    @Query("SELECT * FROM workout_sessions ORDER BY startTime DESC")
    List<WorkoutSession> getAllSessions();

    @Query("SELECT * FROM workout_sessions WHERE startTime >= :fromTime ORDER BY startTime ASC")
    List<WorkoutSession> getSessionsFrom(long fromTime);

    @Query("SELECT * FROM workout_sessions WHERE startTime >= :start AND startTime <= :end ORDER BY startTime ASC")
    List<WorkoutSession> getSessionsBetween(long start, long end);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertTemplate(WorkoutTemplate template);

    @Query("SELECT * FROM workout_templates WHERE name = :name LIMIT 1")
    WorkoutTemplate getTemplateByName(String name);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertTemplateExercise(TemplateExercise templateExercise);

    @Query("SELECT * FROM workout_templates")
    List<WorkoutTemplate> getAllTemplates();

    @Transaction
    @Query("SELECT * FROM exercises WHERE id IN (SELECT exerciseId FROM template_exercises WHERE templateId = :templateId) ORDER BY (SELECT displayOrder FROM template_exercises WHERE templateId = :templateId AND exerciseId = exercises.id)")
    List<Exercise> getExercisesForTemplate(int templateId);

    @Query("SELECT * FROM exercise_sets WHERE exerciseId = :exerciseId AND sessionId = (SELECT MAX(sessionId) FROM exercise_sets WHERE exerciseId = :exerciseId) ORDER BY setOrder ASC")
    List<ExerciseSet> getLastSetsForExercise(int exerciseId);

    @Transaction
    @Query("SELECT ws.startTime, SUM(es.weight) as weight, SUM(es.reps) as reps, " +
           "CASE WHEN e.type = 'DISTANCE_TIME' THEN (SUM(es.weight) * (SUM(es.reps) / 60.0)) ELSE SUM(es.weight * es.reps) END as volume " +
           "FROM exercise_sets es " +
           "JOIN workout_sessions ws ON es.sessionId = ws.id " +
           "JOIN exercises e ON es.exerciseId = e.id " +
           "WHERE es.exerciseId = :exerciseId " +
           "GROUP BY es.sessionId " +
           "ORDER BY ws.startTime ASC")
    List<ExerciseProgress> getExerciseProgress(int exerciseId);
}
