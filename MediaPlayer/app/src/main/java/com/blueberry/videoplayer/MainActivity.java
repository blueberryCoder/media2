package com.blueberry.videoplayer;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnMediaPlayerAudio).setOnClickListener(
                v -> {
                    Intent intent = new Intent(this,VideoPlayerActivity.class) ;
                    MainActivity.this.startActivity(intent);
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        throw new RuntimeException("kill myself.");
    }
}