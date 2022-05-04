package com.blueberry.videoplayer;

import android.content.res.AssetManager;
import android.view.Surface;
import android.view.SurfaceHolder;

/**
 * author: muyonggang
 * date: 2022/4/30
 */
public class MediaPlayerController {
    static {
        System.loadLibrary("mediaplayer");
    }

    private long cPtr = 0;

    public void init(Surface surface, AssetManager assetManager, String path) {
        cPtr = initialize(surface, assetManager, path);
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

    public void destroy() {
        destroy(cPtr);
        cPtr = 0;
    }

    private native long initialize(Surface surface, AssetManager assetManager, String path);

    private native void start(long cPtr);

    private native void pause(long cPtr);

    private native void stop(long cPtr);

    private native void destroy(long cPtr);

}
