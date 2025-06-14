package com.nachiket.opencvedgedetector;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class MyGLSurfaceView extends GLSurfaceView {
    private final MyRenderer renderer;

    public MyGLSurfaceView(Context context) {
        super(context);
        renderer = new MyRenderer();
        init();
    }

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        renderer = new MyRenderer();
        init();
    }

    private void init() {
        setEGLContextClientVersion(3);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    /**
     * Expose the renderer to the Activity for queuing events and processing.
     */
    public MyRenderer getRenderer() {
        return renderer;
    }
}
