package com.blueberry.glexampe;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.ExecutionException;

public class SurfaceViewActivity extends AppCompatActivity {

    private static final String TAG = "SurfaceViewActivity";
    private SurfaceView mSurfaceView ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSurfaceView = new SurfaceView(this);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                // init
                Log.i(TAG, "surfaceCreated: ");
//                RenderThread thread = new RenderThread();
//                thread.start();
                SurfaceJNILib.init(holder.getSurface());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

            }
        });
        setContentView(mSurfaceView);
    }

    public class RenderThread extends Thread {

        private Paint mPaint = new Paint();
        @Override
        public void run() {
            while(true) {
                SurfaceHolder holder = mSurfaceView.getHolder();
                Canvas canvas = holder.lockCanvas();
                canvas.drawColor(Color.WHITE);
                canvas.drawCircle(0, 0, 200, mPaint);
                holder.unlockCanvasAndPost(canvas);
                try {
                    Log.i(TAG, "post canvas : ");
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }
        }
    }
}