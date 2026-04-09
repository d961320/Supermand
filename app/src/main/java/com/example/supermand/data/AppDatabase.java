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

        // 1. Begynder (3 dage/uge – full body)
        int b1 = (int) insertOrGetExercise(dao, "Squat", "WEIGHT");
        int b2 = (int) insertOrGetExercise(dao, "Push-ups", "WEIGHT");
        int b3 = (int) insertOrGetExercise(dao, "Rows", "WEIGHT");
        int b4 = (int) insertOrGetExercise(dao, "Skulderpres", "WEIGHT");
        int b5 = (int) insertOrGetExercise(dao, "Planke", "WEIGHT");

        createTemplateIfMissing(dao, "1. Begynder (Full Body)", "Fokus: Teknik og grundstyrke. Pause: 60–90 sek.", 
            new int[]{b1, b2, b3, b4, b5});

        // 2. Vægttab / fedtforbrænding
        int v2 = (int) insertOrGetExercise(dao, "Deadlift", "WEIGHT");
        int v6 = (int) insertOrGetExercise(dao, "Intervaltræning", "DISTANCE_TIME");
        
        createTemplateIfMissing(dao, "2. Vægttab (Styrke)", "Fokus: Høj puls + kalorieforbrug (Dag 1 & 3)", 
            new int[]{b1, v2, b2, b3, b5});
        createTemplateIfMissing(dao, "2. Vægttab (Kondition)", "30 sek hurtigt / 90 sek roligt x 8–12 (Dag 2 & 4)", 
            new int[]{v6});

        // 3. Muskelopbygning (Hypertrofi)
        int m1 = (int) insertOrGetExercise(dao, "Bænkpres", "WEIGHT");
        int m2 = (int) insertOrGetExercise(dao, "Incline dumbbell press", "WEIGHT");
        int m3 = (int) insertOrGetExercise(dao, "Dips", "WEIGHT");
        int m4 = (int) insertOrGetExercise(dao, "Triceps pushdown", "WEIGHT");
        int m5 = (int) insertOrGetExercise(dao, "Pull-ups", "WEIGHT");
        int m6 = (int) insertOrGetExercise(dao, "Lat pulldown", "WEIGHT");
        int m7 = (int) insertOrGetExercise(dao, "Biceps curls", "WEIGHT");
        int m8 = (int) insertOrGetExercise(dao, "Romanian deadlift", "WEIGHT");
        int m9 = (int) insertOrGetExercise(dao, "Lunges", "WEIGHT");
        int m10 = (int) insertOrGetExercise(dao, "Læg", "WEIGHT");
        int m11 = (int) insertOrGetExercise(dao, "Side laterals", "WEIGHT");
        int m12 = (int) insertOrGetExercise(dao, "Face pulls", "WEIGHT");

        createTemplateIfMissing(dao, "3. Muskel (Bryst + Triceps)", "Fokus: Progressiv overload", new int[]{m1, m2, m3, m4});
        createTemplateIfMissing(dao, "3. Muskel (Ryg + Biceps)", "Fokus: Progressiv overload", new int[]{m5, b3, m6, m7});
        createTemplateIfMissing(dao, "3. Muskel (Ben)", "Fokus: Progressiv overload", new int[]{b1, m8, m9, m10});
        createTemplateIfMissing(dao, "3. Muskel (Skuldre)", "Fokus: Progressiv overload", new int[]{b4, m11, m12});

        // 4. Hjemmetræning
        int h1 = (int) insertOrGetExercise(dao, "Glute bridge", "WEIGHT");
        int h2 = (int) insertOrGetExercise(dao, "Pike push-ups", "WEIGHT");
        int h3 = (int) insertOrGetExercise(dao, "Mountain climbers", "WEIGHT");
        int h4 = (int) insertOrGetExercise(dao, "Burpees", "WEIGHT");

        createTemplateIfMissing(dao, "4. Hjemme (Dag 1 & 3)", "Ingen udstyr nødvendigt", new int[]{b1, b2, m9, b5});
        createTemplateIfMissing(dao, "4. Hjemme (Dag 2 & 4)", "Ingen udstyr nødvendigt", new int[]{h1, h2, h3, h4});

        // 5. Travl person (30 min)
        int t1 = (int) insertOrGetExercise(dao, "Jump squats", "WEIGHT");
        createTemplateIfMissing(dao, "5. Travl person (Cirkel)", "3-4 runder. Pause: 1 min mellem runder.", 
            new int[]{b1, b2, b3, b5, t1});

        // 6. Let træning / genopstart
        int l1 = (int) insertOrGetExercise(dao, "Gåtur / Cykling", "DISTANCE_TIME");
        int l2 = (int) insertOrGetExercise(dao, "Udstrækning", "DISTANCE_TIME");
        createTemplateIfMissing(dao, "6. Let træning", "Fokus: Komme i gang uden overbelastning", 
            new int[]{l1, b1, b2, b3, l2});
    }

    private static void createTemplateIfMissing(WorkoutDao dao, String name, String desc, int[] exerciseIds) {
        WorkoutTemplate existing = dao.getTemplateByName(name);
        if (existing == null) {
            long templateId = dao.insertTemplate(new WorkoutTemplate(name, desc));
            for (int i = 0; i < exerciseIds.length; i++) {
                dao.insertTemplateExercise(new TemplateExercise((int) templateId, exerciseIds[i], i + 1));
            }
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
