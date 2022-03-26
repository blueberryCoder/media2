package com.blueberry.glexampe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class EGLSurfaceActivity extends AppCompatActivity {

    private SurfaceView mSurfaceView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurfaceView = new SurfaceView(this);
        setContentView(mSurfaceView);
       mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
           @Override
           public void surfaceCreated(@NonNull SurfaceHolder holder) {
               EGLJNILib.init(holder.getSurface());
           }

           @Override
           public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

           }

           @Override
           public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

           }
       });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EGLJNILib.destroy();
    }
}