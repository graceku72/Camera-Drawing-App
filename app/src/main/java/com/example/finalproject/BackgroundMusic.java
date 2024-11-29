package com.example.finalproject;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class BackgroundMusic extends Service implements MediaPlayer.OnPreparedListener {
    private static final String ACTION_PLAY = "com.example-action.PLAY";
    MediaPlayer mediaPlayer = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand (Intent intent, int flags, int startId) {
        if (intent != null && ACTION_PLAY.equals(intent.getAction())) {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.background_music);
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        }
        return START_STICKY;
    }

    public void onPrepared (MediaPlayer player) {
        player.start();
    }
}
