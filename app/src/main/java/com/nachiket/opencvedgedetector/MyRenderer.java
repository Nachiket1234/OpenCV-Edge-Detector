package com.nachiket.opencvedgedetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

public class MyRenderer implements GLSurfaceView.Renderer {
    static { System.loadLibrary("opencvedgedetector"); }

    private final Context context;
    private int texId = 0;
    // Java equivalents of your native‐side GL state:
    private int mProgram         = 0;
    private int uTextureLoc      = -1;
    private int uGrayLoc         = -1;
    private int textureId        = 0;
    private int vao              = 0;
    private int vbo              = 0;
    private volatile boolean doProcess = false;

    private boolean doGray = false;

    public void setGrayEnabled(boolean gray) {
        doGray = gray;
    }

    public MyRenderer(Context ctx) {
        this.context = ctx;
    }

    // Called by MainActivity
    public void requestProcessing() {
        doProcess = true;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        nativeInit();
        // Load the test image into a texture
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), R.drawable.edge_test);
        nativeSetTexture(loadTexture(bmp), bmp.getWidth(), bmp.getHeight());
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        nativeResize(w, h);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 1) clear
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        // 2) use program & bind texture
        GLES30.glUseProgram(mProgram);
        GLES30.glUniform1i(uTextureLoc, 0);
        GLES30.glUniform1i(uGrayLoc,  doGray ? 1 : 0);       // ← pass the flag
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        // 3) draw
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4);
        GLES30.glBindVertexArray(0);
    }

    private int loadTexture(Bitmap bmp) {
        int[] ids = new int[1];
        GLES30.glGenTextures(1, ids, 0);
        texId = ids[0];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        return texId;
    }

    // JNI hooks
    private native void nativeInit();
    private native void nativeResize(int w, int h);
    private native void nativeSetTexture(int texId, int width, int height);
    private native void nativeProcess();
    private native void nativeDrawFrame();
}
