cmake_minimum_required(VERSION 3.22.1)
project(opencvedgedetector LANGUAGES CXX)

# point directly to the jni folder that contains OpenCVConfig.cmake
set(OpenCV_DIR "C:/OpenCV-android-sdk/OpenCV-android-sdk/sdk/native/jni")
message(STATUS "OpenCV_DIR = ${OpenCV_DIR}")
find_package(OpenCV REQUIRED)
list(APPEND CMAKE_PREFIX_PATH "${OpenCV_DIR}")

# Disable IPP optimizations (avoids trying to link libippicv.a)
set(OpenCV_USE_IPP OFF CACHE BOOL "" FORCE)

# Then point to your OpenCV SDK and find it:
set(OpenCV_DIR "C:/OpenCV-android-sdk/OpenCV-android-sdk/sdk/native/jni")
find_package(OpenCV REQUIRED)


find_package(OpenCV REQUIRED)

# 2. Include OpenCV headers
include_directories(${OpenCV_INCLUDE_DIRS})

# 3. Pull in the Android log library
find_library(log-lib log)
# 3b. Pull in the Android graphics (bitmap) library
find_library(jnigraphics-lib jnigraphics)
find_library(EGL-lib    EGL)
find_library(GLESv3-lib GLESv3)



# 5. Declare your native library and source files
add_library(
        opencvedgedetector              # C++ library name
        SHARED
        opencvedgedetector.cpp
        #native_gl.cpp
        # your existing JNI code
)

# 6. Link against OpenCV, log, and (eventually) GLES/EGL
target_link_libraries(
        opencvedgedetector
        ${OpenCV_LIBS}
        ${log-lib}
        ${jnigraphics-lib}
        ${GLESv3-lib}
        ${EGL-lib}
)