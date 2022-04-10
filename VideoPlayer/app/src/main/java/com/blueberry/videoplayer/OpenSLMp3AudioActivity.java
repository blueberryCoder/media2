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
public class OpenSLMp3AudioActivity extends Activity {

    private static final String TAG = "OpenSLMp3AudioActivity";

    public static int UNINITIALIZED = 0;
    public static int INITIALIZED = 1;
    public static int STARTED = 2;
    public static int PAUSED = 3;
    public static int STOPED = 4;

    private ResourcesLoader mResourceLoader = new ResourcesLoader();
    private int PERMISSION_REQUEST_CODE = 3000;
    private Button mBtnStart;
    private Button mBtnPause;
    private Button mBtnStop;
    private SeekBar mSeekBar;
    private SLPlayMp3JniLib mMp3SEngine;
    private ImageView mIvCover;
    private int status = UNINITIALIZED;
    private long duration = 0L;
    public static int INTERVAL_UPDATE_PROGRESS = 1;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == INTERVAL_UPDATE_PROGRESS) {
                duration = mMp3SEngine.getDuration();
                Log.i(TAG, "in handler duration: "+ duration);
                if (mMp3SEngine!= null && status == STARTED && duration != 0) {
                    int position = (int) ((double)mMp3SEngine.getPosition() / duration * mSeekBar.getMax());
                    mSeekBar.setProgress(position);
                }

                if(!OpenSLMp3AudioActivity.this.isDestroyed()) {
                    Message newMsg = Message.obtain(this, INTERVAL_UPDATE_PROGRESS);
                    mHandler.sendMessageDelayed(newMsg, 1000);
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opensl_play_mp3_audio);

        mBtnStart = findViewById(R.id.btnStart);
        mBtnPause = findViewById(R.id.btnPause);
        mBtnStop = findViewById(R.id.btnStop);
        mSeekBar = findViewById(R.id.mSeekBar);
        mIvCover = findViewById(R.id.ivCover);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            initializeOpenSLEngine();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser && status == STARTED && mSeekBar.getMax() != 0){
                    float percent = (float) progress / mSeekBar.getMax();
                    mMp3SEngine.seek((long) (percent * duration));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start
                start();
            }
        });

        mBtnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stop();
            }
        });

        mBtnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pause();
            }
        });

        Message message = Message.obtain(mHandler, INTERVAL_UPDATE_PROGRESS);
        mHandler.sendMessage(message);
    }

    private void start() {
        if (status == UNINITIALIZED || status == STARTED || status == PAUSED) {
            return;
        }
        duration = mMp3SEngine.getDuration();
        Log.i(TAG, "before start duration: "+duration);
        mMp3SEngine.start();
        duration = mMp3SEngine.getDuration();
        Log.i(TAG, "after start duration: "+ duration);
        status = STARTED;
    }

    private void pause() {
        if (status == UNINITIALIZED || status == STOPED) {
            return;
        }
        if (status == PAUSED) {
            mMp3SEngine.start();
            status = PAUSED;
        } else if (status == STARTED) {
            mMp3SEngine.pause();
            status = STARTED;
        }
    }

    private void stop() {
        if (status == STARTED || status == PAUSED) {
            mMp3SEngine.stop();
            status = STOPED;
            mSeekBar.setProgress(0);
        }
    }

    private void initializeOpenSLEngine() {
        // loading resources
        String mp3File = mResourceLoader.loadAudioAssetsResource(this);
        if (mp3File != null && !mp3File.isEmpty()) {
            // init opensl es
            mMp3SEngine = new SLPlayMp3JniLib();
            mMp3SEngine.init(mp3File);
            status = INITIALIZED;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeOpenSLEngine();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMp3SEngine.destroy();
    }
}
