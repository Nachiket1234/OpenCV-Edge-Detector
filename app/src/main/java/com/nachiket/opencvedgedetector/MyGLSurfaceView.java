// MyGLSurfaceView.java
package com.nachiket.opencvedgedetector;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class MyGLSurfaceView extends GLSurfaceView {
    // hold onto the renderer instance
    private MyRenderer renderer;

//    public MyGLSurfaceView(Context context) {
//        super(context);
//        init();
//    }

    public MyGLSurfaceView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        setEGLContextClientVersion(3);
        renderer = new MyRenderer(ctx);
        setRenderer(renderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public MyRenderer getRenderer() {
        return renderer;
    }
}
