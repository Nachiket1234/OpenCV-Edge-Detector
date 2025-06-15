# OpenCV Edge Detector Android App

A real-time camera edge detection application built with Android, OpenCV (C++), OpenGL ES, and JNI integration. This app captures camera frames, processes them using OpenCV edge detection algorithms in native C++, and renders the results using OpenGL ES for optimal performance.

## Features

### Core Functionality
- **Real-time Camera Processing**: Captures camera frames using Android Camera2 API
- **Multiple Processing Modes**: 
  - Original camera feed
  - Grayscale conversion
  - Edge detection using OpenCV
- **OpenGL ES Rendering**: Hardware-accelerated rendering for smooth performance
- **JNI Integration**: Seamless communication between Java and native C++ code

### User Interface
- Three toggle buttons for different processing modes
- Real-time preview with smooth frame rendering
- Automatic camera permission handling

## Technical Requirements

### Dependencies
- **Android SDK**: API Level 23+ (Android 6.0+)
- **NDK**: Native Development Kit for C++ compilation
- **OpenCV for Android**: Computer vision library
- **OpenGL ES 3.0**: Hardware-accelerated graphics rendering
- **Camera2 API**: Modern camera access interface

### Hardware Requirements
- Android device with camera
- OpenGL ES 3.0 support
- Minimum API Level 23

##  Project Architecture

### Directory Structure
```
app/
├── src/main/java/com/nachiket/opencvedgedetector/
│   ├── MainActivity.java          # Main activity with camera permission handling
│   ├── CameraManager.java         # Camera2 API implementation
│   ├── MyGLSurfaceView.java      # OpenGL surface view
│   ├── MyRenderer.java           # OpenGL renderer with JNI calls
│   └── NativeRenderer.java       # JNI interface definitions
├── src/main/cpp/
│   ├── opencvedgedetector.cpp    # Main native implementation
│   ├── native_gl.cpp            # OpenGL native functions
│   └── CMakeLists.txt           # CMake build configuration
└── src/main/res/
    └── layout/activity_main.xml  # UI layout with buttons
```

### Key Components

#### 1. MainActivity.java
- Handles camera permissions using runtime permission requests
- Initializes camera manager and OpenGL surface
- Sets up button click listeners for processing mode switching
- Manages activity lifecycle for camera operations

#### 2. CameraManager.java
- Implements Camera2 API for frame capture
- Configures YUV_420_888 format for optimal processing
- Provides frame callback interface for real-time processing
- Handles camera lifecycle and error management

#### 3. MyRenderer.java
- OpenGL ES renderer with three processing modes
- JNI interface for native C++ communication
- Texture management for camera frame display
- Frame processing coordination between Java and native code

#### 4. Native C++ Implementation
- OpenCV integration for image processing
- OpenGL ES shader programs for rendering
- YUV to RGB conversion for camera frames
- Edge detection algorithms using OpenCV

## 🛠️ Setup Instructions

### Prerequisites
1. **Android Studio**: Latest stable version
2. **Android NDK**: Install via SDK Manager
3. **OpenCV Android SDK**: Download from opencv.org

### OpenCV Setup
1. Download OpenCV Android SDK
2. Extract to your development directory
3. Update `CMakeLists.txt` with your OpenCV path:
   ```cmake
   set(OpenCV_DIR "YOUR_PATH/OpenCV-android-sdk/sdk/native/jni")
   ```

### Build Configuration
1. Clone the repository
2. Open project in Android Studio
3. Update OpenCV path in `CMakeLists.txt`
4. Sync Gradle files
5. Build and run on device

### Permissions Setup
The app automatically requests camera permission at runtime. Ensure your `AndroidManifest.xml` includes:
```xml


```

##  Usage

### Running the App
1. Install and launch the app
2. Grant camera permission when prompted
3. Use the three buttons to switch between modes:
   - **Original**: Raw camera feed
   - **Grayscale**: Converted to grayscale
   - **Edge Detection**: OpenCV edge detection applied

### Processing Modes
- **MODE_ORIGINAL (0)**: Displays unprocessed camera frames
- **MODE_GRAYSCALE (1)**: Applies grayscale conversion using luminance coefficients
- **MODE_EDGE_DETECTION (2)**: Applies Sobel edge detection algorithm

## 🔧 Technical Implementation

### Camera Frame Processing Flow
1. **Capture**: Camera2 API captures YUV_420_888 frames
2. **Conversion**: YUV data converted to RGB format
3. **Processing**: Frames sent to native C++ via JNI
4. **Rendering**: OpenGL ES displays processed frames as textures

### YUV to RGB Conversion
The app handles YUV_420_888 format with proper plane extraction:
- Y plane: Full resolution luminance data
- U/V planes: Quarter resolution chrominance data (subsampled)
- ITU-R BT.601 coefficients used for accurate color conversion

### OpenGL ES Shader Pipeline
- Vertex shader: Handles full-screen quad positioning
- Fragment shader: Applies processing modes based on uniform variables
- Texture sampling: Efficient GPU-based image processing

## Known Issues & Solutions

### Compilation Errors Resolution
If you encounter "Cannot resolve symbol" errors:
1. Add missing constants to `MyRenderer.java`
2. Implement required methods (`setProcessingMode`, `processFrame`)
3. Create proper layout file with button IDs
4. Add correct import statements for annotations

### Permission Issues
- Ensure proper manifest declarations
- Handle runtime permission requests correctly
- Test on different Android versions for compatibility

## 📱 Compatibility

### Tested Configurations
- **Android Versions**: 6.0+ (API 23+)
- **Architecture**: ARM64, ARMv7
- **OpenGL ES**: Version 3.0+
- **Camera**: Camera2 API compatible devices

### Performance Considerations
- Target frame rate: 15-30 FPS
- Memory efficient texture updates using `glTexSubImage2D`
- Background thread processing to avoid UI blocking
- Optimized YUV conversion for real-time performance

## Future Enhancements

### Potential Improvements
- Additional OpenCV filters (Gaussian blur, threshold)
- FPS counter and performance metrics
- Video recording functionality
- Advanced edge detection algorithms
- GPU-accelerated processing using compute shaders

### Code Quality
- Error handling improvements
- Memory leak prevention
- Unit test coverage
- Performance profiling integration

##  License

This project is developed as part of an Android + OpenCV + OpenGL technical assessment for RnD Intern position.

##  Contributing

This is an assessment project. For educational purposes, feel free to fork and experiment with different OpenCV algorithms and OpenGL rendering techniques.

---

*Built with Android Studio, OpenCV 4.x, OpenGL ES 3.0, and Camera2 API*

