package com.example.supermand;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supermand.data.Exercise;
import com.example.supermand.data.WorkoutRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManageExercisesActivity extends AppCompatActivity {
    private WorkoutRepository repository;
    private ExerciseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_exercises);

        repository = new WorkoutRepository(getApplication());

        RecyclerView rvExercises = findViewById(R.id.rvExercises);
        rvExercises.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ExerciseAdapter();
        rvExercises.setAdapter(adapter);

        adapter.setOnExerciseClickListener(exercise -> {
            List<String> optionsList = new ArrayList<>(Arrays.asList(
                    getString(R.string.btn_stats), 
                    "Rediger øvelse",
                    "Start træning (kun denne øvelse)"
            ));
            
            if (exercise.isCustom) {
                optionsList.add("Slet øvelse");
            }
            
            String[] options = optionsList.toArray(new String[0]);

            new AlertDialog.Builder(this)
                    .setTitle(exercise.name)
                    .setItems(options, (dialog, which) -> {
                        String selectedOption = options[which];
                        if (selectedOption.equals(getString(R.string.btn_stats))) {
                            Intent intent = new Intent(ManageExercisesActivity.this, ExerciseStatsActivity.class);
                            intent.putExtra("EXERCISE_ID", exercise.id);
                            intent.putExtra("EXERCISE_NAME", exercise.name);
                            startActivity(intent);
                        } else if (selectedOption.equals("Rediger øvelse")) {
                            showEditExerciseDialog(exercise);
                        } else if (selectedOption.equals("Slet øvelse")) {
                            confirmDeleteExercise(exercise);
                        } else {
                            repository.startNewSession(exercise.name, sessionId -> {
                                runOnUiThread(() -> {
                                    Intent intent = new Intent(ManageExercisesActivity.this, WorkoutSessionActivity.class);
                                    intent.putExtra("SESSION_ID", sessionId);
                                    intent.putExtra("SINGLE_EXERCISE_ID", exercise.id);
                                    startActivity(intent);
                                });
                            });
                        }
                    })
                    .show();
        });

        FloatingActionButton fab = findViewById(R.id.fabAddExercise);
        fab.setOnClickListener(v -> showAddExerciseDialog());

        loadExercises();
    }

    private void confirmDeleteExercise(Exercise exercise) {
        new AlertDialog.Builder(this)
                .setTitle("Slet øvelse")
                .setMessage("Er du sikker på, at du vil slette '" + exercise.name + "'? Dette vil også slette al historik for denne øvelse.")
                .setPositiveButton("Slet", (dialog, which) -> {
                    repository.deleteExercise(exercise);
                    loadExercises();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadExercises() {
        repository.getAllExercises(exercises -> {
            runOnUiThread(() -> adapter.setExercises(exercises));
        });
    }

    private void showAddExerciseDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_exercise, null);
        EditText input = view.findViewById(R.id.etExerciseName);
        RadioGroup rgType = view.findViewById(R.id.rgExerciseType);

        new AlertDialog.Builder(this)
                .setTitle(R.string.new_exercise)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        String type = (rgType.getCheckedRadioButtonId() == R.id.rbDistanceTime) ? "DISTANCE_TIME" : "WEIGHT";
                        repository.insertExercise(new Exercise(name, true, type));
                        loadExercises();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showEditExerciseDialog(Exercise exercise) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_exercise, null);
        EditText input = view.findViewById(R.id.etExerciseName);
        RadioGroup rgType = view.findViewById(R.id.rgExerciseType);

        input.setText(exercise.name);
        if ("DISTANCE_TIME".equals(exercise.type)) {
            rgType.check(R.id.rbDistanceTime);
        } else {
            rgType.check(R.id.rbWeight);
        }

        new AlertDialog.Builder(this)
                .setTitle("Rediger øvelse")
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        exercise.name = name;
                        exercise.type = (rgType.getCheckedRadioButtonId() == R.id.rbDistanceTime) ? "DISTANCE_TIME" : "WEIGHT";
                        repository.updateExercise(exercise);
                        loadExercises();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
