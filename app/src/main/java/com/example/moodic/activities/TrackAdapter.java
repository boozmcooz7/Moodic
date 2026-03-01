package com.example.moodic.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.moodic.R;
import com.example.moodic.models.Track;

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {
    private List<Track> tracks;
    private Context context;

    public TrackAdapter(Context context) {
        this.context = context;
        this.tracks = new ArrayList<>();
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = tracks.get(position);

        holder.title.setText(track.getTitle());
        holder.artist.setText(track.getArtist());

        // Optimized Image Loading
        if (track.getThumbnailUrl() != null && !track.getThumbnailUrl().isEmpty()) {
            Glide.with(context)
                    .load(track.getThumbnailUrl())
                    .placeholder(R.drawable.rounded_info_box) // Fallback while loading
                    .into(holder.thumbnail);
        }

        // IMPROVED: Direct YouTube Launch
        holder.playButton.setOnClickListener(v -> {
            String url = track.getYoutubeUrl();
            if (url != null && !url.isEmpty()) {
                try {
                    // Try to launch the YouTube app directly
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } catch (Exception e) {
                    // If YouTube app isn't installed, launch in browser
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(webIntent);
                }
            } else {
                Toast.makeText(context, "Link not available", Toast.LENGTH_SHORT).show();
            }
        });

        holder.favoriteButton.setOnClickListener(v -> {
            holder.favoriteButton.setText("❤️ Favorited");
            holder.favoriteButton.setEnabled(false);
            // TODO: Use FirebaseManager.getInstance().saveFavorite(track) here
            Toast.makeText(context, "Saved to favorites!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    public static class TrackViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbnail;
        public TextView title;
        public TextView artist;
        public Button playButton;
        public Button favoriteButton;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.trackThumbnail);
            title = itemView.findViewById(R.id.trackTitle);
            artist = itemView.findViewById(R.id.trackArtist);
            playButton = itemView.findViewById(R.id.playButton);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
        }
    }
}