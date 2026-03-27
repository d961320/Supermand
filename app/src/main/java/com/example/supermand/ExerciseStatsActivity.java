package com.example.supermand;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.supermand.data.ExerciseProgress;
import com.example.supermand.data.WorkoutRepository;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
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
                updateChart(checkedId == R.id.btnWeight);
            }
        });

        loadProgress();
    }

    private void setupChart() {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
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

    private void loadProgress() {
        repository.getExerciseProgress(exerciseId, progress -> {
            this.progressData = progress;
            runOnUiThread(() -> updateChart(metricToggleGroup.getCheckedButtonId() == R.id.btnWeight));
        });
    }

    private void updateChart(boolean showWeight) {
        if (progressData.isEmpty()) return;

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < progressData.size(); i++) {
            ExerciseProgress p = progressData.get(i);
            float val = showWeight ? (float) p.weight : (float) p.volume;
            entries.add(new Entry(i, val));
        }

        String label = showWeight ? "Vægt (kg)" : "Volumen (kg * reps)";
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(getResources().getColor(R.color.purple_500, getTheme()));
        dataSet.setCircleColor(getResources().getColor(R.color.purple_700, getTheme()));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(true);

        chart.setData(new LineData(dataSet));
        chart.invalidate();
    }
}
