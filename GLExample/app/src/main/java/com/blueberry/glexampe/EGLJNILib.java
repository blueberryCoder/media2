package com.blueberry.glexampe;

import android.view.Surface;

/**
 * author: muyonggang
 * date: 2022/3/20
 */
public class EGLJNILib {

    public static native boolean init(Surface surface);
    public static native boolean destroy();
}
