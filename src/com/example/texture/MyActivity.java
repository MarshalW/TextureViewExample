package com.example.texture;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.Window;

import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.GL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.*;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetShaderInfoLog;

public class MyActivity extends Activity implements TextureView.SurfaceTextureListener {

    private TextureView textureView;

    private RenderThread renderThread;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题

        setContentView(R.layout.main);

        this.textureView = (TextureView) this.findViewById(R.id.textureView);
        this.textureView.setOpaque(false);
        this.textureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        this.renderThread = new RenderThread(surfaceTexture, width, height);
        this.renderThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    private static class RenderThread extends Thread {
        static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
        static final int EGL_OPENGL_ES2_BIT = 4;

        private static float triangleCoords[] = { // in counterclockwise order:
                0.0f, 0.622008459f, 0.0f,   // top
                -0.5f, -0.311004243f, 0.0f,   // bottom left
                0.5f, -0.311004243f, 0.0f   // bottom right
        };

        private SurfaceTexture surfaceTexture;

        private EGL10 egl;
        private EGLDisplay eglDisplay;
        private EGLConfig eglConfig;
        private EGLContext eglContext;
        private EGLSurface eglSurface;
        private GL gl;

        private int width;
        private int height;

        private FloatBuffer vertexBuffer;

        private final String vertexShaderCode =
                "attribute vec4 vPosition;" +
                        "void main() {" +
                        "  gl_Position = vPosition;" +
                        "}";

        private final String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform vec4 vColor;" +
                        "void main() {" +
                        "  gl_FragColor = vColor;" +
                        "}";

        float color[] = {0.63671875f, 0.76953125f, 0.22265625f, 1.0f};

        private void initGL() {
            egl = (EGL10) EGLContext.getEGL();

            eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed "
                        + GLUtils.getEGLErrorString(egl.eglGetError()));
            }

            int[] version = new int[2];
            if (!egl.eglInitialize(eglDisplay, version)) {
                throw new RuntimeException("eglInitialize failed " +
                        GLUtils.getEGLErrorString(egl.eglGetError()));
            }

            eglConfig = chooseEglConfig();
            if (eglConfig == null) {
                throw new RuntimeException("eglConfig not initialized");
            }

            eglContext = createContext(egl, eglDisplay, eglConfig);

            eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, null);
            if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
                int error = egl.eglGetError();
                if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                    Log.e("glDemo", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                    return;
                }
                throw new RuntimeException("createWindowSurface failed "
                        + GLUtils.getEGLErrorString(error));
            }

            if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                throw new RuntimeException("eglMakeCurrent failed "
                        + GLUtils.getEGLErrorString(egl.eglGetError()));
            }

            gl = eglContext.getGL();

//            glEnable(GL_BLEND);
//            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

//            glDisable(GL_DITHER);
//            glHint(GL_PERSPECTIVE_CORRECTION_HINT,
//                    GL10.GL_FASTEST);

//            glClearColor(0, 0, 0, 0);
//            glEnable(GL_CULL_FACE);
//            glShadeModel(GL_SMOOTH);
//            glEnable(GL_DEPTH_TEST);
        }

        private EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
            return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT,
                    new int[]{EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE});
        }

        private EGLConfig chooseEglConfig() {
            int[] configsCount = new int[1];
            EGLConfig[] configs = new EGLConfig[1];
            int[] configSpec = getConfig();
            if (!egl.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
                throw new IllegalArgumentException("eglChooseConfig failed " +
                        GLUtils.getEGLErrorString(egl.eglGetError()));
            } else if (configsCount[0] > 0) {
                return configs[0];
            }
            return null;
        }

        private int[] getConfig() {
            return new int[]{
                    EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 0,
                    EGL10.EGL_STENCIL_SIZE, 0,
                    EGL10.EGL_NONE
            };
        }

        private void finishGL() {
            egl.eglDestroyContext(eglDisplay, eglContext);
            egl.eglDestroySurface(eglDisplay, eglSurface);
        }

        @Override
        public void run() {
            initGL();

            this.vertexBuffer = ByteBuffer.allocateDirect(triangleCoords.length
                    * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
            this.vertexBuffer.put(triangleCoords).position(0);

            int program = buildProgram(vertexShaderCode, fragmentShaderCode);

            int positionHandler = glGetAttribLocation(program, "vPosition");
            checkGlError();

            int colorHandler = glGetUniformLocation(program, "vColor");
            checkGlError();

            glUseProgram(program);
            checkGlError();

            glEnable(GL_BLEND);
//            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            glEnable(GL_ALPHA_BITS);

            glEnableVertexAttribArray(positionHandler);
            checkGlError();

            glClearColor(0f, 0f, 0f, 0f);
            checkGlError();

            glClear(GL_COLOR_BUFFER_BIT);
            checkGlError();

            glVertexAttribPointer(positionHandler, 3,
                    GL_FLOAT, false,
                    3 * 4, vertexBuffer);
            glUniform4fv(colorHandler, 1, color, 0);

            glDrawArrays(GL_TRIANGLES, 0, 3);

            if (!egl.eglSwapBuffers(eglDisplay, eglSurface)) {
                throw new RuntimeException("Cannot swap buffers");
            }

            finishGL();
        }

        public RenderThread(SurfaceTexture surfaceTexture, int width, int height) {
            this.surfaceTexture = surfaceTexture;
            this.width = width;
            this.height = height;
        }

        private int buildProgram(String vertex, String fragment) {
            int vertexShader = buildShader(vertex, GL_VERTEX_SHADER);
            if (vertexShader == 0) return 0;

            int fragmentShader = buildShader(fragment, GL_FRAGMENT_SHADER);
            if (fragmentShader == 0) return 0;

            int program = glCreateProgram();
            glAttachShader(program, vertexShader);
            checkGlError();

            glAttachShader(program, fragmentShader);
            checkGlError();

            glLinkProgram(program);
            checkGlError();

            int[] status = new int[1];
            glGetProgramiv(program, GL_LINK_STATUS, status, 0);
            if (status[0] != GL_TRUE) {
                String error = glGetProgramInfoLog(program);
                Log.d("glDemo", "Error while linking program:\n" + error);
                glDeleteShader(vertexShader);
                glDeleteShader(fragmentShader);
                glDeleteProgram(program);
                return 0;
            }

            return program;
        }

        private int buildShader(String source, int type) {
            int shader = glCreateShader(type);

            glShaderSource(shader, source);
            checkGlError();

            glCompileShader(shader);
            checkGlError();

            int[] status = new int[1];
            glGetShaderiv(shader, GL_COMPILE_STATUS, status, 0);
            if (status[0] != GL_TRUE) {
                String error = glGetShaderInfoLog(shader);
                Log.d("glDemo", "Error while compiling shader:\n" + error);
                glDeleteShader(shader);
                return 0;
            }

            return shader;
        }

        private void checkGlError() {
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                Log.w("glDemo", "GL error = 0x" + Integer.toHexString(error));
            }
        }
    }
}