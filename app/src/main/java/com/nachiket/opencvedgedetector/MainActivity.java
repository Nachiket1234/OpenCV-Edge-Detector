package com.nachiket.opencvedgedetector;

import android.Manifest;
import androidx.annotation.NonNull;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.nachiket.opencvedgedetector.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private ActivityMainBinding binding;
    private CameraManager cameraManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Check camera permission
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
                Log.d(TAG, "Grayscale button clicked");
                MyRenderer renderer = binding.glSurface.getRenderer();
                if (renderer != null) {
                    binding.glSurface.queueEvent(() -> renderer.setProcessingMode(MyRenderer.MODE_GRAYSCALE));
                    binding.glSurface.requestRender();
                }
            });

            binding.btnEdgeDetection.setOnClickListener(v -> {
                Log.d(TAG, "Edge detection button clicked");
                MyRenderer renderer = binding.glSurface.getRenderer();
                if (renderer != null) {
                    binding.glSurface.queueEvent(() -> renderer.setProcessingMode(MyRenderer.MODE_EDGE_DETECTION));
                    binding.glSurface.requestRender();
                }
            });

            binding.btnOriginal.setOnClickListener(v -> {
                Log.d(TAG, "Original button clicked");
                MyRenderer renderer = binding.glSurface.getRenderer();
                if (renderer != null) {
                    binding.glSurface.queueEvent(() -> renderer.setProcessingMode(MyRenderer.MODE_ORIGINAL));
                    binding.glSurface.requestRender();
                }
            });

            Log.d(TAG, "onCreate completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error initializing app: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void initializeCamera() {
        try {
            Log.d(TAG, "Initializing camera");
            cameraManager = new CameraManager(this);
            MyRenderer renderer = binding.glSurface.getRenderer();
            if (renderer != null) {
                cameraManager.setFrameCallback(renderer::processFrame);
                cameraManager.startCamera();
                Log.d(TAG, "Camera initialized successfully");
            } else {
                Log.e(TAG, "Renderer is null, cannot initialize camera");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing camera", e);
            Toast.makeText(this, "Error initializing camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed
                Log.d(TAG, "Camera permission granted");
                initializeCamera();
            } else {
                // Permission denied, show a message
                Log.w(TAG, "Camera permission denied");
                Toast.makeText(this, "Camera permission is required for this app to work", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        try {
            if (binding != null && binding.glSurface != null) {
                binding.glSurface.onResume();
            }
            if (cameraManager != null) {
                cameraManager.startCamera();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        try {
            if (binding != null && binding.glSurface != null) {
                binding.glSurface.onPause();
            }
            if (cameraManager != null) {
                cameraManager.stopCamera();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        try {
            if (cameraManager != null) {
                cameraManager.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }
}