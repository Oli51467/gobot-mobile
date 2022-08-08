package com.irlab.view.jniprocessing;

import org.opencv.core.Mat;

public class JniImageProcessing {
    static {
        System.loadLibrary("native-lib");
    }
    //public native void boardProcessing(long srcRGBMatAddr, long dstGrayMatAddr);
}
