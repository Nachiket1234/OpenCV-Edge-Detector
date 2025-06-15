package com.nachiket.opencvedgedetector;

import android.Manifest;
import androidx.annotation.NonNull;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.nachiket.opencvedgedetector.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private ActivityMainBinding binding;
    private CameraManager cameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check camera permission
        // Check if permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            // Permission already granted, proceed with camera functionality
            initializeCamera();
        }

        // Set up button listeners
        binding.btnGrayscale.setOnClickListener(v -> {
            MyRenderer renderer = binding.glSurface.getRenderer();
            if (renderer != null) {
                binding.glSurface.queueEvent(() -> renderer.setProcessingMode(MyRenderer.MODE_GRAYSCALE));
                binding.glSurface.requestRender();
            }
        });

        binding.btnEdgeDetection.setOnClickListener(v -> {
            MyRenderer renderer = binding.glSurface.getRenderer();
            if (renderer != null) {
                binding.glSurface.queueEvent(() -> renderer.setProcessingMode(MyRenderer.MODE_EDGE_DETECTION));
                binding.glSurface.requestRender();
            }
        });

        binding.btnOriginal.setOnClickListener(v -> {
            MyRenderer renderer = binding.glSurface.getRenderer();
            if (renderer != null) {
                binding.glSurface.queueEvent(() -> renderer.setProcessingMode(MyRenderer.MODE_ORIGINAL));
                binding.glSurface.requestRender();
            }
        });
    }

    private void initializeCamera() {
        cameraManager = new CameraManager(this);
        MyRenderer renderer = binding.glSurface.getRenderer();
        if (renderer != null) {
            cameraManager.setFrameCallback(renderer::processFrame);
        }
        cameraManager.startCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed
                initializeCamera();
            } else {
                // Permission denied, show a message or close the app
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        binding.glSurface.onResume();
        if (cameraManager != null) {
            cameraManager.startCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        binding.glSurface.onPause();
        if (cameraManager != null) {
            cameraManager.stopCamera();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraManager != null) {
            cameraManager.release();
        }
    }
}