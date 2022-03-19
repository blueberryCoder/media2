package com.blueberry.glexampe;

import android.content.Context;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

/**
 * author: muyonggang
 * date: 2022/3/13
 */
public class GL2JNIView extends GLSurfaceView {
    private static final String TAG = "GL2JNIView";

    public GL2JNIView(Context context) {
        this(context, null);
    }

    public GL2JNIView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        Log.d(TAG, "init: ");
//        setEGLConfigChooser(new ConfigChooser());
        setEGLContextFactory(new ContextFactory());
        setRenderer(new Render());
    }
    private static class ContextFactory implements GLSurfaceView.EGLContextFactory {
        private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
            Log.w(TAG, "creating OpenGL ES 2.0 context");
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
            EGLContext context = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
            return context;
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            egl.eglDestroyContext(display, context);
        }
    }

    public static class Render implements GLSurfaceView.Renderer {

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            Log.d(TAG, "onSurfaceCreated: ");
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            Log.d(TAG, "onSurfaceChanged: width=" + width + ",height=" + height);
            String version = gl.glGetString(GL10.GL_VERSION);

            Log.d(TAG, "version: "+ version);
            GL2JNILib.init(width, height);
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            Log.i(TAG, "onDrawFrame: ");
            GL2JNILib.step();
        }
    }
}
