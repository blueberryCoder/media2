package com.blueberry.glexampe;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("glejni");
    }

    private GL2JNIView mGL2JNIView = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGL2JNIView = new GL2JNIView(this);
        setContentView(mGL2JNIView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGL2JNIView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGL2JNIView.onPause();
    }
}