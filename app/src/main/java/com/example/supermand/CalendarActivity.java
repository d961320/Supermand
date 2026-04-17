package com.example.supermand;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.example.supermand.data.Exercise;
import com.example.supermand.data.ExerciseSet;
import com.example.supermand.data.WorkoutRepository;
import com.example.supermand.data.WorkoutSession;
import com.example.supermand.data.WorkoutTemplate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CalendarActivity extends AppCompatActivity {
    private WorkoutRepository repository;
    private SessionSummaryAdapter adapter;
    private long selectedDateMillis;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MMMM yyyy", Locale.getDefault());
    private TextView tvSelectedDate;
    private List<WorkoutTemplate> availableTemplates = new ArrayList<>();
    private CalendarView calendarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        repository = new WorkoutRepository(getApplication());
        calendarView = findViewById(R.id.calendarView);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        RecyclerView rvPastSessions = findViewById(R.id.rvPastSessions);
        Button btnLogWorkout = findViewById(R.id.btnLogWorkoutOnDate);

        rvPastSessions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionSummaryAdapter();
        rvPastSessions.setAdapter(adapter);

        selectedDateMillis = System.currentTimeMillis();
        updateSelectedDateText();

        calendarView.setOnDayClickListener(eventDay -> {
            Calendar clickedDay = eventDay.getCalendar();
            selectedDateMillis = clickedDay.getTimeInMillis();
            updateSelectedDateText();
            loadSessionsForDate();
        });

        adapter.setOnSessionClickListener(session -> {
            showSessionOptionsDialog(session);
        });

        btnLogWorkout.setOnClickListener(v -> showTemplateSelectionDialog());

        loadSessionsForDate();
        loadTemplates();
        highlightWorkoutDays();
    }

    private void showSessionOptionsDialog(WorkoutSession session) {
        String[] options = {"Se detaljer / Rediger", "Slet træning"};
        new AlertDialog.Builder(this)
                .setTitle(session.templateName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        Intent intent = new Intent(this, WorkoutSessionActivity.class);
                        intent.putExtra("SESSION_ID", (long) session.id);
                        startActivity(intent);
                    } else {
                        confirmDeleteSession(session);
                    }
                })
                .show();
    }

    private void confirmDeleteSession(WorkoutSession session) {
        new AlertDialog.Builder(this)
                .setTitle("Slet træning")
                .setMessage("Er du sikker på, at du vil slette denne træning?")
                .setPositiveButton("Slet", (dialog, which) -> {
                    repository.deleteSession(session.id);
                    loadSessionsForDate();
                    highlightWorkoutDays();
                })
                .setNegativeButton("Annuller", null)
                .show();
    }

    private void highlightWorkoutDays() {
        repository.getAllSessions(sessions -> {
            List<EventDay> events = new ArrayList<>();
            Set<String> uniqueDays = new HashSet<>();

            for (WorkoutSession session : sessions) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(session.startTime);
                
                Calendar day = Calendar.getInstance();
                day.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
                day.set(Calendar.MILLISECOND, 0);

                String dateKey = day.get(Calendar.YEAR) + "-" + day.get(Calendar.MONTH) + "-" + day.get(Calendar.DAY_OF_MONTH);
                
                if (!uniqueDays.contains(dateKey)) {
                    uniqueDays.add(dateKey);
                    events.add(new EventDay(day, R.drawable.dot));
                }
            }
            
            runOnUiThread(() -> {
                calendarView.setEvents(events);
                calendarView.setHighlightedDays(new ArrayList<>());
            });
        });
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
