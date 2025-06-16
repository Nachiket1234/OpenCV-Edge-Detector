package com.nachiket.opencvedgedetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView;
import android.util.Log;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.egl.EGLConfig;

public class MyRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "MyRenderer";

    static {
        try {
            System.loadLibrary("opencvedgedetector");
            Log.d(TAG, "Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library", e);
        }
    }

    private final Context context;
    private int textureId = 0;

    // Processing mode constants
    public static final int MODE_ORIGINAL = 0;
    public static final int MODE_GRAYSCALE = 1;
    public static final int MODE_EDGE_DETECTION = 2;

    // Current processing mode
    private volatile int currentMode = MODE_ORIGINAL;

    // Texture dimensions for camera frames
    private int cameraTextureWidth = 0;
    private int cameraTextureHeight = 0;
    private volatile boolean hasValidTexture = false;
    private volatile boolean isInitialized = false;

    public MyRenderer(Context ctx) {
        this.context = ctx;
        Log.d(TAG, "MyRenderer created");
    }

    public void setProcessingMode(int mode) {
        Log.d(TAG, "Setting processing mode to: " + mode);
        this.currentMode = mode;
    }

    public void processFrame(byte[] frameData, int width, int height) {
        if (frameData != null && width > 0 && height > 0 && isInitialized) {
            Log.d(TAG, "Processing frame: " + width + "x" + height + ", data length: " + frameData.length);
            convertYuvToRgbAndUpdateTexture(frameData, width, height);
        }
    }

    private void convertYuvToRgbAndUpdateTexture(byte[] yuvData, int width, int height) {
        try {
            int ySize = width * height;
            int uvSize = ySize / 4;

            if (yuvData.length < ySize + uvSize * 2) {
                Log.e(TAG, "YUV data too small: " + yuvData.length + " < " + (ySize + uvSize * 2));
                return;
            }

            byte[] rgbData = new byte[width * height * 3];

            // YUV to RGB conversion
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    int yIndex = row * width + col;
                    int uvRow = row / 2;
                    int uvCol = col / 2;
                    int uIndex = ySize + uvRow * (width / 2) + uvCol;
                    int vIndex = ySize + uvSize + uvRow * (width / 2) + uvCol;

                    if (uIndex >= yuvData.length || vIndex >= yuvData.length) {
                        continue;
                    }

                    int y = (yuvData[yIndex] & 0xFF) - 16;
                    int u = (yuvData[uIndex] & 0xFF) - 128;
                    int v = (yuvData[vIndex] & 0xFF) - 128;

                    int r = (int) (1.164 * y + 1.596 * v);
                    int g = (int) (1.164 * y - 0.392 * u - 0.813 * v);
                    int b = (int) (1.164 * y + 2.017 * u);

                    r = Math.max(0, Math.min(255, r));
                    g = Math.max(0, Math.min(255, g));
                    b = Math.max(0, Math.min(255, b));

                    int rgbIndex = yIndex * 3;
                    if (rgbIndex + 2 < rgbData.length) {
                        rgbData[rgbIndex] = (byte) r;
                        rgbData[rgbIndex + 1] = (byte) g;
                        rgbData[rgbIndex + 2] = (byte) b;
                    }
                }
            }

            updateOpenGLTexture(rgbData, width, height);
        } catch (Exception e) {
            Log.e(TAG, "Error in YUV to RGB conversion", e);
        }
    }

    private void updateOpenGLTexture(byte[] rgbData, int width, int height) {
        try {
            if (textureId == 0) {
                int[] textures = new int[1];
                GLES30.glGenTextures(1, textures, 0);
                textureId = textures[0];
                Log.d(TAG, "Generated new texture ID: " + textureId);

                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

                nativeSetTexture(textureId, width, height);
            }

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);

            ByteBuffer buffer = ByteBuffer.allocateDirect(rgbData.length);
            buffer.put(rgbData);
            buffer.position(0);

            if (cameraTextureWidth == width && cameraTextureHeight == height && hasValidTexture) {
                GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, width, height,
                        GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE, buffer);
            } else {
                GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGB, width, height, 0,
                        GLES30.GL_RGB, GLES30.GL_UNSIGNED_BYTE, buffer);
                cameraTextureWidth = width;
                cameraTextureHeight = height;
                hasValidTexture = true;
                Log.d(TAG, "Camera texture allocated: " + width + "x" + height);
            }

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

            int error = GLES30.glGetError();
            if (error != GLES30.GL_NO_ERROR) {
                Log.e(TAG, "OpenGL error in updateTexture: " + error);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating OpenGL texture", e);
        }
    }

    private Bitmap createTestBitmap() {
        // Create a simple test pattern bitmap
        int width = 256;
        int height = 256;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Fill with white background
        canvas.drawColor(Color.WHITE);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        // Draw a black square
        paint.setColor(Color.BLACK);
        canvas.drawRect(64, 64, 192, 192, paint);

        // Draw a gray circle
        paint.setColor(Color.GRAY);
        canvas.drawCircle(128, 128, 48, paint);

        // Draw some lines for edge detection
        paint.setColor(Color.DKGRAY);
        paint.setStrokeWidth(4);
        canvas.drawLine(32, 32, 224, 32, paint);
        canvas.drawLine(32, 224, 224, 224, paint);
        canvas.drawLine(32, 32, 32, 224, paint);
        canvas.drawLine(224, 32, 224, 224, paint);

        Log.d(TAG, "Created test bitmap: " + width + "x" + height);
        return bitmap;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated");
        try {
            nativeInit();
            isInitialized = true;

            // Create and load a test bitmap
            Bitmap testBitmap = createTestBitmap();
            if (testBitmap != null) {
                textureId = loadTexture(testBitmap);
                if (textureId > 0) {
                    nativeSetTexture(textureId, testBitmap.getWidth(), testBitmap.getHeight());
                    Log.d(TAG, "Test texture loaded successfully: " + textureId);
                } else {
                    Log.e(TAG, "Failed to load test texture");
                }
                testBitmap.recycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onSurfaceCreated", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int w, int h) {
        Log.d(TAG, "onSurfaceChanged: " + w + "x" + h);
        try {
            nativeResize(w, h);
        } catch (Exception e) {
            Log.e(TAG, "Error in onSurfaceChanged", e);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        try {
            nativeDrawFrame(currentMode);
        } catch (Exception e) {
            Log.e(TAG, "Error in onDrawFrame", e);
            // Fallback rendering
            GLES30.glClearColor(0.5f, 0.0f, 0.0f, 1.0f);
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);
        }
    }

    private int loadTexture(Bitmap bitmap) {
        try {
            int[] textureIds = new int[1];
            GLES30.glGenTextures(1, textureIds, 0);
            int texId = textureIds[0];

            if (texId == 0) {
                Log.e(TAG, "Failed to generate texture");
                return 0;
            }

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texId);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);

            int error = GLES30.glGetError();
            if (error != GLES30.GL_NO_ERROR) {
                Log.e(TAG, "OpenGL error in loadTexture: " + error);
                return 0;
            }

            Log.d(TAG, "Texture loaded successfully: " + texId);
            return texId;
        } catch (Exception e) {
            Log.e(TAG, "Error loading texture", e);
            return 0;
        }
    }

    // JNI method declarations
    private native void nativeInit();
    private native void nativeResize(int w, int h);
    private native void nativeSetTexture(int texId, int width, int height);
    private native void nativeSetMode(int mode);
    private native void nativeProcess();
    private native void nativeDrawFrame(int mode);
}