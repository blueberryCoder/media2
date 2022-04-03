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
public class MediaPlayerAudioActivity extends Activity {

    private static final String TAG = "MediaPlayerAudio";

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
    private MediaPlayer mMediaPlayer;
    private ImageView mIvCover;
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
        setContentView(R.layout.activity_media_player_audio);

        mBtnStart = findViewById(R.id.btnStart);
        mBtnPause = findViewById(R.id.btnPause);
        mBtnStop = findViewById(R.id.btnStop);
        mSeekBar = findViewById(R.id.mSeekBar);
        mIvCover = findViewById(R.id.ivCover);

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
        String mp3File = mResourceLoader.loadAudioAssetsResource(this);
        if (mp3File != null && !mp3File.isEmpty()) {
            mMediaPlayer = new MediaPlayer();
            try {
                mMediaPlayer.setDataSource(mp3File);
                status = INITIALIZED;
                long start = System.currentTimeMillis();
                mMediaPlayer.prepare();
                long cost = System.currentTimeMillis() - start;
                Log.i(TAG, "mediaPlayer prepare cost: " + cost);
                setSeekBarDuration();
                // get cover
                setCover(mp3File);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setCover(String mp3File) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(mp3File);
        byte[] embeddedPicture = retriever.getEmbeddedPicture();
        if (embeddedPicture != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.length);
            mIvCover.setImageBitmap(bitmap);
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
