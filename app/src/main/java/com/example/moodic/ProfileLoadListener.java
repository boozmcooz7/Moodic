package com.example.moodic;

import com.example.moodic.models.User;

public interface ProfileLoadListener {
    void onProfileLoaded(User user);
    void onProfileLoadFailed(String error);
}