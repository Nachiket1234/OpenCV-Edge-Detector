package com.nachiket.opencvedgedetector;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

public class MyGLSurfaceView extends GLSurfaceView {
    private static final String TAG = "MyGLSurfaceView";

    private MyRenderer renderer;

    public MyGLSurfaceView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        Log.d(TAG, "Creating MyGLSurfaceView");

        try {
            setEGLContextClientVersion(3);
            renderer = new MyRenderer(ctx);
            setRenderer(renderer);
            setRenderMode(RENDERMODE_CONTINUOUSLY); // Changed to continuous for better debugging
            Log.d(TAG, "MyGLSurfaceView setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up GLSurfaceView", e);
        }
    }

    public MyRenderer getRenderer() {
        return renderer;
    }
}