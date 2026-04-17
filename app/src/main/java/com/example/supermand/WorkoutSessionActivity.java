package com.example.supermand;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supermand.data.Exercise;
import com.example.supermand.data.ExerciseSet;
import com.example.supermand.data.WorkoutRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class WorkoutSessionActivity extends AppCompatActivity {
    private WorkoutRepository repository;
    private long sessionId;
    private WorkoutExerciseAdapter adapter;
    private List<Exercise> allAvailableExercises = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_session);

        repository = new WorkoutRepository(getApplication());
        sessionId = getIntent().getLongExtra("SESSION_ID", -1);
        int templateId = getIntent().getIntExtra("TEMPLATE_ID", -1);
        int singleExerciseId = getIntent().getIntExtra("SINGLE_EXERCISE_ID", -1);

        RecyclerView rv = findViewById(R.id.rvWorkoutExercises);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkoutExerciseAdapter();
        rv.setAdapter(adapter);

        loadAllExercises();

        if (sessionId != -1) {
            // Loading an existing session
            repository.getExercisesForSession((int) sessionId, exercises -> {
                if (exercises != null && !exercises.isEmpty()) {
                    runOnUiThread(() -> adapter.setExercises(exercises));
                } else if (templateId != -1) {
                    loadExercisesForTemplate(templateId);
                } else if (singleExerciseId != -1) {
                    loadSingleExercise(singleExerciseId);
                }
            });
        }

        findViewById(R.id.btnAddExercise).setOnClickListener(v -> showAddExerciseDialog());

        Button btnFinish = findViewById(R.id.btnFinishWorkout);
        btnFinish.setOnClickListener(v -> {
            repository.endSession((int) sessionId);
            finish();
        });
    }
    
    private void loadExercisesForTemplate(int templateId) {
        repository.getExercisesForTemplate(templateId, exercises -> {
            runOnUiThread(() -> adapter.setExercises(exercises));
        });
    }
    
    private void loadSingleExercise(int exerciseId) {
        repository.getExerciseById(exerciseId, exercise -> {
            if (exercise != null) {
                runOnUiThread(() -> adapter.setExercises(new ArrayList<>(Collections.singletonList(exercise))));
            }
        });
    }

    private void loadAllExercises() {
        repository.getAllExercises(exercises -> {
            this.allAvailableExercises = exercises;
        });
    }

    private void showAddExerciseDialog() {
        List<String> names = new ArrayList<>();
        names.add("+ " + getString(R.string.new_exercise));
        for (Exercise e : allAvailableExercises) {
            names.add(e.name);
        }

        new AlertDialog.Builder(this)
                .setTitle("Tilføj øvelse")
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    if (which == 0) {
                        showCreateNewExerciseDialog();
                    } else {
                        Exercise selected = allAvailableExercises.get(which - 1);
                        adapter.addExercise(selected);
                    }
                })
                .show();
    }

    private void showCreateNewExerciseDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_exercise, null);
        EditText input = view.findViewById(R.id.etExerciseName);
        android.widget.RadioGroup rgType = view.findViewById(R.id.rgExerciseType);

        new AlertDialog.Builder(this)
                .setTitle(R.string.new_exercise)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) {
                        String type = (rgType.getCheckedRadioButtonId() == R.id.rbDistanceTime) ? "DISTANCE_TIME" : "WEIGHT";
                        Exercise newEx = new Exercise(name, true, type);
                        repository.insertExercise(newEx);
                        adapter.addExercise(newEx);
                        loadAllExercises();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private class WorkoutExerciseAdapter extends RecyclerView.Adapter<WorkoutExerciseAdapter.ViewHolder> {
        private List<Exercise> exercises = new ArrayList<>();

        void setExercises(List<Exercise> exercises) {
            this.exercises = new ArrayList<>(exercises);
            notifyDataSetChanged();
        }

        void addExercise(Exercise exercise) {
            this.exercises.add(exercise);
            notifyItemInserted(exercises.size() - 1);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_workout_exercise, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Exercise exercise = exercises.get(position);
            holder.bind(exercise);
        }

        @Override
        public int getItemCount() {
            return exercises.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName;
            LinearLayout setsContainer;
            Button btnAddSet;
            int setCounter = 0;

            ViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvExerciseName);
                setsContainer = itemView.findViewById(R.id.setsContainer);
                btnAddSet = itemView.findViewById(R.id.btnAddSet);
            }

            void bind(Exercise exercise) {
                tvName.setText(exercise.name);
                setsContainer.removeAllViews();
                setCounter = 0;

                // Update headers based on type
                View header = ((ViewGroup) itemView).getChildAt(0);
                if (header instanceof LinearLayout) {
                    LinearLayout inner = (LinearLayout) header;
                    View headersRow = inner.getChildAt(1);
                    if (headersRow instanceof LinearLayout) {
                        LinearLayout hRow = (LinearLayout) headersRow;
                        TextView tvWeightLabel = (TextView) hRow.getChildAt(1);
                        TextView tvRepsLabel = (TextView) hRow.getChildAt(2);
                        if ("DISTANCE_TIME".equals(exercise.type)) {
                            tvWeightLabel.setText("km");
                            tvRepsLabel.setText("Tid");
                        } else {
                            tvWeightLabel.setText("kg");
                            tvRepsLabel.setText("Reps");
                        }
                    }
                }

                // Load existing sets for this session and exercise
                repository.getSetsForExerciseInSession((int) sessionId, exercise.id, sets -> {
                    runOnUiThread(() -> {
                        if (sets != null && !sets.isEmpty()) {
                            for (ExerciseSet set : sets) {
                                addExistingSetRow(exercise, set);
                            }
                        } else {
                            addSetRow(exercise);
                        }
                    });
                });

                btnAddSet.setOnClickListener(v -> addSetRow(exercise));
            }

            private void addExistingSetRow(Exercise exercise, ExerciseSet set) {
                setCounter++;
                View row = createSetRow(exercise);
                TextView tvSetNum = row.findViewById(R.id.tvSetNumber);
                tvSetNum.setText(String.valueOf(set.setOrder));
                if (set.setOrder > setCounter) setCounter = set.setOrder;

                EditText etWeight = row.findViewById(R.id.etWeight);
                EditText etReps = row.findViewById(R.id.etReps);
                EditText etH = row.findViewById(R.id.etHours);
                EditText etM = row.findViewById(R.id.etMinutes);
                EditText etS = row.findViewById(R.id.etSeconds);

                etWeight.setText(String.valueOf(set.weight));
                if ("DISTANCE_TIME".equals(exercise.type)) {
                    int h = set.reps / 3600;
                    int m = (set.reps % 3600) / 60;
                    int s = set.reps % 60;
                    if (h > 0) etH.setText(String.valueOf(h));
                    if (m > 0) etM.setText(String.valueOf(m));
                    if (s > 0) etS.setText(String.valueOf(s));
                } else {
                    etReps.setText(String.valueOf(set.reps));
                }

                setsContainer.addView(row);
            }

            private void addSetRow(Exercise exercise) {
                setCounter++;
                View row = createSetRow(exercise);
                TextView tvSetNum = row.findViewById(R.id.tvSetNumber);
                tvSetNum.setText(String.valueOf(setCounter));
                setsContainer.addView(row);
            }

            private View createSetRow(Exercise exercise) {
                View row = LayoutInflater.from(WorkoutSessionActivity.this).inflate(R.layout.item_set_row, setsContainer, false);
                row.findViewById(R.id.tvPrevious).setVisibility(View.GONE);
                
                EditText etWeight = row.findViewById(R.id.etWeight);
                EditText etReps = row.findViewById(R.id.etReps);
                View llTime = row.findViewById(R.id.llTimeInputs);
                EditText etH = row.findViewById(R.id.etHours);
                EditText etM = row.findViewById(R.id.etMinutes);
                EditText etS = row.findViewById(R.id.etSeconds);

                if ("DISTANCE_TIME".equals(exercise.type)) {
                    etWeight.setHint("km");
                    etReps.setVisibility(View.GONE);
                    llTime.setVisibility(View.VISIBLE);
                }

                View.OnFocusChangeListener saveListener = (v, hasFocus) -> {
                    if (!hasFocus) {
                        saveSet(exercise, row);
                    }
                };
                etWeight.setOnFocusChangeListener(saveListener);
                etReps.setOnFocusChangeListener(saveListener);
                etH.setOnFocusChangeListener(saveListener);
                etM.setOnFocusChangeListener(saveListener);
                etS.setOnFocusChangeListener(saveListener);

                row.findViewById(R.id.btnDeleteSet).setOnClickListener(v -> {
                    int order = Integer.parseInt(((TextView)row.findViewById(R.id.tvSetNumber)).getText().toString());
                    repository.deleteSet((int) sessionId, exercise.id, order);
                    setsContainer.removeView(row);
                    reorderSets();
                });
                return row;
            }

            private void reorderSets() {
                setCounter = 0;
                for (int i = 0; i < setsContainer.getChildCount(); i++) {
                    setCounter++;
                    View row = setsContainer.getChildAt(i);
                    ((TextView) row.findViewById(R.id.tvSetNumber)).setText(String.valueOf(setCounter));
                }
            }

            private void saveSet(Exercise exercise, View row) {
                EditText etWeight = row.findViewById(R.id.etWeight);
                EditText etReps = row.findViewById(R.id.etReps);
                EditText etH = row.findViewById(R.id.etHours);
                EditText etM = row.findViewById(R.id.etMinutes);
                EditText etS = row.findViewById(R.id.etSeconds);
                int order = Integer.parseInt(((TextView) row.findViewById(R.id.tvSetNumber)).getText().toString());

                try {
                    double weight = etWeight.getText().toString().isEmpty() ? 0 : Double.parseDouble(etWeight.getText().toString());
                    int secondValue;
                    if ("DISTANCE_TIME".equals(exercise.type)) {
                        int h = etH.getText().toString().isEmpty() ? 0 : Integer.parseInt(etH.getText().toString());
                        int m = etM.getText().toString().isEmpty() ? 0 : Integer.parseInt(etM.getText().toString());
                        int s = etS.getText().toString().isEmpty() ? 0 : Integer.parseInt(etS.getText().toString());
                        secondValue = (h * 3600) + (m * 60) + s;
                    } else {
                        secondValue = etReps.getText().toString().isEmpty() ? 0 : Integer.parseInt(etReps.getText().toString());
                    }
                    
                    if (weight > 0 || secondValue > 0) {
                        repository.addSetToSession(new ExerciseSet((int) sessionId, exercise.id, weight, secondValue, order));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
    }
}
