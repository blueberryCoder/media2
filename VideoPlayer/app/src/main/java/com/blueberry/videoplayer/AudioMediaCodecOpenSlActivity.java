package com.blueberry.videoplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaMetadataRetriever;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * author: muyonggang
 * date: 2022/3/26
 */
public class AudioMediaCodecOpenSlActivity extends Activity {

    private static final String TAG = "MediaPlayerAudio";

    public static int UNINITIALIZED = 0;
    public static int INITIALIZED = 1;
    public static int STARTED = 2;
    public static int PAUSED = 3;
    public static int STOPPED = 4;
    public static int INTERVAL_UPDATE_PROGRESS = 1;

    private ResourcesLoader mResourceLoader = new ResourcesLoader();
    private int PERMISSION_REQUEST_CODE = 3000;
    private Button mBtnStart;
    private Button mBtnPause;
    private Button mBtnStop;
    private SeekBar mSeekBar;
    private final Object lock = new Object();
    private AudioTrack mAudioTrack;
    private ImageView mIvCover;
    private  volatile  int status = UNINITIALIZED;
    private WriteThread mWriteThread;
    private String mp3FilePath ="";
    private SLMediaCodecAudio slMediaCodecAudio = null;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == INTERVAL_UPDATE_PROGRESS) {
                Message newMsg = Message.obtain(this, INTERVAL_UPDATE_PROGRESS);
                mHandler.sendMessageDelayed(newMsg, 1000);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_mediacodec_opensl);

        mBtnStart = findViewById(R.id.btnStart);
        mBtnPause = findViewById(R.id.btnPause);
        mBtnStop = findViewById(R.id.btnStop);
        mSeekBar = findViewById(R.id.mSeekBar);
        mIvCover = findViewById(R.id.ivCover);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_MEDIA_LOCATION)
                == PackageManager.PERMISSION_GRANTED
        ) {
            initializeMediaPlayer();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_MEDIA_LOCATION
                    },
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

    private void initializeAudioTrack() {
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        AudioAttributes audioAttributes = new AudioAttributes.Builder().build();
        AudioFormat audioFormat = new AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_MP3)
                .setSampleRate(44100)
                .setChannelMask(AudioFormat.CHANNEL_IN_LEFT | AudioFormat.CHANNEL_IN_RIGHT)
                .build();

        int bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_MP3) * 4;

        mAudioTrack = new AudioTrack(audioAttributes, audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM,
                audioManager.generateAudioSessionId()
        );
    }

    private void start() {
        if (status == UNINITIALIZED || status == STARTED || status == PAUSED) {
            return;
        }
        slMediaCodecAudio.start();
        status = STARTED;
    }

    private void pause() {
        if (status == UNINITIALIZED || status == STOPPED) {
            return;
        }
        slMediaCodecAudio.pause();
        status = PAUSED;
    }

    private void stop() {
        if (status == STARTED || status == PAUSED) {
            slMediaCodecAudio.stop();
            status = STOPPED;
        }
    }

    private void initializeMediaPlayer() {
        // loading resources
        this.mp3FilePath = mResourceLoader.loadAudioAssetsResource(this);
        status = INITIALIZED;
        setCover(mp3FilePath);

        slMediaCodecAudio = new SLMediaCodecAudio();
        slMediaCodecAudio.init(this.getAssets(),"Mojito.mp3");
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
        ) {
            initializeMediaPlayer();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAudioTrack.release();
        if(mWriteThread!=null && mWriteThread.isAlive()){
            mWriteThread.interrupt();
        }
    }


    class WriteThread extends Thread {
        private File mFile = null;

        WriteThread(File file) {
            this.mFile = file;
        }

        @Override
        public void run() {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(mFile);
                int len = -1;
                byte[] buffer = new byte[4096];
                while (!this.isInterrupted()) {
                    while (status == STARTED && (len = fis.read(buffer)) != -1) {
                        mAudioTrack.write(buffer, 0, len);
                        if (status != STARTED) {
                            break;
                        }
                    }
                    synchronized (lock) {
                        while (status != STARTED) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }
}
