package com.nachiket.opencvedgedetector;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.nachiket.opencvedgedetector.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private MyRenderer renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1) Grab the renderer
        MyRenderer renderer = binding.glSurface.getRenderer();

        // 2) Load your test image into the texture ONCE
        Bitmap bmp = BitmapFactory.decodeResource(
                getResources(),
                R.drawable.edge_test
        ).copy(Bitmap.Config.ARGB_8888, true);

        // Queue texture upload on the GL thread
        binding.glSurface.post(() -> {
            binding.glSurface.queueEvent(() -> renderer.loadTexture(bmp));
            binding.glSurface.requestRender();
        });


        // 3) Button now toggles GPU‐shader gray mode
        binding.btnConvert.setOnClickListener(v -> {
            binding.glSurface.queueEvent(renderer::requestProcessing);
            binding.glSurface.requestRender();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.glSurface.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.glSurface.onPause();
    }


}

