package com.example.supermand;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supermand.data.Exercise;
import com.example.supermand.data.WorkoutRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private WorkoutRepository repository;
    private TemplateAdapter adapter;
    private Spinner spinnerExercises;
    private Button btnSelectDate;
    private List<Exercise> allExercises = new ArrayList<>();
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MMM", Locale.getDefault());
    private static final String PREFS_NAME = "SupermandPrefs";
    private static final String KEY_LANG = "language";

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lang = prefs.getString(KEY_LANG, "da");
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(locale);
        super.attachBaseContext(newBase.createConfigurationContext(config));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = new WorkoutRepository(getApplication());

        // Standard setup
        RecyclerView rvTemplates = findViewById(R.id.rvTemplates);
        rvTemplates.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TemplateAdapter();
        rvTemplates.setAdapter(adapter);

        adapter.setOnTemplateClickListener(template -> {
            showDateSelectionDialog(template.name, template.id);
        });

        // Buttons
        findViewById(R.id.btnQuickStart).setOnClickListener(v -> showDateSelectionDialog(getString(R.string.empty_session), -1));
        findViewById(R.id.btnShowStats).setOnClickListener(v -> startActivity(new Intent(this, StatisticsActivity.class)));
        findViewById(R.id.btnShowCalendar).setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
        findViewById(R.id.btnManageExercises).setOnClickListener(v -> startActivity(new Intent(this, ManageExercisesActivity.class)));

        // Quick Log Setup
        spinnerExercises = findViewById(R.id.spinnerExercises);
        btnSelectDate = findViewById(R.id.btnSelectDate);
        Button btnQuickLog = findViewById(R.id.btnQuickLog);

        btnSelectDate.setOnClickListener(v -> showDatePicker());
        btnQuickLog.setOnClickListener(v -> performQuickLog());

        findViewById(R.id.tvTitle).setOnLongClickListener(v -> {
            showLanguageDialog();
            return true;
        });

        updateDateButtonText();
        loadInitialData();
    }

    private void showDateSelectionDialog(String templateName, int templateId) {
        String[] options = {getString(R.string.today), getString(R.string.btn_calendar)};
        new AlertDialog.Builder(this)
                .setTitle(templateName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        startWorkoutAtTime(templateName, templateId, System.currentTimeMillis());
                    } else {
                        showDatePickerForTemplate(templateName, templateId);
                    }
                })
                .show();
    }

    private void showDatePickerForTemplate(String templateName, int templateId) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(year, month, dayOfMonth);
            long time = chosen.getTimeInMillis();
            if (isToday(time)) {
                time = System.currentTimeMillis();
            } else {
                chosen.set(Calendar.HOUR_OF_DAY, 12);
                time = chosen.getTimeInMillis();
            }
            startWorkoutAtTime(templateName, templateId, time);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showLanguageDialog() {
        String[] langs = {"Dansk", "English"};
        new AlertDialog.Builder(this)
                .setTitle(R.string.language)
                .setItems(langs, (dialog, which) -> {
                    String langCode = (which == 0) ? "da" : "en";
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putString(KEY_LANG, langCode);
                    editor.apply();
                    
                    Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                    finish();
                })
                .show();
    }

    private void loadInitialData() {
        loadTemplates();
        repository.getAllExercises(exercises -> {
            this.allExercises = exercises;
            runOnUiThread(() -> {
                List<String> names = new ArrayList<>();
                for (Exercise e : exercises) names.add(e.name);
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerExercises.setAdapter(spinnerAdapter);
            });
        });
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDate.set(year, month, dayOfMonth);
            updateDateButtonText();
        }, selectedDate.get(Calendar.YEAR), selectedDate.get(Calendar.MONTH), selectedDate.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateButtonText() {
        Calendar today = Calendar.getInstance();
        if (today.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR)) {
            btnSelectDate.setText(R.string.today);
        } else {
            btnSelectDate.setText(dateFormat.format(selectedDate.getTime()));
        }
    }

    private void performQuickLog() {
        int pos = spinnerExercises.getSelectedItemPosition();
        if (pos >= 0 && pos < allExercises.size()) {
            Exercise selected = allExercises.get(pos);
            long time = selectedDate.getTimeInMillis();
            if (isToday(time)) time = System.currentTimeMillis();

            startWorkoutAtTime(selected.name, -1, time, selected.id);
        }
    }

    private void startWorkoutAtTime(String name, int templateId, long time) {
        startWorkoutAtTime(name, templateId, time, -1);
    }

    private void startWorkoutAtTime(String name, int templateId, long time, int singleExerciseId) {
        repository.startNewSessionAtTime(name, time, sessionId -> {
            runOnUiThread(() -> {
                Intent intent = new Intent(MainActivity.this, WorkoutSessionActivity.class);
                intent.putExtra("SESSION_ID", sessionId);
                intent.putExtra("TEMPLATE_ID", templateId);
                if (singleExerciseId != -1) {
                    intent.putExtra("SINGLE_EXERCISE_ID", singleExerciseId);
                }
                startActivity(intent);
            });
        });
    }

    private boolean isToday(long millis) {
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);
    }

    private void loadTemplates() {
        repository.getAllTemplates(templates -> {
            runOnUiThread(() -> adapter.setTemplates(templates));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTemplates();
    }
}
