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

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> {

    public interface OnRemoveClickListener {
        void onRemove(Track track);
    }

    private List<Track> tracks = new ArrayList<>();
    private final Context context;
    private final OnRemoveClickListener removeListener;

    public FavoritesAdapter(Context context, OnRemoveClickListener removeListener) {
        this.context = context;
        this.removeListener = removeListener;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = new ArrayList<>(tracks);
        notifyDataSetChanged();
    }

    public void removeTrack(Track track) {
        int index = -1;
        for (int i = 0; i < tracks.size(); i++) {
            if (tracks.get(i).getId().equals(track.getId())) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            tracks.remove(index);
            notifyItemRemoved(index);
        }
    }

    @NonNull
    @Override
    public FavoriteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_track, parent, false);
        return new FavoriteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FavoriteViewHolder holder, int position) {
        Track track = tracks.get(position);

        holder.title.setText(track.getTitle());
        holder.artist.setText(track.getArtist());

        if (track.getThumbnailUrl() != null && !track.getThumbnailUrl().isEmpty()) {
            Glide.with(context)
                    .load(track.getThumbnailUrl())
                    .into(holder.thumbnail);
        }

        // Play — open MusicPlayerActivity with the YouTube URL
        holder.playButton.setOnClickListener(v -> {
            Intent intent = new Intent(context, MusicPlayerActivity.class);
            intent.putExtra("videoUrl", track.getYoutubeUrl());
            intent.putExtra("mood", ""); // no mood context needed here
            context.startActivity(intent);
        });

        // Remove from favorites
        holder.favoriteButton.setText("🗑️ Remove");
        holder.favoriteButton.setOnClickListener(v -> removeListener.onRemove(track));
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    static class FavoriteViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title, artist;
        Button playButton, favoriteButton;

        FavoriteViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.trackThumbnail);
            title = itemView.findViewById(R.id.trackTitle);
            artist = itemView.findViewById(R.id.trackArtist);
            playButton = itemView.findViewById(R.id.playButton);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
        }
    }
}
