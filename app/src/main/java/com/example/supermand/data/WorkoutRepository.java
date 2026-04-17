package com.example.supermand.data;

import android.app.Application;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class WorkoutRepository {
    private final WorkoutDao workoutDao;
    private final ExecutorService executor = AppDatabase.databaseWriteExecutor;

    public WorkoutRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        workoutDao = db.workoutDao();
    }

    public void getAllExercises(RepositoryCallback<List<Exercise>> callback) {
        executor.execute(() -> {
            List<Exercise> exercises = workoutDao.getAllExercises();
            callback.onComplete(exercises);
        });
    }

    public void getExerciseById(int id, RepositoryCallback<Exercise> callback) {
        executor.execute(() -> {
            Exercise exercise = workoutDao.getExerciseById(id);
            callback.onComplete(exercise);
        });
    }

    public void getAllTemplates(RepositoryCallback<List<WorkoutTemplate>> callback) {
        executor.execute(() -> {
            List<WorkoutTemplate> templates = workoutDao.getAllTemplates();
            callback.onComplete(templates);
        });
    }

    public void getExercisesForTemplate(int templateId, RepositoryCallback<List<Exercise>> callback) {
        executor.execute(() -> {
            List<Exercise> exercises = workoutDao.getExercisesForTemplate(templateId);
            callback.onComplete(exercises);
        });
    }

    public void getLastSetsForExercise(int exerciseId, RepositoryCallback<List<ExerciseSet>> callback) {
        executor.execute(() -> {
            List<ExerciseSet> sets = workoutDao.getLastSetsForExercise(exerciseId);
            callback.onComplete(sets);
        });
    }

    public void getSessionsFrom(long fromTime, RepositoryCallback<List<WorkoutSession>> callback) {
        executor.execute(() -> {
            List<WorkoutSession> sessions = workoutDao.getSessionsFrom(fromTime);
            callback.onComplete(sessions);
        });
    }

    public void getSessionsBetween(long start, long end, RepositoryCallback<List<WorkoutSession>> callback) {
        executor.execute(() -> {
            List<WorkoutSession> sessions = workoutDao.getSessionsBetween(start, end);
            callback.onComplete(sessions);
        });
    }

    public void getAllSessions(RepositoryCallback<List<WorkoutSession>> callback) {
        executor.execute(() -> {
            List<WorkoutSession> sessions = workoutDao.getAllSessions();
            callback.onComplete(sessions);
        });
    }

    public void getExerciseProgress(int exerciseId, RepositoryCallback<List<ExerciseProgress>> callback) {
        executor.execute(() -> {
            List<ExerciseProgress> progress = workoutDao.getExerciseProgress(exerciseId);
            callback.onComplete(progress);
        });
    }

    public void getSetsForSession(int sessionId, RepositoryCallback<List<ExerciseSet>> callback) {
        executor.execute(() -> {
            List<ExerciseSet> sets = workoutDao.getSetsForSession(sessionId);
            callback.onComplete(sets);
        });
    }

    public void getSetsForExerciseInSession(int sessionId, int exerciseId, RepositoryCallback<List<ExerciseSet>> callback) {
        executor.execute(() -> {
            List<ExerciseSet> sets = workoutDao.getSetsForExerciseInSession(sessionId, exerciseId);
            callback.onComplete(sets);
        });
    }

    public void getExercisesForSession(int sessionId, RepositoryCallback<List<Exercise>> callback) {
        executor.execute(() -> {
            List<Exercise> exercises = workoutDao.getExercisesForSession(sessionId);
            callback.onComplete(exercises);
        });
    }

    public void insertExercise(Exercise exercise) {
        executor.execute(() -> workoutDao.insertExercise(exercise));
    }

    public void updateExercise(Exercise exercise) {
        executor.execute(() -> workoutDao.updateExercise(exercise));
    }

    public void deleteExercise(Exercise exercise) {
        executor.execute(() -> workoutDao.deleteExercise(exercise));
    }

    public void deleteSession(int sessionId, Runnable onComplete) {
        executor.execute(() -> {
            workoutDao.deleteSession(sessionId);
            if (onComplete != null) onComplete.run();
        });
    }

    public void startNewSession(String templateName, RepositoryCallback<Long> callback) {
        startNewSessionAtTime(templateName, System.currentTimeMillis(), callback);
    }

    public void startNewSessionAtTime(String templateName, long startTime, RepositoryCallback<Long> callback) {
        executor.execute(() -> {
            long sessionId = workoutDao.insertSession(new WorkoutSession(startTime, templateName));
            callback.onComplete(sessionId);
        });
    }

    public void endSession(int sessionId) {
        executor.execute(() -> workoutDao.endSession(sessionId, System.currentTimeMillis()));
    }

    public void addSetToSession(ExerciseSet set) {
        executor.execute(() -> workoutDao.insertSet(set));
    }

    public void deleteSet(int sessionId, int exerciseId, int setOrder) {
        executor.execute(() -> workoutDao.deleteSet(sessionId, exerciseId, setOrder));
    }

    public interface RepositoryCallback<T> {
        void onComplete(T result);
    }
}
