package com.example.moodic;

public interface AuthCompleteListener{
    void onSuccess(String uid);
    void onFailure(String error);
}
