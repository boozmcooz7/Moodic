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

import com.example.moodic.R;
import com.example.moodic.models.Track;

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private final Context context;
    private List<Track> tracks = new ArrayList<>();

    public TrackAdapter(Context context) {
        this.context = context;
    }

    public void setTracks(List<Track> tracks) {
        if (tracks != null) {
            this.tracks = tracks;
            notifyDataSetChanged(); // כפיית ציור מחדש של הרשימה על המסך
        }
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // קישור לקובץ ה-Layout המאוחד והמשותף
        View view = LayoutInflater.from(context).inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = tracks.get(position);

        // מניעת קריסות באמצעות בדיקות null קשיחות
        String title = track.getTitle() != null ? track.getTitle() : "Unknown Title";
        String artist = track.getArtist() != null ? track.getArtist() : "Unknown Artist";

        holder.titleText.setText(title);
        holder.artistText.setText(artist);

        // הגדרת כפתור הניגון הבטוח שיוצא לאפליקציית יוטיוב הרשמית למניעת שגיאות Playback
        if (holder.playButton != null) {
            holder.playButton.setOnClickListener(v -> {
                String url = track.getYoutubeUrl();
                if (url == null || url.isEmpty()) {
                    url = "https://www.youtube.com/watch?v=" + track.getId();
                }
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(context, "Cannot open YouTube link", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    // הצהרה יחידה ומדויקת על ה-ViewHolder ללא כפילויות
    public static class TrackViewHolder extends RecyclerView.ViewHolder {
        TextView titleText;
        TextView artistText;
        Button playButton;
        ImageView thumbnail;
        Button favoriteButton;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText      = itemView.findViewById(R.id.trackTitle);
            artistText     = itemView.findViewById(R.id.trackArtist);
            playButton     = itemView.findViewById(R.id.playButton);
            thumbnail      = itemView.findViewById(R.id.trackThumbnail);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);

            // במסך התוצאות הרגיל מסתירים את כפתור המועדפים (הוא נחוץ רק ב-FavoritesAdapter)
            if (favoriteButton != null) {
                favoriteButton.setVisibility(View.GONE);
            }
        }
    }
}