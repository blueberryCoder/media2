package com.blueberry.videoplayer;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.IOException;

/**
 * author: muyonggang
 * date: 2022/3/26
 */
public class MediaPlayerVideoActivity extends Activity {

    private static final String TAG = "MediaPlayerVideo";

    public static int UNINITIALIZED = 0;
    public static int DATA_LOADED = 1;
    public static int INITIALIZED = 2;
    public static int STARTED = 3;
    public static int PAUSED = 4;
    public static int STOPED = 5;

    private ResourcesLoader mResourceLoader = new ResourcesLoader();
    private int PERMISSION_REQUEST_CODE = 3000;
    private Button mBtnStart;
    private Button mBtnPause;
    private Button mBtnStop;
    private SeekBar mSeekBar;
    private MediaPlayer mMediaPlayer;
    private SurfaceView mSurfaceView;
    private int status = UNINITIALIZED;

    public static int INTERVAL_UPDATE_PROGRESS = 1;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == INTERVAL_UPDATE_PROGRESS) {
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
                }
                Message newMsg = Message.obtain(this, INTERVAL_UPDATE_PROGRESS);
                mHandler.sendMessageDelayed(newMsg, 1000);
            }
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player_video);

        mBtnStart = findViewById(R.id.btnStart);
        mBtnPause = findViewById(R.id.btnPause);
        mBtnStop = findViewById(R.id.btnStop);
        mSeekBar = findViewById(R.id.mSeekBar);
        mSurfaceView = findViewById(R.id.surfaceView);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            initializeMediaPlayer();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start
                start();
            }
        });
        mBtnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause();
            }
        });
        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();

            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.i(TAG, "onProgressChanged: ");
                if (fromUser && status == STARTED) {
                    mMediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.i(TAG, "onStartTrackingTouch: ");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.i(TAG, "onStopTrackingTouch: ");
            }
        });

        Message message = Message.obtain(mHandler, INTERVAL_UPDATE_PROGRESS);
        mHandler.sendMessage(message);

        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                if(status == DATA_LOADED) {
                    Surface surface = holder.getSurface();
                    if(surface.isValid()){
                        mMediaPlayer.setSurface((surface));
                        long start = System.currentTimeMillis();
                        try {
                            mMediaPlayer.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        long cost = System.currentTimeMillis() - start;
                        Log.i(TAG, "mediaPlayer prepare cost: " + cost);
                        setSeekBarDuration();
                        status = INITIALIZED;
                    }
                }
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });

        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                status = STOPED;
                mSeekBar.setProgress(0);
            }
        });
    }

    private void start() {
        if (status == UNINITIALIZED || status == STARTED || status == PAUSED) {
            return;
        }
        try {
            if(status == STOPED){
                mMediaPlayer.prepare();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        mMediaPlayer.start();
        status = STARTED;
    }

    private void pause() {
        if (status == UNINITIALIZED || status == STOPED) {
            return;
        }
        if (status == STARTED) {
            mMediaPlayer.pause();
            status = PAUSED;
        } else {
            mMediaPlayer.start();
            status = STARTED;
        }
    }

    private void stop() {
        if (status == STARTED || status == PAUSED) {
            mMediaPlayer.stop();
            status = STOPED;
            mSeekBar.setProgress(0);
        }
    }

    private void initializeMediaPlayer() {
        // loading resources
        String mp4File = mResourceLoader.loadVideoAssetsResource(this);
        if (mp4File != null && !mp4File.isEmpty()) {
            mMediaPlayer = new MediaPlayer();
            try {
                mMediaPlayer.setDataSource(mp4File);
                status = DATA_LOADED;

                // get cover
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void setSeekBarDuration() {
        int duration = mMediaPlayer.getDuration();
        mMediaPlayer.getCurrentPosition();
        mSeekBar.setMax(duration);
        mSeekBar.setProgress(mMediaPlayer.getCurrentPosition());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeMediaPlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaPlayer.stop();
    }
}
