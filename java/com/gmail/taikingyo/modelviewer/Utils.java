package com.gmail.taikingyo.modelviewer;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by taiking on 2017/09/15.
 */

public class Utils {

    public static final int FSIZE = Float.SIZE / Byte.SIZE;
    public static final int SSIZE = Short.SIZE / Byte.SIZE;
    public static final int[] TEXTURE_UNIT = {
            GLES20.GL_TEXTURE0,
            GLES20.GL_TEXTURE1,
            GLES20.GL_TEXTURE2,
            GLES20.GL_TEXTURE3,
            GLES20.GL_TEXTURE4,
            GLES20.GL_TEXTURE5,
            GLES20.GL_TEXTURE6,
            GLES20.GL_TEXTURE7,
            GLES20.GL_TEXTURE8,
            GLES20.GL_TEXTURE9,
            GLES20.GL_TEXTURE10,
            GLES20.GL_TEXTURE11,
            GLES20.GL_TEXTURE12,
            GLES20.GL_TEXTURE13,
            GLES20.GL_TEXTURE14,
            GLES20.GL_TEXTURE15,
            GLES20.GL_TEXTURE16,
            GLES20.GL_TEXTURE17,
            GLES20.GL_TEXTURE18,
            GLES20.GL_TEXTURE19,
            GLES20.GL_TEXTURE20,
            GLES20.GL_TEXTURE21,
            GLES20.GL_TEXTURE22,
            GLES20.GL_TEXTURE23,
            GLES20.GL_TEXTURE24,
            GLES20.GL_TEXTURE25,
            GLES20.GL_TEXTURE26,
            GLES20.GL_TEXTURE27,
            GLES20.GL_TEXTURE28,
            GLES20.GL_TEXTURE29,
            GLES20.GL_TEXTURE30,
            GLES20.GL_TEXTURE31,
    };

    static GLSurfaceView initView(Context context, GLSurfaceView.Renderer renderer) {
        GLSurfaceView view = new GLSurfaceView(context);
        view.setEGLContextClientVersion(2);
        view.setRenderer(renderer);

        return view;
    }

    static int initShaders(String v_shader, String f_shader){
        //VertexShader
        int vs = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vs, v_shader);
        GLES20.glCompileShader(vs);
        int[] compiledVS = new int[1];
        GLES20.glGetShaderiv(vs, GLES20.GL_COMPILE_STATUS, compiledVS, 0);
        if(compiledVS[0] == GLES20.GL_FALSE) {
            System.out.println("VS compile err: " + GLES20.glGetShaderInfoLog(vs));
            return 0;
        }

        //FragmentShader
        int fs = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fs, f_shader);
        GLES20.glCompileShader(fs);
        int[] compiledFS = new int[1];
        GLES20.glGetShaderiv(fs, GLES20.GL_COMPILE_STATUS, compiledFS, 0);
        if(compiledFS[0] == GLES20.GL_FALSE) {
            System.out.println("FS compile err: " + GLES20.glGetShaderInfoLog(fs));
            return 0;
        }

        int program = GLES20.glCreateProgram();

        GLES20.glAttachShader(program, vs);
        GLES20.glAttachShader(program, fs);

        GLES20.glLinkProgram(program);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0);

        if(linked[0] == GLES20.GL_TRUE) {
            return program;
        }else {
            System.out.println("link err: " + GLES20.glGetProgramInfoLog(program));
            return 0;
        }
    }

    public static String loadFromAsset(Context context, String file) throws IOException {
        BufferedInputStream bis = null;
        byte[] data;
        try {
            bis = new BufferedInputStream(context.getAssets().open(file));
            int n = bis.available();
            data = new byte[n];
            bis.read(data, 0, n);
        }catch(IOException e) {
            System.out.println("can't loaded shader: " + file);
            throw e;
        }finally {
            if(bis != null) bis.close();
        }

        return new String(data);
    }

    public static FloatBuffer buildFloatBuffer(float[] f) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(f.length * FSIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(f);
        floatBuffer.position(0);

        return floatBuffer;
    }

    public static ByteBuffer buildByteBuffer(byte[] b) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(b.length);
        byteBuffer.order(ByteOrder.nativeOrder());
        byteBuffer.put(b);
        byteBuffer.position(0);

        return byteBuffer;
    }

    public static ShortBuffer buildShortBuffer(short[] s) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(s.length * SSIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(s);
        shortBuffer.position(0);

        return shortBuffer;
    }

    public static void setPerspectiveMatrix(float[] matrix, int offset, float fov, float aspect, float near, float far) {
        float top = near * (float)Math.tan(Math.toRadians(fov));
        float bottom = -top;
        float left = bottom * aspect;
        float right = top * aspect;
        Matrix.frustumM(matrix, offset, left, right, bottom, top, near, far);
    }

    public static void closs(float[] f, float[] vec1, float[] vec2) {
        if(f.length == 3 && vec1.length == 3 && vec2.length == 3) {
            f[0] = vec1[1] * vec2[2] - vec1[2] * vec2[1];
            f[1] = vec1[2] * vec2[0] - vec1[0] * vec2[2];
            f[2] = vec1[0] * vec2[1] - vec1[1] * vec2[0];
            normalizeVecf(f);
        }
    }
    
    public static void normalizeVecf(float[] vec) {
        double sum = 0.0;
        for(float f : vec) sum += Math.pow(f, 2.0);
        double norm = Math.sqrt(sum);
        for(int i = 0; i < vec.length; i++) vec[i] /= norm;
    }

    public static void printFloats(float[] fs, int size) {
        for(int i = 0; i < fs.length; i++) {
            if(i % size == 0) System.out.print("\n" + i / size + ": ");
            System.out.printf("\t%4.2f", fs[i]);
        }
        System.out.println();
    }

    public static void printShorts(short[] ss, int size) {
        for(int i = 0; i < ss.length; i++) {
            if(i % size == 0) System.out.print("\n" + i / size + ": ");
            System.out.printf("\t%2d", ss[i]);
        }
        System.out.println();
    }
}
