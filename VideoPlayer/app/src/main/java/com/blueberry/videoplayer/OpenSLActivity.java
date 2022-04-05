package com.blueberry.videoplayer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

/**
 * author: muyonggang
 * date: 2022/3/26
 */
public class OpenSLActivity extends Activity {

    private static final String TAG = "OpenSLActivity";

    private static final int AUDIO_ECHO_REQUEST = 0;
    private Button mBtnStart;
    private SeekBar mDelaySeekBar;
    private SeekBar mDecaySeekBar;
    private Boolean isPlaying = false;
    private float echoDecayProgress;
    private int echoDelayProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opensl);
        mBtnStart = findViewById(R.id.btnStart);
        mDecaySeekBar = findViewById(R.id.decaySeekBar);
        mDelaySeekBar = findViewById(R.id.delaySeekBar);

        echoDelayProgress = mDelaySeekBar.getProgress() * 1000 / mDelaySeekBar.getMax();
        mDelaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                echoDelayProgress = progress * 1000 / mDelaySeekBar.getMax();
                Log.i(TAG, "onProgressChanged: delayProgress:" + echoDelayProgress + ",decayProgress:" + echoDecayProgress);
                SLJniLib.configureEcho(echoDelayProgress, echoDecayProgress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        echoDecayProgress = (float) mDecaySeekBar.getProgress()/mDecaySeekBar.getMax();
        mDecaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(!fromUser){
                    return;
                }
                echoDecayProgress = (float) seekBar.getProgress()/seekBar.getMax();
                SLJniLib.configureEcho(echoDelayProgress,echoDecayProgress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_ECHO_REQUEST);
        } else {
            createSLEngine();
        }

        mBtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startEcho();
            }
        });

    }

    private void createSLEngine() {
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        String nativeSampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
        String nativeFramesPerBuf = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);

        int recBufSize = AudioRecord.getMinBufferSize(Integer.parseInt(nativeSampleRate)
                , AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (recBufSize == AudioRecord.ERROR || recBufSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "onCreate: doesn't support audio.");
        }
        Log.i(TAG, "SampleRate:" + nativeSampleRate +
                ",nativeSampleBufSize:" + nativeFramesPerBuf +
                ",echoDelayProgress:"+ echoDelayProgress+
                ",echoDecayProgress:"+echoDecayProgress
        );
        // 48000
        // 96
        // 100,
        // 0.1
        SLJniLib.createSLEngine(
                // sample rate
                Integer.parseInt(nativeSampleRate),
                Integer.parseInt(nativeFramesPerBuf),
                echoDelayProgress,
                echoDecayProgress);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SLJniLib.deleteSLEngine();
    }

    private void startEcho() {
        if (!isPlaying) {
            if (!SLJniLib.createSLBufferQueueAudioPlayer()) {
                Log.e(TAG, "startEcho: Failed to create audio player.");
                return;
            }
            if (!SLJniLib.createAudioRecorder()) {
                SLJniLib.deleteSLBufferQueueAudioPlayer();
                Log.e(TAG, "startEcho: Failed to create audio recorder.");
                return;
            }
            SLJniLib.startPlay();
        } else {
            SLJniLib.stopPlay();
            SLJniLib.deleteAudioRecorder();
            SLJniLib.deleteSLBufferQueueAudioPlayer();
        }
        isPlaying = !isPlaying;

        if (isPlaying) {
            mBtnStart.setText("stop");
        } else {
            mBtnStart.setText("Start");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_ECHO_REQUEST && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createSLEngine();
        }
    }
}
