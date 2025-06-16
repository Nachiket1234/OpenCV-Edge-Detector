#include <jni.h>
#include <android/log.h>
#include <vector>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <GLES3/gl3.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "NativeCpp", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "NativeCpp", __VA_ARGS__)

// Globals
GLuint g_prog = 0, g_tex = 0, g_vao = 0, g_vbo = 0;
GLint g_uTex = -1;
GLint g_uMode = -1;
int g_texW = 0, g_texH = 0;

// Simple full‐screen quad (position + UV coordinates)
static const float QUAD[16] = {
        // pos      // UV
        -1.0f,  1.0f,  0.0f, 0.0f,  // top-left
        -1.0f, -1.0f,  0.0f, 1.0f,  // bottom-left
        1.0f,  1.0f,  1.0f, 0.0f,  // top-right
        1.0f, -1.0f,  1.0f, 1.0f   // bottom-right
};

GLuint compileShader(GLenum type, const char* src) {
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &src, nullptr);
    glCompileShader(shader);

    // Check compilation status
    GLint compiled;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);
    if (!compiled) {
        GLint infoLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 1) {
            char* infoLog = (char*)malloc(sizeof(char) * infoLen);
            glGetShaderInfoLog(shader, infoLen, NULL, infoLog);
            LOGE("Error compiling %s shader: %s",
                 (type == GL_VERTEX_SHADER) ? "vertex" : "fragment", infoLog);
            free(infoLog);
        }
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeInit(JNIEnv*, jobject) {
    LOGI("Starting native initialization");

    const char* VERT = R"(#version 300 es
precision mediump float;
layout(location=0) in vec2 aPos;
layout(location=1) in vec2 aUV;
out vec2 vUV;
void main() {
    gl_Position = vec4(aPos, 0.0, 1.0);
    vUV = aUV;
}
)";

    const char* FRAG = R"(#version 300 es
precision mediump float;
in vec2 vUV;
out vec4 fragColor;
uniform sampler2D u_texture;
uniform int uMode;

void main() {
    vec4 color = texture(u_texture, vUV);

    if (uMode == 1) {
        // Grayscale mode
        float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        fragColor = vec4(vec3(gray), color.a);
    } else if (uMode == 2) {
        // Edge detection mode
        vec2 texelSize = 1.0 / textureSize(u_texture, 0);

        // Sample neighboring pixels for Sobel filter
        vec3 tl = texture(u_texture, vUV + vec2(-texelSize.x, -texelSize.y)).rgb;
        vec3 tm = texture(u_texture, vUV + vec2(0.0, -texelSize.y)).rgb;
        vec3 tr = texture(u_texture, vUV + vec2(texelSize.x, -texelSize.y)).rgb;
        vec3 ml = texture(u_texture, vUV + vec2(-texelSize.x, 0.0)).rgb;
        vec3 mr = texture(u_texture, vUV + vec2(texelSize.x, 0.0)).rgb;
        vec3 bl = texture(u_texture, vUV + vec2(-texelSize.x, texelSize.y)).rgb;
        vec3 bm = texture(u_texture, vUV + vec2(0.0, texelSize.y)).rgb;
        vec3 br = texture(u_texture, vUV + vec2(texelSize.x, texelSize.y)).rgb;

        // Sobel X and Y gradients
        vec3 sobelX = tl + 2.0*ml + bl - tr - 2.0*mr - br;
        vec3 sobelY = tl + 2.0*tm + tr - bl - 2.0*bm - br;

        // Calculate magnitude
        vec3 sobel = sqrt(sobelX*sobelX + sobelY*sobelY);
        float edge = length(sobel) / 3.0;

        fragColor = vec4(vec3(edge), 1.0);
    } else {
        // Original mode
        fragColor = color;
    }
}
)";

    // Compile shaders
    GLuint vertexShader = compileShader(GL_VERTEX_SHADER, VERT);
    GLuint fragmentShader = compileShader(GL_FRAGMENT_SHADER, FRAG);

    if (vertexShader == 0 || fragmentShader == 0) {
        LOGE("Failed to compile shaders");
        return;
    }

    // Create and link program
    g_prog = glCreateProgram();
    glAttachShader(g_prog, vertexShader);
    glAttachShader(g_prog, fragmentShader);
    glLinkProgram(g_prog);

    // Check link status
    GLint linked;
    glGetProgramiv(g_prog, GL_LINK_STATUS, &linked);
    if (!linked) {
        GLint infoLen = 0;
        glGetProgramiv(g_prog, GL_INFO_LOG_LENGTH, &infoLen);
        if (infoLen > 1) {
            char* infoLog = (char*)malloc(sizeof(char) * infoLen);
            glGetProgramInfoLog(g_prog, infoLen, NULL, infoLog);
            LOGE("Error linking program: %s", infoLog);
            free(infoLog);
        }
        glDeleteProgram(g_prog);
        g_prog = 0;
        return;
    }

    // Clean up shaders
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);

    // Get uniform locations
    glUseProgram(g_prog);
    g_uTex = glGetUniformLocation(g_prog, "u_texture");
    g_uMode = glGetUniformLocation(g_prog, "uMode");

    LOGI("Uniform locations - texture: %d, mode: %d", g_uTex, g_uMode);

    // Set up vertex array and buffer
    glGenVertexArrays(1, &g_vao);
    glGenBuffers(1, &g_vbo);

    glBindVertexArray(g_vao);
    glBindBuffer(GL_ARRAY_BUFFER, g_vbo);
    glBufferData(GL_ARRAY_BUFFER, sizeof(QUAD), QUAD, GL_STATIC_DRAW);

    // Position attribute
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), (void*)0);
    glEnableVertexAttribArray(0);

    // UV attribute
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 4 * sizeof(float), (void*)(2 * sizeof(float)));
    glEnableVertexAttribArray(1);

    glBindVertexArray(0);

    // Set clear color
    glClearColor(0.2f, 0.2f, 0.2f, 1.0f);

    // Enable blending for proper alpha handling
    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    LOGI("Native initialization completed successfully");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeResize(JNIEnv*, jobject, jint w, jint h) {
    glViewport(0, 0, w, h);
    LOGI("Viewport set to %dx%d", w, h);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeSetTexture(
        JNIEnv*, jobject, jint texId, jint w, jint h) {
    g_tex = (GLuint)texId;
    g_texW = w;
    g_texH = h;
    LOGI("Texture set: ID=%d, size=%dx%d", texId, w, h);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeDrawFrame(JNIEnv*, jobject, jint mode) {
    // Clear the screen
    glClear(GL_COLOR_BUFFER_BIT);

    // Check if we have everything we need
    if (g_prog == 0) {
        LOGE("No shader program available");
        return;
    }

    if (g_tex == 0) {
        LOGE("No texture available");
        return;
    }

    // Use our shader program
    glUseProgram(g_prog);

    // Bind texture
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, g_tex);

    // Set uniforms
    if (g_uTex >= 0) {
        glUniform1i(g_uTex, 0);
    }
    if (g_uMode >= 0) {
        glUniform1i(g_uMode, mode);
    }

    // Draw the quad
    glBindVertexArray(g_vao);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glBindVertexArray(0);

    // Check for OpenGL errors
    GLenum error = glGetError();
    if (error != GL_NO_ERROR) {
        LOGE("OpenGL error in drawFrame: 0x%x", error);
    }
}

// Unused functions for compatibility
extern "C"
JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeSetMode(JNIEnv*, jobject, jint mode) {
    LOGI("Native mode set to: %d", mode);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeProcess(JNIEnv*, jobject) {
    // Processing is handled in the shader
}