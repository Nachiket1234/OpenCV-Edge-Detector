#include <jni.h>
#include <android/log.h>
#include <vector>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <GLES3/gl3.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "NativeCpp", __VA_ARGS__)

// Globals
GLuint g_prog = 0, g_tex = 0, g_vao = 0, g_vbo = 0;
GLint g_uTex = -1;
GLint   g_uGray    = -1;
int g_texW = 0, g_texH = 0;

// Simple full‐screen quad
static const float QUAD[16] = {
        -1,  1,  0,0,
        -1, -1,  0,1,
        1,  1,  1,0,
        1, -1,  1,1
};

GLuint compileShader(GLenum t, const char* src) {
    GLuint s = glCreateShader(t);
    glShaderSource(s,1,&src,nullptr);
    glCompileShader(s);
    return s;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeInit(JNIEnv*, jobject) {
    const char* VERT = R"(#version 300 es
        layout(location=0) in vec2 aPos;
        layout(location=1) in vec2 aUV;
        out vec2 vUV;
        void main(){ gl_Position=vec4(aPos,0,1); vUV=aUV; }
    )";
    const char* FRAG = R"(#version 300 es
precision mediump float;
in vec2 vUV;
out vec4 fragColor;
uniform sampler2D u_texture;   // matches Java_com_…_u_texture
uniform bool      uGray;       // our toggle

void main() {
    vec4 c = texture(u_texture, vUV);
    if (uGray) {
        float g = dot(c.rgb, vec3(0.299, 0.587, 0.114));
        fragColor = vec4(vec3(g), c.a);
    } else {
        fragColor = c;
    }
}
)";

    // 1) compile & link
    GLuint vs = compileShader(GL_VERTEX_SHADER,   VERT);
    GLuint fs = compileShader(GL_FRAGMENT_SHADER, FRAG);
    g_prog = glCreateProgram();
    glAttachShader(g_prog, vs);
    glAttachShader(g_prog, fs);
    glLinkProgram(g_prog);
    glDeleteShader(vs);
    glDeleteShader(fs);

    // 2) bind the program so our glGetUniformLocation calls go against it
    glUseProgram(g_prog);

    // 3) now grab both uniform locations
    g_uTex       = glGetUniformLocation(g_prog, "u_texture");
    g_uGray      = glGetUniformLocation(g_prog, "uGray");   // ← here

    // 4) set up your quad VAO/VBO as before
    glGenVertexArrays(1, &g_vao);
    glGenBuffers(1,      &g_vbo);
    glBindVertexArray(g_vao);
    glBindBuffer(GL_ARRAY_BUFFER, g_vbo);
    glBufferData(GL_ARRAY_BUFFER, sizeof(QUAD), QUAD, GL_STATIC_DRAW);
    glVertexAttribPointer(0, 2, GL_FLOAT, GL_FALSE, 4*4, (void*)0);
    glEnableVertexAttribArray(0);
    glVertexAttribPointer(1, 2, GL_FLOAT, GL_FALSE, 4*4, (void*)(2*4));
    glEnableVertexAttribArray(1);
    glBindVertexArray(0);

    // 5) clear-color, etc.
    glClearColor(0,0,0,1);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeResize(JNIEnv*, jobject, jint w, jint h) {
    glViewport(0,0,w,h);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeSetTexture(
        JNIEnv*, jobject, jint texId, jint w, jint h) {
    g_tex = (GLuint)texId;
    g_texW = w; g_texH = h;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeProcess(JNIEnv*, jobject) {
    // no‐op: done in shader
}

extern "C"
JNIEXPORT void JNICALL
Java_com_nachiket_opencvedgedetector_MyRenderer_nativeDrawFrame(JNIEnv*, jobject) {
    glClear(GL_COLOR_BUFFER_BIT);
    if (!g_prog || !g_tex) return;

    glUseProgram(g_prog);
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, g_tex);
    glUniform1i(g_uTex, 0);

    glBindVertexArray(g_vao);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
    glBindVertexArray(0);
}
