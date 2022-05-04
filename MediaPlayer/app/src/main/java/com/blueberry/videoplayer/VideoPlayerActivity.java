package com.blueberry.videoplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class VideoPlayerActivity extends AppCompatActivity {

    private static final String TAG = "VideoPlayerActivity";
    private SurfaceView mSurfaceView;
    private MediaPlayerController controller ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player_actvitiy);

        findViewById(R.id.btnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(controller != null){
                    controller.start();
                }

            }
        });
        findViewById(R.id.btnPause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(controller != null){
                    controller.pause();
                }
            }
        });
        findViewById(R.id.btnStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(controller != null){
                    controller.stop();
                    finish();
                }
            }
        });

        mSurfaceView = findViewById(R.id.surfaceView);

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                Log.i(TAG, "surfaceCreated: holder:");
                controller = new MediaPlayerController();
                controller.init(holder.getSurface(),VideoPlayerActivity.this.getAssets(),"captain_women.mp4");
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                Log.i(TAG, "surfaceChanged: holder:" + holder + "format:" + format + ",width:" + width + ",height:" + height);
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                Log.i(TAG, "surfaceDestroyed: ");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(controller!=null){
            controller.destroy();
        }
    }
}