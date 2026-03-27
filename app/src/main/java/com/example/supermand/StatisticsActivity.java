package com.example.supermand;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supermand.data.Exercise;
import com.example.supermand.data.WorkoutRepository;
import com.example.supermand.data.WorkoutSession;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

public class StatisticsActivity extends AppCompatActivity {
    private WorkoutRepository repository;
    private BarChart barChart;
    private LineChart mainLineChart;
    private MaterialButtonToggleGroup timeToggleGroup;
    private MaterialButtonToggleGroup mainChartToggle;
    private RecyclerView rvExerciseCharts;
    private ExerciseChartAdapter chartAdapter;
    private int currentDays = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        repository = new WorkoutRepository(getApplication());
        barChart = findViewById(R.id.barChart);
        mainLineChart = findViewById(R.id.mainLineChart);
        timeToggleGroup = findViewById(R.id.timeToggleGroup);
        mainChartToggle = findViewById(R.id.mainChartToggle);
        rvExerciseCharts = findViewById(R.id.rvExerciseCharts);

        rvExerciseCharts.setLayoutManager(new LinearLayoutManager(this));
        chartAdapter = new ExerciseChartAdapter();
        rvExerciseCharts.setAdapter(chartAdapter);

        setupMainCharts();

        timeToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnWeek) currentDays = 7;
                else if (checkedId == R.id.btnMonth) currentDays = 30;
                else if (checkedId == R.id.btnYear) currentDays = 365;
                loadData();
            }
        });

        mainChartToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                boolean showLine = (checkedId == R.id.btnMainLine);
                mainLineChart.setVisibility(showLine ? View.VISIBLE : View.GONE);
                barChart.setVisibility(showLine ? View.GONE : View.VISIBLE);
            }
        });

        loadData();
    }

    private void setupMainCharts() {
        // BarChart
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        XAxis xAxisBar = barChart.getXAxis();
        xAxisBar.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBar.setGranularity(1f);
        barChart.getAxisRight().setEnabled(false);
        barChart.getAxisLeft().setAxisMinimum(0f);

        // LineChart
        mainLineChart.getDescription().setEnabled(false);
        mainLineChart.getLegend().setEnabled(false);
        XAxis xAxisLine = mainLineChart.getXAxis();
        xAxisLine.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisLine.setGranularity(1f);
        mainLineChart.getAxisRight().setEnabled(false);
        mainLineChart.getAxisLeft().setAxisMinimum(0f);
    }

    private void loadData() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -currentDays);
        long fromTime = cal.getTimeInMillis();

        repository.getSessionsFrom(fromTime, sessions -> {
            runOnUiThread(() -> updateMainCharts(sessions));
        });

        repository.getAllExercises(exercises -> {
            List<ExerciseChartAdapter.ExerciseData> allExerciseData = new ArrayList<>();
            if (exercises.isEmpty()) {
                runOnUiThread(() -> chartAdapter.setData(new ArrayList<>()));
                return;
            }
            AtomicInteger remaining = new AtomicInteger(exercises.size());
            for (Exercise exercise : exercises) {
                repository.getExerciseProgress(exercise.id, progress -> {
                    if (progress != null && !progress.isEmpty()) {
                        synchronized (allExerciseData) {
                            allExerciseData.add(new ExerciseChartAdapter.ExerciseData(exercise.name, progress));
                        }
                    }
                    if (remaining.decrementAndGet() == 0) {
                        runOnUiThread(() -> chartAdapter.setData(allExerciseData));
                    }
                });
            }
        });
    }

    private void updateMainCharts(List<WorkoutSession> sessions) {
        Map<Integer, Integer> counts = new TreeMap<>();
        Map<Integer, Long> timestamps = new TreeMap<>();
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        
        for (int i = 0; i < currentDays; i++) {
            counts.put(i, 0);
            cal.setTimeInMillis(now);
            cal.add(Calendar.DAY_OF_YEAR, -(currentDays - 1 - i));
            timestamps.put(i, cal.getTimeInMillis());
        }

        for (WorkoutSession session : sessions) {
            int dayOffset = (int) ((now - session.startTime) / (1000 * 60 * 60 * 24));
            if (dayOffset >= 0 && dayOffset < currentDays) {
                int key = currentDays - 1 - dayOffset;
                counts.put(key, counts.getOrDefault(key, 0) + 1);
            }
        }

        ValueFormatter dateFormatter = new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                Long ts = timestamps.get((int) value);
                return ts != null ? mFormat.format(new Date(ts)) : "";
            }
        };

        // Update BarChart
        List<BarEntry> barEntries = new ArrayList<>();
        List<Entry> lineEntries = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : counts.entrySet()) {
            barEntries.add(new BarEntry(entry.getKey(), entry.getValue()));
            lineEntries.add(new Entry(entry.getKey(), entry.getValue()));
        }

        BarDataSet barSet = new BarDataSet(barEntries, "Træninger");
        barSet.setColor(0xFF6200EE);
        barChart.getXAxis().setValueFormatter(dateFormatter);
        barChart.setData(new BarData(barSet));
        barChart.invalidate();

        // Update LineChart
        LineDataSet lineSet = new LineDataSet(lineEntries, "Træninger");
        lineSet.setColor(0xFF6200EE);
        lineSet.setCircleColor(0xFF3700B3);
        lineSet.setLineWidth(2f);
        lineSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lineSet.setDrawFilled(true);
        mainLineChart.getXAxis().setValueFormatter(dateFormatter);
        mainLineChart.setData(new LineData(lineSet));
        mainLineChart.invalidate();
    }
}
