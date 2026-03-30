package com.example.moodic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Moodic", "Broadcast Received: Audio is becoming noisy!");

        // We send a custom signal that our MainActivity will hear
        Intent pauseSignal = new Intent("STOP_MUSIC_NOW");
        context.sendBroadcast(pauseSignal);
    }
}