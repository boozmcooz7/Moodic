package com.example.moodic;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.Channel;

import java.io.IOException;
import java.util.Collections;

public class YoutubeApiExample {

    private static final String TAG = "YoutubeApiExample";
    private static final int RC_SIGN_IN = 1001;

    private final Activity activity;
    private GoogleSignInClient googleSignInClient;

    public YoutubeApiExample(Activity activity) {
        this.activity = activity;
        setupGoogleSignIn();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope("https://www.googleapis.com/auth/youtube.readonly"))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public void signIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void handleSignInResult(Intent data) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
                .addOnSuccessListener(account -> {
                    Log.d(TAG, "Signed in as: " + account.getEmail());
                    fetchYoutubeChannels(account);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Sign-in failed", e));
    }

    private void fetchYoutubeChannels(@NonNull GoogleSignInAccount account) {
        // Set up credentials with OAuth token from GoogleSignIn
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                activity,
                Collections.singleton("https://www.googleapis.com/auth/youtube.readonly")
        );
        credential.setSelectedAccount(account.getAccount());

        YouTube youtubeService = new YouTube.Builder(
                new NetHttpTransport(),
                new GsonFactory(),
                credential
        ).setApplicationName("Moodic").build();

        new Thread(() -> {
            try {
                YouTube.Channels.List request = youtubeService.channels()
                        .list("snippet,contentDetails,statistics");
                request.setMine(true); // Get the signed-in user's channels
                ChannelListResponse response = request.execute();
                for (Channel channel : response.getItems()) {
                    Log.d(TAG, "Channel title: " + channel.getSnippet().getTitle());
                }
            } catch (IOException e) {
                Log.e(TAG, "YouTube API call failed", e);
            }
        }).start();
    }
}
