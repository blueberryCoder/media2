package com.blueberry.videoplayer;

/**
 * author: muyonggang
 * date: 2022/4/3
 */
public class SLJniLib {
    static  {
        System.loadLibrary("videoplayer");
    }
    public static native void createSLEngine(int rate,int framesPerBuf,long delayInMs,float decay);
    public static native void deleteSLEngine();

    public static native boolean configureEcho(int delayInMs,float decay);

    public static native boolean createSLBufferQueueAudioPlayer();
    public static native void deleteSLBufferQueueAudioPlayer();

    public static native boolean createAudioRecorder();
    public static native void deleteAudioRecorder();
    public static native void startPlay();
    public static native void stopPlay();

}
