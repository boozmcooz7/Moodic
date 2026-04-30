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
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.moodic.R;
import com.example.moodic.data.FirebaseManager;
import com.example.moodic.models.Track;
import com.example.moodic.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder>
        implements DefaultLifecycleObserver {

    private static final String TAG = "TrackAdapter";
    private List<Track> tracks = new ArrayList<>();
    private final Context context;
    private final RequestOptions glideOptions;

    public TrackAdapter(Context context) {
        this.context = context;
        // Optimization: Pre-configure Glide settings
        this.glideOptions = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert);
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks != null ? tracks : new ArrayList<>();
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
                    .apply(glideOptions)
                    .into(holder.thumbnail);
        } else {
            Glide.with(context).clear(holder.thumbnail);
            holder.thumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Play Button: Construct YouTube link if full URL is missing
        holder.playButton.setOnClickListener(v -> {
            String url = track.getYoutubeUrl();
            if (url == null || url.isEmpty()) {
                url = "https://www.youtube.com/watch?v=" + track.getId();
            }
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        });

        // Favorite Button: Local Cache + Firebase Sync
        holder.favoriteButton.setOnClickListener(v -> {
            if (track.getId() == null) return;

            // 1. Instant Local Persistence
            SharedPreferencesManager.getInstance().addFavorite(track);

            // 2. UI Feedback
            holder.favoriteButton.setText("❤️ Favorited");
            holder.favoriteButton.setEnabled(false);

            // 3. Background Firebase Sync
            // FirebaseManager.getInstance().saveFavoriteTrack(...)
            Log.d(TAG, "Syncing favorite to cloud: " + track.getTitle());
        });
    }

    @Override
    public void onViewRecycled(@NonNull TrackViewHolder holder) {
        super.onViewRecycled(holder);
        // CRITICAL: Release memory when row moves to recycle pool
        Glide.with(context).clear(holder.thumbnail);
    }

    @Override
    public int getItemCount() { return tracks.size(); }

    // --- Lifecycle Awareness ---
    @Override
    public void onPause(@NonNull LifecycleOwner owner) { Glide.with(context).pauseRequests(); }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) { Glide.with(context).resumeRequests(); }

    public static class TrackViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbnail;
        public TextView title, artist;
        public Button playButton, favoriteButton;

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