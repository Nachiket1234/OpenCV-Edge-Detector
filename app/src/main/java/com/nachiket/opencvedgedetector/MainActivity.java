package com.nachiket.opencvedgedetector;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.nachiket.opencvedgedetector.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 1) Grab the renderer you installed in your GLSurfaceView
        MyRenderer renderer = binding.glSurface.getRenderer();
        // ^-- you should have added a `getRenderer()` method in your custom GLSurfaceView

        // 2) Only schedule processing when the button is clicked
        binding.btnConvert.setOnClickListener(v -> {
            binding.glSurface.queueEvent(() -> {
                renderer.setGrayEnabled(true);      // ← turn on gray
            });
            binding.glSurface.requestRender();
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        binding.glSurface.onPause();   // Inform GLSurfaceView to pause
    }

}