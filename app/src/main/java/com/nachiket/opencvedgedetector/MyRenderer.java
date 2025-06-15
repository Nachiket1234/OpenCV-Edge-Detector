package com.nachiket.opencvedgedetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
    // Processing mode constants
    public static final int MODE_ORIGINAL = 0;
    public static final int MODE_GRAYSCALE = 1;
    public static final int MODE_EDGE_DETECTION = 2;

    // Current processing mode
    private int currentMode = MODE_ORIGINAL;

    // Texture update flag for thread synchronization
    // Texture dimensions for camera frames
    private int cameraTextureWidth = 0;
    private int cameraTextureHeight = 0;
    private volatile boolean textureNeedsUpdate = false;
    private byte[] pendingRgbData = null;
    private int pendingWidth = 0;
    private int pendingHeight = 0;


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

    // Add this method to handle mode switching
    public void setProcessingMode(int mode) {
        this.currentMode = mode;
        // The actual processing will be handled in the fragment shader
    }
    // Add this method to handle camera frame processing
    public void processFrame(byte[] frameData, int width, int height) {
        if (frameData != null && width > 0 && height > 0) {
            // Convert YUV_420_888 to RGB and update OpenGL texture
            // This is where real-time camera processing will happen
            convertYuvToRgbAndUpdateTexture(frameData, width, height);
        }
    }
    // Helper method for YUV to RGB conversion


    // Helper method for YUV to RGB conversion
    private void convertYuvToRgbAndUpdateTexture(byte[] yuvData, int width, int height) {
        // YUV_420_888 format has Y plane followed by U and V planes
        // Y plane: width * height bytes
        // U plane: (width/2) * (height/2) bytes
        // V plane: (width/2) * (height/2) bytes

        int ySize = width * height;
        int uvSize = ySize / 4; // U and V are subsampled by 2 in both dimensions

        byte[] rgbData = new byte[width * height * 3];

        // Simple YUV to RGB conversion using standard coefficients
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int yIndex = row * width + col;
                int uvRow = row / 2;
                int uvCol = col / 2;
                int uIndex = ySize + uvRow * (width / 2) + uvCol;
                int vIndex = ySize + uvSize + uvRow * (width / 2) + uvCol;

                // Extract YUV values
                int y = (yuvData[yIndex] & 0xFF) - 16;
                int u = (yuvData[uIndex] & 0xFF) - 128;
                int v = (yuvData[vIndex] & 0xFF) - 128;

                // YUV to RGB conversion using ITU-R BT.601 coefficients
                int r = (int) (1.164 * y + 1.596 * v);
                int g = (int) (1.164 * y - 0.392 * u - 0.813 * v);
                int b = (int) (1.164 * y + 2.017 * u);

                // Clamp values to 0-255 range
                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                // Store RGB values
                int rgbIndex = yIndex * 3;
                rgbData[rgbIndex] = (byte) r;
                rgbData[rgbIndex + 1] = (byte) g;
                rgbData[rgbIndex + 2] = (byte) b;
            }
        }

        // Update OpenGL texture with RGB data
        updateOpenGLTexture(rgbData, width, height);
    }

    private void updateOpenGLTexture(byte[] rgbData, int width, int height) {
        // Update the OpenGL texture with new frame data
        // This will trigger a re-render with the new camera frame

        if (textureId == 0) {
            // Generate texture if it doesn't exist
            int[] textures = new int[1];
            GLES30.glGenTextures(1, textures, 0);
            textureId = textures[0];

            // Set up texture parameters
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        }

        // Bind the texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);

        // Convert byte array to ByteBuffer for OpenGL
        ByteBuffer buffer = ByteBuffer.allocateDirect(rgbData.length);
        buffer.put(rgbData);
        buffer.position(0);

        // Update texture data - use glTexSubImage2D for better performance when updating existing texture
        if (cameraTextureWidth == width && cameraTextureHeight == height) {
            // Same dimensions, use faster glTexSubImage2D
            GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, width, height,
                    GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE, buffer);
        } else {
            // Different dimensions, need to reallocate texture
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGB, width, height, 0,
                    GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE, buffer);
            cameraTextureWidth = width;
            cameraTextureHeight = height;
        }

        // Unbind texture
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
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
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glUseProgram(mProgram);
        GLES30.glUniform1i(uTextureLoc, 0);

        // Use currentMode instead of doGray
        GLES30.glUniform1i(uGrayLoc, currentMode);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
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
