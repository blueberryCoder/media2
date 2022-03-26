package com.blueberry.glexampe;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("glejni");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnGl2SurfaceView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Intent  intent = new Intent(MainActivity.this,GL2SurfaceViewActivity.class) ;
               MainActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.btnGlSurfaceView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent  intent = new Intent(MainActivity.this,SurfaceViewActivity.class) ;
                MainActivity.this.startActivity(intent);
            }
        });

        findViewById(R.id.btnEGLSurfaceView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent  intent = new Intent(MainActivity.this,EGLSurfaceActivity.class) ;
                MainActivity.this.startActivity(intent);
            }
        });
    }

}