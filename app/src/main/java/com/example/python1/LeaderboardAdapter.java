package com.example.python1;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.LeaderboardViewHolder> {

    private ArrayList<UserProgress> leaderboardList; // Member variable

    public LeaderboardAdapter(ArrayList<UserProgress> leaderboardList) {
        // --- BEST PRACTICE: Copy the list here ---
        // This ensures the adapter has its own independent copy of the data.
        // Changes to the original list passed in by the Activity won't directly affect
        // the adapter's internal list unless you explicitly call updateData().
        this.leaderboardList = new ArrayList<>(leaderboardList);
    }

    @Override
    public LeaderboardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.leaderboard_item, parent, false);
        return new LeaderboardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(LeaderboardViewHolder holder, int position) {
        UserProgress user = leaderboardList.get(position);

        // Set rank starting from 4
        int rank = position + 4;
        holder.rankTextView.setText(getRankWithSuffix(rank));

        // Set username
        holder.usernameTextView.setText(user.getUsername());

        // Set percentage
        holder.percentageTextView.setText(String.format("%.2f%%", user.getPercentage()));

        // Set progress bar value
        int progress = (int) user.getPercentage();  // Cast to int for ProgressBar
        holder.progressBar.setProgress(progress);

        // Set profile image using Glide
        Glide.with(holder.itemView.getContext())
                .load(user.getProfileImageUrl())
                .placeholder(R.drawable.profile)  // Placeholder image while loading
                .error(R.drawable.profile) // Error image if loading fails
                .into(holder.profileImageView);

        // Set OnClickListener for item click
        holder.itemView.setOnClickListener(v -> onProfileClick(position, holder.itemView.getContext()));
    }


    @Override
    public int getItemCount() {
        return leaderboardList.size();
    }

    public void updateData(ArrayList<UserProgress> newData) {
        this.leaderboardList.clear(); // Clear old data
        this.leaderboardList.addAll(newData); // Add new data
        notifyDataSetChanged();
        Log.d("LeaderboardAdapter", "Adapter updated data. New item count: " + getItemCount());// Changed TAG to LeaderboardAdapter
    }

    private void onProfileClick(int position, Context context) {
        if (position >= 0 && position < leaderboardList.size()) {
            UserProgress selectedUser = leaderboardList.get(position);
            String selectedUserId = selectedUser.getUserId();

            // Calculate rank starting from 4
            int selectedRank = position + 4;
            String selectedUserRank = getRankWithSuffix(selectedRank);

            Intent intent = new Intent(context, User_leaderboard_profile.class);
            intent.putExtra("USER_ID", selectedUserId);
            intent.putExtra("USER_RANK", selectedUserRank); // Pass the rank

            context.startActivity(intent);
        }
    }

    // Helper method to format the rank with suffix
    private String getRankWithSuffix(int rank) {
        // Handle suffix for rank (1st, 2nd, 3rd, etc.)
        if (rank % 10 == 1 && rank != 11) {
            return rank + "st";
        } else if (rank % 10 == 2 && rank != 12) {
            return rank + "nd";
        } else if (rank % 10 == 3 && rank != 13) {
            return rank + "rd";
        } else {
            return rank + "th";
        }
    }

    public static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        TextView rankTextView, usernameTextView, percentageTextView;
        ImageView profileImageView;
        ProgressBar progressBar;

        public LeaderboardViewHolder(View itemView) {
            super(itemView);
            rankTextView = itemView.findViewById(R.id.tvRank); // Rank TextView
            usernameTextView = itemView.findViewById(R.id.tvUsername);
            percentageTextView = itemView.findViewById(R.id.tvPercentage);
            profileImageView = itemView.findViewById(R.id.imgProfile);
            progressBar = itemView.findViewById(R.id.progressBar);  // ProgressBar
        }
    }
}