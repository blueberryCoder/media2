package com.blueberry.videoplayer;


import android.content.res.AssetManager;

/**
 * author: muyonggang
 * date: 2022/4/10
 */
public class SLMediaCodecAudio {

    private long cPtr;

    static {
        System.loadLibrary("videoplayer");
    }

    public void init(AssetManager assetManager, String path) {
        cPtr = initialize(assetManager, path);
    }

    public void start() {
        start(cPtr);
    }

    public void pause() {
        pause(cPtr);
    }

    public void stop() {
        stop(cPtr);
    }

    private native long initialize(AssetManager assetManager, String path);

    private native void start(long cPtr);

    private native void pause(long cPtr);

    private native void stop(long cPtr);

}
