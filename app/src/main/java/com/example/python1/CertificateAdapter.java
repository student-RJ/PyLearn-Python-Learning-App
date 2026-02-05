package com.example.python1;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class CertificateAdapter extends RecyclerView.Adapter<CertificateAdapter.ViewHolder> {

    private Context context;
    private List<CertificateModel> certificateList;

    public CertificateAdapter(Context context, List<CertificateModel> certificateList) {
        this.context = context;
        this.certificateList = certificateList;
    }

    @NonNull
    @Override
    public CertificateAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.certificate_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CertificateAdapter.ViewHolder holder, int position) {
        CertificateModel cert = certificateList.get(position);
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Set basic text info
        holder.tvTopic.setText(cert.getTopic());
        holder.tvDate.setText("Date: " + cert.getDate());
        holder.tvScore.setText("Score: " + cert.getScore() + "%");

        // Set preview info
        holder.tvPreviewUsername.setText("Loading...");
        holder.tvPreviewTopic.setText(cert.getTopic()); // ✅ Ensures topic is set
        holder.tvPreviewDate.setText("Date: " + cert.getDate()); // ✅ Fix: Add "Date:"
        holder.tvPreviewScore.setText("Score: " + cert.getScore() + "%"); // ✅ Fix: Add "Score:"

        // Ensure preview is hidden by default and icon is correct
        holder.framePreview.setVisibility(View.GONE);
        holder.btnView.setImageResource(R.drawable.eye_close); // ✅ Reset icon on refresh

        // Fetch user name from Firestore
        // Fetch user name from Firestore inside onBindViewHolder
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("Profile_details")) {
                        // Profile_details is a Map
                        Map<String, Object> profileDetails = (Map<String, Object>) documentSnapshot.get("Profile_details");
                        if (profileDetails != null && profileDetails.containsKey("name")) {
                            String userName = (String) profileDetails.get("name");
                            holder.tvPreviewUsername.setText(userName);
                        } else {
                            holder.tvPreviewUsername.setText("User");
                        }
                    } else {
                        holder.tvPreviewUsername.setText("User");
                    }
                })
                .addOnFailureListener(e -> holder.tvPreviewUsername.setText("User"));


        // Toggle preview visibility and icon
        holder.btnView.setOnClickListener(v -> {
            if (holder.framePreview.getVisibility() == View.GONE) {
                holder.framePreview.setVisibility(View.VISIBLE);
                holder.btnView.setImageResource(R.drawable.eye_open);
            } else {
                holder.framePreview.setVisibility(View.GONE);
                holder.btnView.setImageResource(R.drawable.eye_close);
            }
        });

        // Shared logic for download/share
        View.OnClickListener generateAndHandlePDF = view -> {
            FirebaseFirestore.getInstance().collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String userName = "User";
                        if (documentSnapshot.exists() && documentSnapshot.contains("Profile_details")) {
                            Map<String, Object> profileDetails = (Map<String, Object>) documentSnapshot.get("Profile_details");
                            if (profileDetails != null && profileDetails.containsKey("name")) {
                                userName = (String) profileDetails.get("name");
                            }
                        }

                        CertificateModel model = certificateList.get(position);
                        Uri pdfUri = CertificatePDFGenerator.generateCertificate(
                                context,
                                userName,
                                model.getTopic(),
                                model.getDate(),
                                model.getScore()
                        );

                        if (pdfUri != null) {
                            int id = view.getId();
                            if (id == R.id.btn_download_cert) {
                                Intent openIntent = new Intent(Intent.ACTION_VIEW);
                                openIntent.setDataAndType(pdfUri, "application/pdf");
                                openIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY);
                                try {
                                    context.startActivity(openIntent);
                                } catch (Exception e) {
                                    Toast.makeText(context, "No PDF viewer found.", Toast.LENGTH_SHORT).show();
                                }
                            } else if (id == R.id.btn_share_cert) {
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.setType("application/pdf");
                                shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);
                                shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                context.startActivity(Intent.createChooser(shareIntent, "Share Certificate"));
                            }
                        } else {
                            Toast.makeText(context, "Failed to generate certificate", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Failed to fetch name", Toast.LENGTH_SHORT).show());
        };

        holder.btnDownload.setOnClickListener(generateAndHandlePDF);
        holder.btnShare.setOnClickListener(generateAndHandlePDF);
    }



    @Override
    public int getItemCount() {
        return certificateList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTopic, tvDate, tvScore;
        TextView tvPreviewUsername, tvPreviewTopic, tvPreviewDate, tvPreviewScore;
        ImageButton btnView, btnDownload, btnShare;
        View framePreview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTopic = itemView.findViewById(R.id.tv_cert_topic);
            tvDate = itemView.findViewById(R.id.tv_cert_date);
            tvScore = itemView.findViewById(R.id.tv_cert_score);

            tvPreviewUsername = itemView.findViewById(R.id.tv_preview_username);
            tvPreviewTopic = itemView.findViewById(R.id.tv_preview_topic);
            tvPreviewDate = itemView.findViewById(R.id.tv_preview_date);
            tvPreviewScore = itemView.findViewById(R.id.tv_preview_score);

            btnView = itemView.findViewById(R.id.btn_view_cert);
            btnDownload = itemView.findViewById(R.id.btn_download_cert);
            btnShare = itemView.findViewById(R.id.btn_share_cert);

            framePreview = itemView.findViewById(R.id.frame_preview);
        }
    }
}
