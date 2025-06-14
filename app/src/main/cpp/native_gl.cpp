#include <jni.h>
#include <GLES3/gl3.h>

extern "C" {

JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeInit(JNIEnv*, jobject) {
// Set clear color to a distinct blue
glClearColor(0.1f, 0.2f, 0.3f, 1.0f);
}

JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeResize(JNIEnv*, jobject, jint w, jint h) {
glViewport(0, 0, w, h);
}

JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeDrawFrame(JNIEnv*, jobject) {
// Clear the screen
glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
}

JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeProcess(JNIEnv*, jobject) {
    // call your OpenCV JNI code here, or trigger whatever processing you need
}

}
