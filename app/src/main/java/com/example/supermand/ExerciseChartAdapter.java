package com.example.supermand;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supermand.data.ExerciseProgress;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExerciseChartAdapter extends RecyclerView.Adapter<ExerciseChartAdapter.ViewHolder> {
    private List<ExerciseData> dataList = new ArrayList<>();

    public static class ExerciseData {
        String name;
        List<ExerciseProgress> progress;
        boolean showLine = true;

        public ExerciseData(String name, List<ExerciseProgress> progress) {
            this.name = name;
            this.progress = progress;
        }
    }

    public void setData(List<ExerciseData> data) {
        this.dataList = data;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise_chart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExerciseData data = dataList.get(position);
        holder.tvName.setText(data.name);

        holder.toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                data.showLine = (checkedId == R.id.btnLine);
                updateVisibility(holder, data);
            }
        });

        // Set initial state
        holder.toggleGroup.check(data.showLine ? R.id.btnLine : R.id.btnBar);
        updateVisibility(holder, data);
        
        setupLineChart(holder.lineChart, data.progress);
        setupBarChart(holder.barChart, data.progress);
    }

    private void updateVisibility(ViewHolder holder, ExerciseData data) {
        holder.lineChart.setVisibility(data.showLine ? View.VISIBLE : View.GONE);
        holder.barChart.setVisibility(data.showLine ? View.GONE : View.VISIBLE);
    }

    private void setupLineChart(LineChart chart, List<ExerciseProgress> progress) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        
        xAxis.setValueFormatter(createDateFormatter(progress));

        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < progress.size(); i++) {
            entries.add(new Entry(i, (float) progress.get(i).weight));
        }

        LineDataSet set = new LineDataSet(entries, "Vægt");
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setDrawValues(false);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setColor(0xFF6200EE);
        set.setCircleColor(0xFF3700B3);

        chart.setData(new LineData(set));
        chart.invalidate();
    }

    private void setupBarChart(BarChart chart, List<ExerciseProgress> progress) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0f);
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        
        xAxis.setValueFormatter(createDateFormatter(progress));

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < progress.size(); i++) {
            entries.add(new BarEntry(i, (float) progress.get(i).weight));
        }

        BarDataSet set = new BarDataSet(entries, "Vægt");
        set.setColor(0xFF6200EE);
        set.setDrawValues(false);

        chart.setData(new BarData(set));
        chart.invalidate();
    }

    private ValueFormatter createDateFormatter(List<ExerciseProgress> progress) {
        return new ValueFormatter() {
            private final SimpleDateFormat mFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                if (idx >= 0 && idx < progress.size()) {
                    return mFormat.format(new Date(progress.get(idx).startTime));
                }
                return "";
            }
        };
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        LineChart lineChart;
        BarChart barChart;
        MaterialButtonToggleGroup toggleGroup;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvExerciseName);
            lineChart = itemView.findViewById(R.id.exerciseLineChart);
            barChart = itemView.findViewById(R.id.exerciseBarChart);
            toggleGroup = itemView.findViewById(R.id.toggleChartType);
        }
    }
}
