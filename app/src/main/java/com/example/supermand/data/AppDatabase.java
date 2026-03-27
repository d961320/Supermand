package com.example.supermand.data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Exercise.class, WorkoutSession.class, ExerciseSet.class, WorkoutTemplate.class, TemplateExercise.class}, version = 4)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract WorkoutDao workoutDao();

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "supermand_database")
                            .addCallback(sRoomDatabaseCallback)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            databaseWriteExecutor.execute(() -> {
                initializeDefaultData(INSTANCE);
            });
        }
    };

    public static void initializeDefaultData(AppDatabase database) {
        WorkoutDao dao = database.workoutDao();
        
        // Add default exercises if they don't exist
        long squatId = insertOrGetExercise(dao, "Squat", "WEIGHT");
        long benchId = insertOrGetExercise(dao, "Bench Press", "WEIGHT");
        long deadliftId = insertOrGetExercise(dao, "Deadlift", "WEIGHT");
        long overheadId = insertOrGetExercise(dao, "Overhead Press", "WEIGHT");
        long rowId = insertOrGetExercise(dao, "Barbell Row", "WEIGHT");

        // Add "Full Body" template if it doesn't exist
        WorkoutTemplate existingTemplate = dao.getTemplateByName("Full Body");
        if (existingTemplate == null) {
            long fullBodyId = dao.insertTemplate(new WorkoutTemplate("Full Body", "Basic strength training for the whole body"));
            dao.insertTemplateExercise(new TemplateExercise((int) fullBodyId, (int) squatId, 1));
            dao.insertTemplateExercise(new TemplateExercise((int) fullBodyId, (int) benchId, 2));
            dao.insertTemplateExercise(new TemplateExercise((int) fullBodyId, (int) rowId, 3));
        }
    }

    private static long insertOrGetExercise(WorkoutDao dao, String name, String type) {
        Exercise existing = dao.getExerciseByName(name);
        if (existing != null) {
            return existing.id;
        }
        return dao.insertExercise(new Exercise(name, false, type));
    }
}
