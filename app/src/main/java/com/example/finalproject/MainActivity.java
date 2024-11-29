package com.example.finalproject;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    SoundPool soundPool;
    int clickSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        soundPool = new SoundPool.Builder().setMaxStreams(1).build();
        clickSound = soundPool.load(this, R.raw.click_sound, 1);

        Intent musicIntent = new Intent(this, BackgroundMusic.class);
        musicIntent.setAction("com.example-action.PLAY");
        startService(musicIntent);
    }

    private void playClickSound() {
        soundPool.play(clickSound, 1, 1, 0, 0, 1);
    }

    public void navigateToPhotoTagger(View view) {
        playClickSound();
        Intent intent = new Intent(MainActivity.this, PhotoTaggerActivity.class);
        startActivity(intent);
    }

    public void navigatetoSketchTagger(View view) {
        playClickSound();
        Intent intent = new Intent(MainActivity.this, SketchTaggerActivity.class);
        startActivity(intent);
    }

    public void navigatetoStoryTeller(View view) {
        playClickSound();
        Intent intent = new Intent(MainActivity.this, StoryTellerActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        soundPool.release();
    }
}