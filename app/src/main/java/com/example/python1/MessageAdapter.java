package com.example.python1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout; // Keep this if ai_message_layout is LinearLayout
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout; // Import ConstraintLayout
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Import Glide

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (message.isUser()) {
            holder.userMessageLayout.setVisibility(View.VISIBLE);
            holder.aiMessageLayout.setVisibility(View.GONE);

            holder.userMessageText.setText(message.getText());
            // Format time if needed, or just display directly
            holder.userMessageTime.setText(message.getTimestamp()); // Assuming Message has getTimestamp()

            // Load user avatar
            if (message.getAvatarUrl() != null && !message.getAvatarUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(message.getAvatarUrl())
                        .placeholder(R.drawable.profile) // Placeholder image
                        .error(R.drawable.profile)       // Error image
                        .into(holder.userAvatarImage);
            } else {
                holder.userAvatarImage.setImageResource(R.drawable.profile); // Default if URL is null/empty
            }

            // Set user name
            holder.userNameText.setText(message.getUserName());

        } else {
            holder.userMessageLayout.setVisibility(View.GONE);
            holder.aiMessageLayout.setVisibility(View.VISIBLE);

            holder.aiMessageText.setText(message.getText());
            // Format time if needed, or just display directly
            holder.aiMessageTime.setText(message.getTimestamp()); // Assuming Message has getTimestamp()
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ViewHolder class
    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        // User message views
        ConstraintLayout userMessageLayout; // CHANGED FROM LinearLayout to ConstraintLayout
        TextView userMessageText;
        TextView userMessageTime;
        ImageView userAvatarImage;
        TextView userNameText;

        // AI message views
        LinearLayout aiMessageLayout; // This is still a LinearLayout in your XML
        TextView aiMessageText;
        TextView aiMessageTime;
        // No AI avatar ImageView or TextView for name, as per your XML

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            // Initialize user message views
            // CRUCIAL CHANGE HERE: Cast to ConstraintLayout
            userMessageLayout = itemView.findViewById(R.id.user_message_layout);
            userMessageText = itemView.findViewById(R.id.user_message_text);
            userMessageTime = itemView.findViewById(R.id.user_message_time);
            userAvatarImage = itemView.findViewById(R.id.user_avatar_image);
            userNameText = itemView.findViewById(R.id.user_name_text);

            // Initialize AI message views
            // This remains LinearLayout as per your XML
            aiMessageLayout = itemView.findViewById(R.id.ai_message_layout);
            aiMessageText = itemView.findViewById(R.id.ai_message_text);
            aiMessageTime = itemView.findViewById(R.id.ai_message_time);
        }
    }
}