package com.blueberry.videoplayer;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Environment;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * author: muyonggang
 * date: 2022/3/26
 */
public class ResourcesLoader {
    public static String MOJITO = "Mojito.mp3";
    public static String CAPTAIN_WOMEN = "captain_women.mp4";
    private static final String TAG = "ResourcesLoader";

    public String loadVideoAssetsResource(Activity activity) {
        File externalFilesDir = activity.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        File captainMp4= new File(externalFilesDir, CAPTAIN_WOMEN);
        if (captainMp4.exists()) {
            return captainMp4.getAbsolutePath();
        }
        loadAssetsFile(activity,CAPTAIN_WOMEN, captainMp4);
        return captainMp4.getAbsolutePath();
    }

    public String loadAudioAssetsResource(Activity activity) {
        File externalFilesDir = activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File mojitoMp3 = new File(externalFilesDir, MOJITO);
        if (mojitoMp3.exists()) {
            return mojitoMp3.getAbsolutePath();
        }
        loadAssetsFile(activity,MOJITO, mojitoMp3);
        return mojitoMp3.getAbsolutePath();
    }

    private void loadAssetsFile(Activity activity,String path, File mojitoMp3) {
        AssetManager assetManager = activity.getAssets();
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            inputStream = assetManager.open( path);
            fileOutputStream = new FileOutputStream(mojitoMp3);
            byte[] buffer = new byte[1024];
            int len = -1;
            while ((len = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeSilently(fileOutputStream);
            closeSilently(inputStream);
            assetManager.close();
        }
    }

    public void closeSilently(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            // ignored.
        }
    }
}
