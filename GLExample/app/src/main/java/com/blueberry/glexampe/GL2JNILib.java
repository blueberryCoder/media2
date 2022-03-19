package com.blueberry.glexampe;

/**
 * author: muyonggang
 * date: 2022/3/13
 */
public class GL2JNILib {

    public static native void init(int width,int height);
    public static native void step();
}
