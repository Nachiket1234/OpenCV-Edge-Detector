#include <jni.h>
#include <string>
#include <vector>
#include <android/bitmap.h>


// OpenCV headers
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>

// OpenGL ES 3.0 headers
#include <GLES3/gl3.h>

// Android Logging
#include <android/log.h>
#define LOG_TAG "NativeCpp"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// --- Global Variables for OpenGL State ---
GLuint g_programId = 0;
GLuint g_textureId = 0;
GLuint g_vbo = 0;
GLuint g_vao = 0;

GLint g_uTextureLocation = -1; // Location of the texture uniform in the shader
GLint g_textureWidth = 0;
GLint g_textureHeight = 0;
// at the very top, after your #includes
static GLint   g_uGrayLocation = -1;   // uniform location for the “u_gray” toggle
static bool    doGray           = false; // C++ mirror of your Java “requestProcessing” flag



// --- Shader Source Code ---
// Vertex shader
const char* VERTEX_SHADER = R"(#version 300 es
precision mediump float;
layout(location = 0) in vec2 a_position;
layout(location = 1) in vec2 a_texCoord;
out vec2 v_texCoord;
void main() {
    gl_Position = vec4(a_position, 0.0, 1.0);
    v_texCoord = a_texCoord;
}
)";

// Fragment shader
const char* FRAGMENT_SHADER = R"(#version 300 es
precision mediump float;
in vec2 v_texCoord;
out vec4 fragColor;
uniform sampler2D u_texture;
uniform bool u_gray;         // new toggle
void main() {
    vec4 c = texture(u_texture, v_texCoord);
    if(u_gray) {
      float g = dot(c.rgb, vec3(0.299, 0.587, 0.114));
      fragColor = vec4(g, g, g, c.a);
    } else {
      fragColor = c;
    }
}
)";



// --- Helper Function to Compile and Link Shaders ---
GLuint createProgram(const char* vertexSrc, const char* fragmentSrc) {
    auto loadShader = [](GLenum type, const char* shaderSrc) -> GLuint {
        GLuint shader = glCreateShader(type);
        glShaderSource(shader, 1, &shaderSrc, nullptr);
        glCompileShader(shader);

        GLint compiled = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
        if (!compiled) {
            GLint logLen = 0;
            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &logLen);
            std::string log(logLen, ' ');
            glGetShaderInfoLog(shader, logLen, nullptr, log.data());
            LOGI("Shader compile failed:\n%s", log.c_str());
            glDeleteShader(shader);
            return 0;
        }
        return shader;
    };

    GLuint vertexShader = loadShader(GL_VERTEX_SHADER, vertexSrc);
    GLuint fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentSrc);
    GLuint program = glCreateProgram();
    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    glLinkProgram(program);
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);
    return program;
}

// --- JNI Implementations from MyRenderer.java ---

extern "C" JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeInit(JNIEnv* env, jobject thiz) {
    LOGI(">>> nativeInit() called, program=%d", g_programId);
    g_programId = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
    glUseProgram(g_programId);
    g_uTextureLocation = glGetUniformLocation(g_programId, "u_texture");
    // 2) Grab the grayscale toggle location
    g_uGrayLocation = glGetUniformLocation(g_programId, "u_gray");
    // Make sure the default is “off” (0)
    glUniform1i(g_uGrayLocation, 0);

    GLfloat vertices[] = {-1.0f,  1.0f, 0.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, 1.0f,  1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 1.0f, 1.0f};
    glGenVertexArrays(1, &g_vao);
    glBindVertexArray(g_vao);
    glGenBuffers(1, &g_vbo);
    glBindBuffer(GL_ARRAY_BUFFER, g_vbo);
    glBufferData(GL_ARRAY_BUFFER, sizeof(vertices), vertices, GL_STATIC_DRAW);
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), (void*)0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(GLfloat), (void*)(2 * sizeof(GLfloat)));
    glEnableVertexAttribArray(1);
    glBindVertexArray(0);
    glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
}

extern "C" JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeResize(JNIEnv* env, jobject thiz, jint width, jint height) {
    glViewport(0, 0, width, height);
}

// JNI stub to update the quad vertices
extern "C"
JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeUpdateVertices(
        JNIEnv*, jobject,
        jfloat viewAspect, jfloat imageAspect) {

    // determine scaling on X or Y to preserve image aspect
    float xScale, yScale;
    if (viewAspect > imageAspect) {
        xScale = imageAspect / viewAspect;
        yScale = 1.0f;
    } else {
        xScale = 1.0f;
        yScale = viewAspect / imageAspect;
    }

    // interleaved {pos.x, pos.y, tex.u, tex.v}
    float verts[] = {
            -xScale,  yScale,  0.0f, 0.0f,
            -xScale, -yScale,  0.0f, 1.0f,
            xScale,  yScale,  1.0f, 0.0f,
            xScale, -yScale,  1.0f, 1.0f,
    };

    // update the GPU-side buffer:
    glBindBuffer(GL_ARRAY_BUFFER, g_vbo);
    glBufferSubData(GL_ARRAY_BUFFER, 0, sizeof(verts), verts);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
}


// **FIX APPLIED**: This function now receives width and height from Java.
extern "C" JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeSetTexture(
        JNIEnv* env, jobject thiz, jint texture_id, jint width, jint height) {
    g_textureId = (GLuint)texture_id;
    g_textureWidth = width;
    g_textureHeight = height;
    LOGI("Native texture set. ID: %d, Size: %dx%d", g_textureId, g_textureWidth, g_textureHeight);
}

extern "C" JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeProcess(JNIEnv *env, jobject thiz) {
    if (g_textureId == 0 || g_textureWidth == 0 || g_textureHeight == 0) return;
    glPixelStorei(GL_PACK_ALIGNMENT,   1);
    glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    std::vector<unsigned char> buffer(g_textureWidth * g_textureHeight * 4);
    glBindTexture(GL_TEXTURE_2D, g_textureId);
    glReadPixels(0, 0, g_textureWidth, g_textureHeight, GL_RGBA, GL_UNSIGNED_BYTE, buffer.data());

    cv::Mat src(g_textureHeight, g_textureWidth, CV_8UC4, buffer.data());
    cv::Mat gray, gray_rgba;
    cv::cvtColor(src, gray, cv::COLOR_RGBA2GRAY);
    cv::cvtColor(gray, gray_rgba, cv::COLOR_GRAY2RGBA);
    memcpy(buffer.data(), gray_rgba.data, gray_rgba.total() * gray_rgba.elemSize());

    doGray = true;
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, g_textureWidth, g_textureHeight, GL_RGBA, GL_UNSIGNED_BYTE, buffer.data());
    glBindTexture(GL_TEXTURE_2D, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeDrawFrame(JNIEnv *env, jobject thiz) {
    glClear(GL_COLOR_BUFFER_BIT);
    if (g_programId == 0 || g_textureId == 0) return;

    // 1) Make sure we're using our shader program
    glUseProgram(g_programId);

    // 2) Set the uniforms
    glUniform1i(g_uGrayLocation, doGray ? 1 : 0);
    glUniform1i(g_uTextureLocation, 0);  // texture unit 0

    // 3) Bind the texture
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, g_textureId);

    // 4) Draw the quad
    glBindVertexArray(g_vao);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    // 5) Cleanup
    glBindVertexArray(0);
    glBindTexture(GL_TEXTURE_2D, 0);

    // optionally reset doGray so it only applies once
    doGray = false;
}
