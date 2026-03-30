package com.example.moodic.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodic.R;
import com.example.moodic.data.FirebaseManager;
import com.example.moodic.models.Track;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar loadingBar;
    private TextView emptyText;
    private FavoritesAdapter adapter;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = findViewById(R.id.favoritesRecyclerView);
        loadingBar = findViewById(R.id.loadingBar);
        emptyText = findViewById(R.id.emptyText);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoritesAdapter(this, track -> removeFavorite(track));
        recyclerView.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        loadingBar.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);

        FirebaseManager.getInstance().loadFavoriteTracks(uid, new FirebaseManager.FavoritesLoadListener() {
            @Override
            public void onFavoritesLoaded(List<Track> tracks) {
                loadingBar.setVisibility(View.GONE);
                if (tracks.isEmpty()) {
                    emptyText.setVisibility(View.VISIBLE);
                } else {
                    adapter.setTracks(tracks);
                }
            }

            @Override
            public void onFavoritesLoadFailed(String error) {
                loadingBar.setVisibility(View.GONE);
                Toast.makeText(FavoritesActivity.this,
                        "Failed to load favorites: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeFavorite(Track track) {
        FirebaseManager.getInstance().removeFavoriteTrack(uid, track.getId(),
                new FirebaseManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        adapter.removeTrack(track);
                        if (adapter.getItemCount() == 0) {
                            emptyText.setVisibility(View.VISIBLE);
                        }
                        Toast.makeText(FavoritesActivity.this,
                                "Removed from favorites", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(FavoritesActivity.this,
                                "Failed to remove: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
