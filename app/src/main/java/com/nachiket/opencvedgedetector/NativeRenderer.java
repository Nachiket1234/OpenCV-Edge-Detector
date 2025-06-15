package com.nachiket.opencvedgedetector;

public class NativeRenderer {
    static { System.loadLibrary("opencvedgedetector"); }

    public static native void nativeInit();
    public static native void nativeResize(int w, int h);
    public static native void nativeSetTexture(int tex, int w, int h);
    public static native void nativeProcess();
    public static native void nativeDrawFrame();
}
