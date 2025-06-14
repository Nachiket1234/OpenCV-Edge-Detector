package com.nachiket.opencvedgedetector;

import android.opengl.GLES30;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NativeRenderer implements GLSurfaceView.Renderer {
    static { System.loadLibrary("opencvedgedetector"); }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        nativeOnSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        nativeOnSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        nativeOnDrawFrame();
    }

    // JNI declarations
    private static native void nativeOnSurfaceCreated();
    private static native void nativeOnSurfaceChanged(int w, int h);
    private static native void nativeOnDrawFrame();
}

