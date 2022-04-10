package com.blueberry.videoplayer;

import android.graphics.Bitmap;

/**
 * author: muyonggang
 * date: 2022/4/9
 */
public class SLPlayMp3JniLib {
    static {
        System.loadLibrary("videoplayer");
    }

    private long cPtr = 0;

    public void init(String filePath) {
        cPtr = initialize(filePath);
    }

    public int start() {
        return start(cPtr);
    }

    public int pause() {
        return pause(cPtr);
    }

    public int stop() {
        return stop(cPtr);
    }

    public int destroy() {
        return destroy(cPtr);
    }

    public long getDuration() {
        return getDuration(cPtr);
    }

    public int seek(long position) {
        return seek(cPtr, position);
    }

    public long getPosition() {
        return getPosition(cPtr);
    }


    /**
     * create opensl engine and load mp3 file as source and setup output.
     *
     * @param filePath mp3 file path
     * @return cPtr
     */
    private native long initialize(String filePath);

    private native int start(long cPtr);

    private native int pause(long cPtr);

    private native int stop(long cPtr);

    private native int destroy(long cPtr);

    private native long getDuration(long cPtr);

    private native long getPosition(long cPtr);

    private native int seek(long cPtr, long position);
}
