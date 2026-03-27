package com.example.supermand;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.supermand.data.WorkoutTemplate;
import java.util.ArrayList;
import java.util.List;

public class TemplateAdapter extends RecyclerView.Adapter<TemplateAdapter.ViewHolder> {
    private List<WorkoutTemplate> templates = new ArrayList<>();
    private OnTemplateClickListener listener;

    public interface OnTemplateClickListener {
        void onTemplateClick(WorkoutTemplate template);
    }

    public void setOnTemplateClickListener(OnTemplateClickListener listener) {
        this.listener = listener;
    }

    public void setTemplates(List<WorkoutTemplate> templates) {
        this.templates = templates;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutTemplate template = templates.get(position);
        holder.tvName.setText(template.name);
        holder.tvDescription.setText(template.description);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTemplateClick(template);
            }
        });
    }

    @Override
    public int getItemCount() {
        return templates.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDescription;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTemplateName);
            tvDescription = itemView.findViewById(R.id.tvTemplateDescription);
        }
    }
}
