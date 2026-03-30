package com.example.moodic.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
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
    private Context context;
    private List<Track> trackList = new ArrayList<>();

    public TrackAdapter(Context context) {
        this.context = context;
    }

    public void setTracks(List<Track> tracks) {
        this.trackList = tracks;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return trackList != null ? trackList.size() : 0;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = trackList.get(position);

        // ✅ FIXED: Used 'title' and 'artist' to match your ViewHolder definition
        holder.title.setText(track.getTitle());
        holder.artist.setText(track.getArtist());

        // Load Thumbnail (Glide)
        if (track.getThumbnailUrl() != null && !track.getThumbnailUrl().isEmpty()) {
            Glide.with(context).load(track.getThumbnailUrl()).into(holder.thumbnail);
        } else {
             //holder.thumbnail.setImageResource(R.drawable.ic_music_note); // Fallback icon
        }

        // Play Button Click
        holder.playButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/watch?v=" + track.getId()));
            context.startActivity(intent);
        });

        // Favorite Button Click
        holder.favoriteButton.setOnClickListener(v -> {
                if (track != null && track.getId() != null) {
                    Log.d("TRACK_ADAPTER", "Favoriting: " + track.getTitle());
                    // TRIGGER YOUR FIREBASE SAVE HERE
                    // FirebaseManager.getInstance().saveToFavorites(uid, track);
                } else {
                    Toast.makeText(context, "Cannot favorite this track", Toast.LENGTH_SHORT).show();
                }
            });
    }

    public static class TrackViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbnail;
        public TextView title;
        public TextView artist;
        public Button playButton;
        public Button favoriteButton;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            // ✅ These IDs must match your item_track.xml exactly
            thumbnail = itemView.findViewById(R.id.trackThumbnail);
            title = itemView.findViewById(R.id.trackTitle);
            artist = itemView.findViewById(R.id.trackArtist);
            playButton = itemView.findViewById(R.id.playButton);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
        }
    }
}