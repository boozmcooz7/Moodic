package com.example.moodic.activities;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
        holder.bind(track);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    public class TrackViewHolder extends RecyclerView.ViewHolder {
        private ImageView thumbnail;
        private TextView title;
        private TextView artist;
        private Button playButton;
        private Button favoriteButton;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.trackThumbnail);
            title = itemView.findViewById(R.id.trackTitle);
            artist = itemView.findViewById(R.id.trackArtist);
            playButton = itemView.findViewById(R.id.playButton);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
        }

        public void bind(Track track) {
            // Set text
            title.setText(track.getTitle());
            artist.setText(track.getArtist());

            // Load thumbnail with Glide
            if (track.getThumbnailUrl() != null && !track.getThumbnailUrl().isEmpty()) {
                Glide.with(context)
                        .load(track.getThumbnailUrl())
                        .into(thumbnail);
            }

            // Play button - open YouTube
            playButton.setOnClickListener(v -> {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
                intent.setData(android.net.Uri.parse(track.getYoutubeUrl()));
                context.startActivity(intent);
            });

            // Favorite button - save to Firebase (TODO)
            favoriteButton.setOnClickListener(v -> {
                favoriteButton.setText("❤️ Favorited");
                favoriteButton.setEnabled(false);
                // TODO: Save to Firebase favorites
            });
        }
    }
}