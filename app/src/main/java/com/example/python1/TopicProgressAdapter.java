package com.example.python1; // Your package name

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TopicProgressAdapter extends RecyclerView.Adapter<TopicProgressAdapter.ViewHolder> {

    private List<TopicProgressModel> topicList;
    private Context context;

    // Constructor
    public TopicProgressAdapter(List<TopicProgressModel> topicList) {
        this.topicList = topicList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_topic_progress, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TopicProgressModel model = topicList.get(position);

        holder.tvTopic.setText("📘 " + model.getTopic());

        // Use concise labels for stats
        holder.tvAverageScore.setText(String.format(Locale.getDefault(),"📊 Avg: %.1f", model.getAverageScore()));
        holder.tvHighestScore.setText("🏆 High: " + model.getHighestScore());
        holder.tvQuizzesAttempted.setText("📝 Attempts: " + model.getQuizzesAttempted());
        holder.tvTotalScore.setText("📈 Total: " + model.getTotalScore());

        // Quiz Completed Status and Badge
        if (model.isQuizCompleted()) {
            holder.tvQuizCompleted.setText("✅ Quiz Completed: True");
            holder.tvQuizCompleted.setTextColor(ContextCompat.getColor(context, R.color.progress_excellent)); // Example: Green
            holder.tvBadge.setText("🏅 Completed");
            holder.tvBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvQuizCompleted.setText("❌ Quiz Completed: False");
            holder.tvQuizCompleted.setTextColor(ContextCompat.getColor(context, R.color.progress_poor)); // Example: Red or Grey
            holder.tvBadge.setVisibility(View.GONE);
        }

        // Use concise labels for time spent
        holder.tvBestTimeSpent.setText("⏱️ Best: " + formatTime(model.getBestTimeSpent()));
        holder.tvTotalTimeSpent.setText("🕒 Total: " + formatTime(model.getTotalTimeSpent()));

        // Normalize progress
        final int MAX_POSSIBLE_SCORE_FOR_TOPIC = 1000; // Example max score
        int progress;
        if (model.getTotalScore() >= MAX_POSSIBLE_SCORE_FOR_TOPIC) {
            progress = 100;
        } else if (model.getTotalScore() <= 0) {
            progress = 0;
        } else {
            progress = (int) ((model.getTotalScore() / (float) MAX_POSSIBLE_SCORE_FOR_TOPIC) * 100);
        }

        holder.progressBar.setProgressCompat(progress, true);
        holder.tvProgressPercent.setText(String.format(Locale.getDefault(),"%d%%", progress));

        // Change progress bar color and progress text color
        if (progress >= 90) {
            holder.progressBar.setIndicatorColor(ContextCompat.getColor(context, R.color.progress_excellent));
            holder.tvProgressPercent.setTextColor(ContextCompat.getColor(context, R.color.progress_excellent));
        } else if (progress >= 75) {
            holder.progressBar.setIndicatorColor(ContextCompat.getColor(context, R.color.progress_good));
            holder.tvProgressPercent.setTextColor(ContextCompat.getColor(context, R.color.progress_good));
        } else if (progress >= 50) {
            holder.progressBar.setIndicatorColor(ContextCompat.getColor(context, R.color.progress_average));
            holder.tvProgressPercent.setTextColor(ContextCompat.getColor(context, R.color.progress_average));
        } else {
            holder.progressBar.setIndicatorColor(ContextCompat.getColor(context, R.color.progress_poor));
            holder.tvProgressPercent.setTextColor(ContextCompat.getColor(context, R.color.progress_poor));
        }

        // holder.tvProgressQuizzesInfo.setText(...); // If you have data for "X/Y Quizzes"

        setupBarChart(holder.barChart, model);

        holder.btnShare.setOnClickListener(v -> {
            // Use concise labels in share content as well
            String shareContent = "📘 Topic: " + model.getTopic() +
                    "\n📊 Avg: " + String.format(Locale.getDefault(),"%.1f", model.getAverageScore()) +
                    "\n🏆 High: " + model.getHighestScore() +
                    "\n📝 Attempts: " + model.getQuizzesAttempted() +
                    "\n📈 Total: " + model.getTotalScore() +
                    "\n" + (model.isQuizCompleted() ? "✅ Quiz Completed: True" : "❌ Quiz Completed: False") +
                    "\n⏱️ Best: " + formatTime(model.getBestTimeSpent()) + // Concise label
                    "\n🕒 Total: " + formatTime(model.getTotalTimeSpent()) + // Concise label
                    "\n🔥 Keep learning with PyLearn!";

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "My Progress in " + model.getTopic());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
            context.startActivity(Intent.createChooser(shareIntent, "Share via"));
        });
    }

    private void setupBarChart(BarChart barChart, TopicProgressModel model) {
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.setDrawValueAboveBar(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(new String[]{"Avg Score"}));
        // Ensure R.color.soft_pink is defined in your colors.xml
        xAxis.setTextColor(ContextCompat.getColor(context, R.color.soft_pink));


        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(10f); // Assuming score is out of 10
        leftAxis.setGranularity(2f);
        // Ensure R.color.soft_pink is defined in your colors.xml
        leftAxis.setTextColor(ContextCompat.getColor(context, R.color.red));
        leftAxis.setDrawGridLines(true);

        barChart.getAxisRight().setEnabled(false);

        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, (float) model.getAverageScore()));

        BarDataSet dataSet = new BarDataSet(entries, "Average Score");
        // Ensure R.color.soft_pink is defined in your colors.xml
        dataSet.setValueTextColor(ContextCompat.getColor(context, R.color.progress_poor));
        dataSet.setValueTextSize(10f);

        double avgScore = model.getAverageScore();
        if (avgScore >= 8) {
            dataSet.setColor(ContextCompat.getColor(context, R.color.progress_excellent));
        } else if (avgScore >= 6) {
            dataSet.setColor(ContextCompat.getColor(context, R.color.progress_good));
        } else if (avgScore >= 4) {
            dataSet.setColor(ContextCompat.getColor(context, R.color.progress_average));
        } else {
            dataSet.setColor(ContextCompat.getColor(context, R.color.progress_poor));
        }

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.4f);

        barChart.setData(barData);
        barChart.setFitBars(true);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private String formatTime(long millis) {
        if (millis < 0) millis = 0;
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d hr %d min %d sec", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format(Locale.getDefault(), "%d min %d sec", minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%d sec", seconds);
        }
    }

    @Override
    public int getItemCount() {
        return topicList == null ? 0 : topicList.size();
    }

    public void setTopicProgressList(List<TopicProgressModel> newTopicList) {
        this.topicList = newTopicList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvAverageScore, tvHighestScore, tvQuizzesAttempted, tvTotalScore,
                tvBestTimeSpent, tvTotalTimeSpent, tvQuizCompleted, tvProgressPercent;
        Chip tvBadge;
        LinearProgressIndicator progressBar;
        BarChart barChart;
        Button btnShare;

        public ViewHolder(View view) {
            super(view);
            tvTopic = view.findViewById(R.id.tvTopicName);
            tvAverageScore = view.findViewById(R.id.tvAverageScore);
            tvHighestScore = view.findViewById(R.id.tvHighestScore);
            tvQuizzesAttempted = view.findViewById(R.id.tvQuizzesAttempted);
            tvTotalScore = view.findViewById(R.id.tvTotalScore);
            tvBestTimeSpent = view.findViewById(R.id.tvBestTimeSpent);
            tvTotalTimeSpent = view.findViewById(R.id.tvTotalTimeSpent);
            tvQuizCompleted = view.findViewById(R.id.tvQuizCompleted);
            tvBadge = view.findViewById(R.id.tvBadge);
            progressBar = view.findViewById(R.id.progressBar);
            barChart = view.findViewById(R.id.barChart);
            btnShare = view.findViewById(R.id.btnShare);
            tvProgressPercent = view.findViewById(R.id.tvProgressPercent);
        }
    }
}