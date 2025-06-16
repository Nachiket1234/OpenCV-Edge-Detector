package com.nachiket.opencvedgedetector;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraManager {
    private static final String TAG = "CameraManager";
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private static Size chooseOptimalSize(Size[] choices) {
        // Pick the largest available size under MAX_PREVIEW_WIDTH x MAX_PREVIEW_HEIGHT
        Size optimalSize = choices[0];
        for (Size option : choices) {
            if (option.getWidth() <= MAX_PREVIEW_WIDTH && option.getHeight() <= MAX_PREVIEW_HEIGHT) {
                if (option.getWidth() * option.getHeight() > optimalSize.getWidth() * optimalSize.getHeight()) {
                    optimalSize = option;
                }
            }
        }
        Log.d(TAG, "Chosen optimal size: " + optimalSize.getWidth() + "x" + optimalSize.getHeight());
        return optimalSize;
    }

    private final ImageReader.OnImageAvailableListener onImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = null;
                    try {
                        image = reader.acquireNextImage();
                        if (frameCallback != null && image != null) {
                            // Extract YUV data from all three planes
                            byte[] yuvData = extractYuvData(image);
                            if (yuvData != null) {
                                frameCallback.onFrameAvailable(yuvData, image.getWidth(), image.getHeight());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing image", e);
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }

                private byte[] extractYuvData(Image image) {
                    try {
                        // Extract data from Y, U, V planes
                        Image.Plane[] planes = image.getPlanes();
                        if (planes.length < 3) {
                            Log.e(TAG, "Invalid number of planes: " + planes.length);
                            return null;
                        }

                        ByteBuffer yBuffer = planes[0].getBuffer();
                        ByteBuffer uBuffer = planes[1].getBuffer();
                        ByteBuffer vBuffer = planes[2].getBuffer();

                        int ySize = yBuffer.remaining();
                        int uSize = uBuffer.remaining();
                        int vSize = vBuffer.remaining();

                        byte[] yuvData = new byte[ySize + uSize + vSize];
                        yBuffer.get(yuvData, 0, ySize);
                        uBuffer.get(yuvData, ySize, uSize);
                        vBuffer.get(yuvData, ySize + uSize, vSize);

                        return yuvData;
                    } catch (Exception e) {
                        Log.e(TAG, "Error extracting YUV data", e);
                        return null;
                    }
                }
            };

    private Context context;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private Size previewSize;
    private FrameCallback frameCallback;

    public interface FrameCallback {
        void onFrameAvailable(byte[] frameData, int width, int height);
    }

    public CameraManager(Context context) {
        this.context = context;
        Log.d(TAG, "CameraManager created");
    }

    public void setFrameCallback(FrameCallback callback) {
        this.frameCallback = callback;
        Log.d(TAG, "Frame callback set");
    }

    public void startCamera() {
        Log.d(TAG, "Starting camera");
        try {
            startBackgroundThread();
            openCamera();
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera", e);
        }
    }

    public void stopCamera() {
        Log.d(TAG, "Stopping camera");
        try {
            closeCamera();
            stopBackgroundThread();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping camera", e);
        }
    }

    public void release() {
        Log.d(TAG, "Releasing camera");
        stopCamera();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        Log.d(TAG, "Background thread started");
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
                Log.d(TAG, "Background thread stopped");
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera() {
        android.hardware.camera2.CameraManager manager =
                (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            String[] cameraIds = manager.getCameraIdList();
            if (cameraIds.length == 0) {
                Log.e(TAG, "No cameras available");
                return;
            }

            String cameraId = cameraIds[0];
            Log.d(TAG, "Using camera ID: " + cameraId);

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map == null) {
                Log.e(TAG, "StreamConfigurationMap is null");
                return;
            }

            Size[] outputSizes = map.getOutputSizes(ImageFormat.YUV_420_888);
            if (outputSizes == null || outputSizes.length == 0) {
                Log.e(TAG, "No output sizes available for YUV_420_888");
                return;
            }

            previewSize = chooseOptimalSize(outputSizes);

            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(),
                    ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, backgroundHandler);

            manager.openCamera(cameraId, stateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error opening camera", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Camera permission not granted", e);
        }
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        Log.d(TAG, "Camera closed");
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "Camera opened");
            cameraDevice = camera;
            createCaptureSession();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "Camera disconnected");
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "Camera error: " + error);
            camera.close();
            cameraDevice = null;
        }
    };

    private void createCaptureSession() {
        try {
            if (cameraDevice == null || imageReader == null) {
                Log.e(TAG, "Camera device or image reader is null");
                return;
            }

            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            Log.d(TAG, "Capture session configured");
                            captureSession = session;
                            startPreview();
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            Log.e(TAG, "Failed to configure capture session");
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating capture session", e);
        }
    }

    private void startPreview() {
        try {
            if (cameraDevice == null || captureSession == null || imageReader == null) {
                Log.e(TAG, "Cannot start preview - components not ready");
                return;
            }

            CaptureRequest.Builder builder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(imageReader.getSurface());
            builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            // Start the repeating request
            captureSession.setRepeatingRequest(
                    builder.build(),
                    null,
                    backgroundHandler
            );
            Log.d(TAG, "Preview started");
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error starting camera preview", e);
        }
    }
}