package com.example.supermand;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.supermand.data.ExerciseProgress;
import com.example.supermand.data.WorkoutRepository;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExerciseStatsActivity extends AppCompatActivity {
    private WorkoutRepository repository;
    private LineChart chart;
    private TextView tvExerciseName;
    private MaterialButtonToggleGroup metricToggleGroup;
    private List<ExerciseProgress> progressData = new ArrayList<>();
    private int exerciseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_stats);

        repository = new WorkoutRepository(getApplication());
        chart = findViewById(R.id.exerciseChart);
        tvExerciseName = findViewById(R.id.tvExerciseName);
        metricToggleGroup = findViewById(R.id.metricToggleGroup);

        exerciseId = getIntent().getIntExtra("EXERCISE_ID", -1);
        String exerciseName = getIntent().getStringExtra("EXERCISE_NAME");
        tvExerciseName.setText(exerciseName);

        setupChart();

        metricToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                updateChart(checkedId);
            }
        });

        loadProgress();
    }

    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);
        int textColor = getThemeColor(android.R.attr.textColorPrimary);
        chart.getLegend().setTextColor(textColor);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(textColor);
        xAxis.setGridColor(getThemeColor(android.R.attr.textColorSecondary));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(textColor);
        leftAxis.setGridColor(getThemeColor(android.R.attr.textColorSecondary));

        chart.getAxisRight().setEnabled(false);
        
        xAxis.setValueFormatter(new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < progressData.size()) {
                    return mFormat.format(new Date(progressData.get(index).startTime));
                }
                return "";
            }
        });
    }

    private int getThemeColor(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getTheme().resolveAttribute(attr, typedValue, true)) {
            return typedValue.data;
        }
        return Color.GRAY;
    }

    private void loadProgress() {
        repository.getExerciseProgress(exerciseId, progress -> {
            this.progressData = progress;
            runOnUiThread(() -> {
                boolean hasTime = false;
                for (ExerciseProgress p : progressData) {
                    if (p.reps > 0) {
                        hasTime = true;
                        break;
                    }
                }
                
                if (hasTime) {
                    findViewById(R.id.btnPace).setVisibility(View.VISIBLE);
                    findViewById(R.id.btnTime).setVisibility(View.VISIBLE);
                    // For cardio exercises like "Løb", let's default to Pace if available
                    Button btnPace = findViewById(R.id.btnPace);
                    metricToggleGroup.check(R.id.btnPace);
                } else {
                    findViewById(R.id.btnPace).setVisibility(View.GONE);
                    findViewById(R.id.btnTime).setVisibility(View.GONE);
                    metricToggleGroup.check(R.id.btnMainMetric);
                }
                
                updateChart(metricToggleGroup.getCheckedButtonId());
            });
        });
    }

    private void updateChart(int checkedId) {
        if (progressData.isEmpty()) return;

        List<Entry> entries = new ArrayList<>();
        String label = "";

        for (int i = 0; i < progressData.size(); i++) {
            ExerciseProgress p = progressData.get(i);
            float val = 0;

            if (checkedId == R.id.btnMainMetric) {
                val = (float) p.weight;
                label = "Km / Vægt";
            } else if (checkedId == R.id.btnVolume) {
                val = (float) p.volume;
                label = "Volumen";
            } else if (checkedId == R.id.btnPace) {
                if (p.weight > 0) {
                    val = (float) ((p.reps / 60.0) / p.weight);
                }
                label = "Tempo (min/km)";
            } else if (checkedId == R.id.btnTime) {
                val = (float) (p.reps / 60.0);
                label = "Tid (min)";
            }
            entries.add(new Entry(i, val));
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(getResources().getColor(R.color.purple_500, getTheme()));
        dataSet.setCircleColor(getResources().getColor(R.color.purple_700, getTheme()));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(getThemeColor(android.R.attr.textColorPrimary));
        
        if (checkedId == R.id.btnPace) {
            dataSet.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int minutes = (int) value;
                    int seconds = (int) ((value - minutes) * 60);
                    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
                }
            });
        }

        chart.setData(new LineData(dataSet));
        chart.invalidate();
    }
}
