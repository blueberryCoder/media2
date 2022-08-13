package com.blueberry.audioeffect;

/**
 * author: muyonggang
 * date: 2022/7/10
 */
public class AudioEffectLib {

    private static String LIB_NAME = "audioeffect";

    public static void loadLibrary() {
        System.loadLibrary(LIB_NAME);
    }
}
