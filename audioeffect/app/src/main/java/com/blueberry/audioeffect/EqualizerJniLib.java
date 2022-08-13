package com.blueberry.audioeffect;

/**
 * author: muyonggang
 * date: 2022/7/10
 */
public class EqualizerJniLib {

    private long cPtr;

    public void initialize() {
        cPtr = init();
    }

    public void setInput(String input) {
        setInput(cPtr, input);
    }

    public void setOutput(String output) {
        setOutput(cPtr,output);
    }

    public void start() {
        start(cPtr);
    }

    private native long init();

    private native void setInput(long cptr, String input);

    private native void setOutput(long cptr, String output);

    private native void start(long cptr);
}
