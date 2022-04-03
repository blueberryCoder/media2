package com.blueberry.videoplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnMediaPlayerAudio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent  = new Intent(MainActivity.this, MediaPlayerAudioActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.btnMediaPlayerVideo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent  = new Intent(MainActivity.this, MediaPlayerVideoActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });
        findViewById(R.id.btnAudioTrack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent  = new Intent(MainActivity.this, AudioTrackActivity.class);
                MainActivity.this.startActivity(intent);

            }
        });
    }
}