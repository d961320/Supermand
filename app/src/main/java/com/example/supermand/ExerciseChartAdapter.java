package com.example.supermand;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supermand.data.ExerciseProgress;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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
        int selectedMetricId = R.id.btnMainMetric;

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

        holder.toggleGroup.removeOnButtonCheckedListener(holder.chartTypeListener);
        holder.chartTypeListener = (group, checkedId, isChecked) -> {
            if (isChecked) {
                data.showLine = (checkedId == R.id.btnLine);
                updateVisibility(holder, data);
            }
        };
        holder.toggleGroup.addOnButtonCheckedListener(holder.chartTypeListener);
        holder.toggleGroup.check(data.showLine ? R.id.btnLine : R.id.btnBar);

        holder.metricToggleGroup.removeOnButtonCheckedListener(holder.metricListener);
        holder.metricListener = (group, checkedId, isChecked) -> {
            if (isChecked) {
                data.selectedMetricId = checkedId;
                updateCharts(holder, data);
            }
        };
        holder.metricToggleGroup.addOnButtonCheckedListener(holder.metricListener);
        
        boolean hasTimeData = false;
        for (ExerciseProgress p : data.progress) {
            if (p.reps > 0) {
                hasTimeData = true;
                break;
            }
        }
        holder.btnPace.setVisibility(hasTimeData ? View.VISIBLE : View.GONE);
        holder.btnTime.setVisibility(hasTimeData ? View.VISIBLE : View.GONE);
        
        holder.metricToggleGroup.check(data.selectedMetricId);
        
        updateVisibility(holder, data);
        updateCharts(holder, data);
    }

    private void updateVisibility(ViewHolder holder, ExerciseData data) {
        holder.lineChart.setVisibility(data.showLine ? View.VISIBLE : View.GONE);
        holder.barChart.setVisibility(data.showLine ? View.GONE : View.VISIBLE);
    }

    private void updateCharts(ViewHolder holder, ExerciseData data) {
        setupLineChart(holder.lineChart, data.progress, data.selectedMetricId);
        setupBarChart(holder.barChart, data.progress, data.selectedMetricId);
    }

    private void setupLineChart(LineChart chart, List<ExerciseProgress> progress, int metricId) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.getAxisRight().setEnabled(false);
        
        int textColor = getThemeColor(chart.getContext(), android.R.attr.textColorPrimary);
        chart.getLegend().setTextColor(textColor);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(textColor);
        xAxis.setDrawLabels(true);
        xAxis.setValueFormatter(createDateFormatter(progress));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(textColor);
        leftAxis.setDrawLabels(true);

        List<Entry> entries = new ArrayList<>();
        String label = getMetricLabel(metricId);

        for (int i = 0; i < progress.size(); i++) {
            entries.add(new Entry(i, calculateMetricValue(progress.get(i), metricId)));
        }

        LineDataSet set = new LineDataSet(entries, label);
        set.setLineWidth(2f);
        set.setCircleRadius(3f);
        set.setDrawValues(true);
        set.setValueTextColor(textColor);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setColor(0xFF6200EE);
        set.setCircleColor(0xFF3700B3);

        if (metricId == R.id.btnPace) {
            set.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int minutes = (int) value;
                    int seconds = (int) ((value - minutes) * 60);
                    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
                }
            });
        }

        chart.setData(new LineData(set));
        chart.invalidate();
    }

    private void setupBarChart(BarChart chart, List<ExerciseProgress> progress, int metricId) {
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0f);
        
        int textColor = getThemeColor(chart.getContext(), android.R.attr.textColorPrimary);
        chart.getLegend().setTextColor(textColor);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(textColor);
        xAxis.setDrawLabels(true);
        xAxis.setValueFormatter(createDateFormatter(progress));

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(textColor);
        leftAxis.setDrawLabels(true);

        List<BarEntry> entries = new ArrayList<>();
        String label = getMetricLabel(metricId);

        for (int i = 0; i < progress.size(); i++) {
            entries.add(new BarEntry(i, calculateMetricValue(progress.get(i), metricId)));
        }

        BarDataSet set = new BarDataSet(entries, label);
        set.setColor(0xFF6200EE);
        set.setDrawValues(true);
        set.setValueTextColor(textColor);

        chart.setData(new BarData(set));
        chart.invalidate();
    }

    private float calculateMetricValue(ExerciseProgress p, int metricId) {
        if (metricId == R.id.btnMainMetric) return (float) p.weight;
        if (metricId == R.id.btnVolume) return (float) p.volume;
        if (metricId == R.id.btnPace) {
            return p.weight > 0 ? (float) ((p.reps / 60.0) / p.weight) : 0f;
        }
        if (metricId == R.id.btnTime) return (float) (p.reps / 60.0);
        return 0f;
    }

    private String getMetricLabel(int metricId) {
        if (metricId == R.id.btnMainMetric) return "Enhed";
        if (metricId == R.id.btnVolume) return "Volumen";
        if (metricId == R.id.btnPace) return "Tempo (min/km)";
        if (metricId == R.id.btnTime) return "Tid (min)";
        return "";
    }

    private int getThemeColor(Context context, int attr) {
        TypedValue typedValue = new TypedValue();
        if (context.getTheme().resolveAttribute(attr, typedValue, true)) {
            if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return typedValue.data;
            } else {
                return ContextCompat.getColor(context, typedValue.resourceId);
            }
        }
        return Color.GRAY;
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
        MaterialButtonToggleGroup metricToggleGroup;
        View btnPace, btnTime;
        MaterialButtonToggleGroup.OnButtonCheckedListener chartTypeListener;
        MaterialButtonToggleGroup.OnButtonCheckedListener metricListener;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvExerciseName);
            lineChart = itemView.findViewById(R.id.exerciseLineChart);
            barChart = itemView.findViewById(R.id.exerciseBarChart);
            toggleGroup = itemView.findViewById(R.id.toggleChartType);
            metricToggleGroup = itemView.findViewById(R.id.metricToggleGroup);
            btnPace = itemView.findViewById(R.id.btnPace);
            btnTime = itemView.findViewById(R.id.btnTime);
        }
    }
}
