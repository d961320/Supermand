package com.example.supermand;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supermand.data.WorkoutRepository;
import com.example.supermand.data.WorkoutSession;
import com.example.supermand.data.WorkoutTemplate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {
    private WorkoutRepository repository;
    private SessionSummaryAdapter adapter;
    private long selectedDateMillis;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MMMM yyyy", Locale.getDefault());
    private TextView tvSelectedDate;
    private List<WorkoutTemplate> availableTemplates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        repository = new WorkoutRepository(getApplication());
        CalendarView calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        RecyclerView rvPastSessions = findViewById(R.id.rvPastSessions);
        Button btnLogWorkout = findViewById(R.id.btnLogWorkoutOnDate);

        rvPastSessions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionSummaryAdapter();
        rvPastSessions.setAdapter(adapter);

        selectedDateMillis = calendarView.getDate();
        updateSelectedDateText();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar cal = Calendar.getInstance();
            cal.set(year, month, dayOfMonth, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            selectedDateMillis = cal.getTimeInMillis();
            updateSelectedDateText();
            loadSessionsForDate();
        });

        btnLogWorkout.setOnClickListener(v -> showTemplateSelectionDialog());

        loadSessionsForDate();
        loadTemplates();
    }

    private void loadTemplates() {
        repository.getAllTemplates(templates -> {
            availableTemplates = templates;
        });
    }

    private void showTemplateSelectionDialog() {
        List<String> options = new ArrayList<>();
        options.add("Tom session (Hurtig start)");
        for (WorkoutTemplate t : availableTemplates) {
            options.add(t.name);
        }

        String[] optionsArray = options.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Vælg program for denne dag")
                .setItems(optionsArray, (dialog, which) -> {
                    String templateName;
                    int templateId;
                    if (which == 0) {
                        templateName = "Manuel log";
                        templateId = -1;
                    } else {
                        WorkoutTemplate selected = availableTemplates.get(which - 1);
                        templateName = selected.name;
                        templateId = selected.id;
                    }
                    startWorkoutOnSelectedDate(templateName, templateId);
                })
                .show();
    }

    private void startWorkoutOnSelectedDate(String templateName, int templateId) {
        long sessionStartTime = selectedDateMillis;
        if (isToday(selectedDateMillis)) {
            sessionStartTime = System.currentTimeMillis();
        } else {
            // If it's another day, set it to 12:00 PM for better default than 00:00
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedDateMillis);
            cal.set(Calendar.HOUR_OF_DAY, 12);
            sessionStartTime = cal.getTimeInMillis();
        }

        repository.startNewSessionAtTime(templateName, sessionStartTime, sessionId -> {
            runOnUiThread(() -> {
                Intent intent = new Intent(CalendarActivity.this, WorkoutSessionActivity.class);
                intent.putExtra("SESSION_ID", sessionId);
                intent.putExtra("TEMPLATE_ID", templateId);
                startActivity(intent);
            });
        });
    }

    private void updateSelectedDateText() {
        tvSelectedDate.setText("Træninger d. " + dateFormat.format(selectedDateMillis));
    }

    private void loadSessionsForDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(selectedDateMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long start = cal.getTimeInMillis();
        
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long end = cal.getTimeInMillis();

        repository.getSessionsBetween(start, end, sessions -> {
            runOnUiThread(() -> adapter.setSessions(sessions));
        });
    }

    private boolean isToday(long millis) {
        Calendar today = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(millis);
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);
    }
}
