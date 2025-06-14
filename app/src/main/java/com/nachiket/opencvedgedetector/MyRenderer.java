package com.nachiket.opencvedgedetector;

import android.graphics.Bitmap;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {
    static {
        System.loadLibrary("opencvedgedetector");
    }

    // --- Native (C++) Hooks ---
    // These are promises that you will implement these functions in C++.
    private native void nativeInit();
    private native void nativeResize(int width, int height);
    private native void nativeDrawFrame();
    private native void nativeProcess();

    // **FIX APPLIED**: Changed signature to also pass width and height.
    private native void nativeSetTexture(int textureId, int width, int height);

    // This flag signals the render thread to perform processing.
    private volatile boolean doProcess = false;

    // This method is called from MainActivity to trigger processing.
    public void requestProcessing() {
        doProcess = true;
    }

    // This method is called from MainActivity to upload the Bitmap to the GPU.
    public void loadTexture(Bitmap bmp) {
        // 1. Generate a new OpenGL texture ID
        int[] textureIds = new int[1];
        GLES30.glGenTextures(1, textureIds, 0);
        int textureId = textureIds[0];

        // 2. Bind the texture and upload the Bitmap data
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0); // Unbind

        // 3. **FIX APPLIED**: Call the new native method, passing the ID and dimensions.
        nativeSetTexture(textureId, bmp.getWidth(), bmp.getHeight());
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        nativeInit();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        nativeResize(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // **FIX APPLIED**: All drawing logic is removed from Java.
        // We now only delegate to the C++ code.

        // First, check if a processing request is pending
        if (doProcess) {
            nativeProcess();   // If so, call the native processing function
            doProcess = false; // Reset the flag
        }

        // Then, always call the native drawing function
        nativeDrawFrame();
    }
}
