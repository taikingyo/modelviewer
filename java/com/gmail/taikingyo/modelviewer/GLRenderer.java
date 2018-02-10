package com.gmail.taikingyo.modelviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.util.LinkedList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by taiking on 2017/09/15.
 */

class GLRenderer implements GLSurfaceView.Renderer {
    private static final String ATTRIB_POSITION_NAME = "a_Position";
    private static final String ATTRIB_COLOR_NAME = "a_Color";
    private static final String ATTRIB_NORMAL_NAME = "a_Normal";
    private static final String ATTRIB_TEXCOORD_NAME = "a_TexCoord";
    private static final String UNIFORM_MVPMATRIX_NAME = "u_MvpMatrix";
    private static final String UNIFORM_MODEL_MATRIX_NAME = "u_ModelMatrix";
    private static final String UNIFORM_NORMAL_MATRIX_NAME = "u_NormalMatrix";
    private static final String UNIFORM_SAMPLER_NAME = "u_Sampler";
    private static final String UNIFORM_POINT_LIGHT_NAME = "u_PointLight";
    private static final String UNIFORM_POINT_LIGHT_POSITION_NAME = "u_PointLightPosition";
    private static final String UNIFORM_AMBIENT_LIGHT_NAME = "u_AmbientLight";

    private int numOfTextureUnit;

    private String solidVertexShader, solidFragmentShader, texVertexShader, texFragmentShader;
    private int solidProgram, texProgram;

    private int u_MvpMatrix;
    private int u_ModelMatrix;
    private int u_NormalMatrix;
    private int u_Sampler;

    private float[] normalMatrix = new float[16];
    private float[] viewMatrix = new float[16];
    private float[] projectionMatrix = new float[16];
    private float[] vpMatrix = new float[16];
    private float[] vpRotMatrix = new float[16];
    private float[] rotationMatrix = new float[16];
    private float[] mvpMatrix = new float[16];

    private LinkedList<Node> model;
    private LinkedList<SolidSegment> solids;
    private LinkedList<TexturedSegment> textures;
    private Bitmap[] maps;

    private boolean lockRotate = false;

    public GLRenderer(String[] shaders, LinkedList<Node> model, Bitmap[] maps) {
        this.solidVertexShader = shaders[0];
        this.solidFragmentShader = shaders[1];
        this.texVertexShader = shaders[2];
        this.texFragmentShader = shaders[3];
        this.model = model;
        this.maps = maps;

        textures = new LinkedList<TexturedSegment>();
        solids = new LinkedList<SolidSegment>();
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        int[] units = new int[1];
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_IMAGE_UNITS, units, 0);
        numOfTextureUnit = units[0];
        Log.i("GLRenderer", "num of texture unit: " + numOfTextureUnit);
        solidProgram = Utils.initShaders(solidVertexShader, solidFragmentShader);
        texProgram = Utils.initShaders(texVertexShader, texFragmentShader);

        Matrix.setLookAtM(viewMatrix, 0,
                0.0f, 0.0f, 30.0f,
                0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f);

        Matrix.setIdentityM(rotationMatrix, 0);

        GLES20.glClearColor(0.0f, 0.0f, 0.2f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA , GLES20.GL_ONE_MINUS_SRC_ALPHA);

        initTexture();

        for(Node node : model) {
            node.init();
            toClass(node);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float aspect = (float)width / height;
        Utils.setPerspectiveMatrix(projectionMatrix, 0, 30.0f, aspect, 1.0f, 100.0f);
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(vpRotMatrix, 0, vpMatrix, 0, rotationMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        drawModel();
    }

    private void drawModel() {
        float[] iMat = new float[16];
        Matrix.setIdentityM(iMat, 0);

        for(Node node : model) node.toWorld(iMat);

        //テクスチャ無しセグメントの描画
        setProgram(solidProgram);
        for(SolidSegment seg : solids) if(seg.isVisible()) drawSolidSegment(seg);

        //テクスチャ付セグメントの描画
        setProgram(texProgram);
        for(TexturedSegment seg : textures) if(seg.isVisible()) drawTexturedSegment(seg);
    }

    //使用するプログラム（シェーダ）の準備
    private void setProgram(int program) {
        GLES20.glUseProgram(program);
        initLighting(program);
        u_MvpMatrix = GLES20.glGetUniformLocation(program, UNIFORM_MVPMATRIX_NAME);
        u_ModelMatrix = GLES20.glGetUniformLocation(program, UNIFORM_MODEL_MATRIX_NAME);
        u_NormalMatrix = GLES20.glGetUniformLocation(program, UNIFORM_NORMAL_MATRIX_NAME);
    }

    //セグメントの種類毎にリストに追加
    private void toClass(Node node) {
        if(node.getClass() == TexturedSegment.class && maps.length > 0) {
            if(((TexturedSegment) node).getTextureUnit() < numOfTextureUnit) {
                Log.i("GLRenderer", node.getName() + " is TexturedSegment");
                textures.add((TexturedSegment) node);
            }else {
                Log.i("GLRenderer", "full of texture unit " + node.getName() + " is SolidSegment");
                solids.add((SolidSegment) node);
            }
        }else if(node.getClass() == SolidSegment.class) {
            Log.i("GLRenderer", node.getName() + " is SolidSegment");
            solids.add((SolidSegment) node);
        }
        if(node.getClass() == Group.class) {
            LinkedList<Node> children = ((Group) node).getChildren();
            for(Node child : children) toClass(child);
        }
    }
    
    private void drawTexturedSegment(TexturedSegment segment) {
        //ワールド座標への変換行列と視点、表示領域行列からmvp行列を作りGLESへ渡す
        float[] matrix = new float[16];
        Matrix.rotateM(matrix, 0, segment.getWorld(), 0, 90.0f, 1.0f, 0, 0);
        GLES20.glUniformMatrix4fv(u_ModelMatrix, 1, false, matrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, vpRotMatrix, 0, matrix, 0);
        GLES20.glUniformMatrix4fv(u_MvpMatrix, 1, false, mvpMatrix, 0);

        //法線の変換行列を作りGLESへ渡す
        float[] invertMatrix = new float[16];
        Matrix.invertM(invertMatrix, 0, matrix, 0);
        Matrix.transposeM(normalMatrix, 0, invertMatrix, 0);
        GLES20.glUniformMatrix4fv(u_NormalMatrix, 1, false, normalMatrix, 0);

        //使用するテクスチャユニット番号をGLESへ渡す
        int unit = segment.getTextureUnit();
        GLES20.glActiveTexture(Utils.TEXTURE_UNIT[unit]);
        GLES20.glUniform1i(u_Sampler, unit);

        //頂点の座標データをGLESへ渡す
        int a_Position = GLES20.glGetAttribLocation(texProgram, ATTRIB_POSITION_NAME);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, segment.getPositions());
        GLES20.glVertexAttribPointer(a_Position, segment.elementsOfPosition(), GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(a_Position);

        //頂点の色データをGLESへ渡す
        int a_Color = GLES20.glGetAttribLocation(texProgram, ATTRIB_COLOR_NAME);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, segment.getColors());
        GLES20.glVertexAttribPointer(a_Color, segment.elementsOfColor(), GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(a_Color);

        //頂点の法線データをGLESへ渡す
        int a_Normal = GLES20.glGetAttribLocation(texProgram, ATTRIB_NORMAL_NAME);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, segment.getNormals());
        GLES20.glVertexAttribPointer(a_Normal, 3, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(a_Normal);

        //頂点のテクスチャ座標データをGLESへ渡す
        int a_TexCoord = GLES20.glGetAttribLocation(texProgram, ATTRIB_TEXCOORD_NAME);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, segment.getTexCoords());
        GLES20.glVertexAttribPointer(a_TexCoord, 2, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(a_TexCoord);

        //面のインデックスデータをGLESへ渡す
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, segment.getIndices());

        //α値が設定されている場合デプスバッファへ書き込まない
        if(segment.elementsOfColor() == 4) {
            GLES20.glDepthMask(false);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, segment.numObVertices(), GLES20.GL_UNSIGNED_SHORT, 0);
            GLES20.glDepthMask(true);
        }else GLES20.glDrawElements(GLES20.GL_TRIANGLES, segment.numObVertices(), GLES20.GL_UNSIGNED_SHORT, 0);
    }

    private void drawSolidSegment(SolidSegment segment) {
        //ワールド座標への変換行列と視点、表示領域行列からmvp行列を作りGLESへ渡す
        float[] matrix = new float[16];
        Matrix.rotateM(matrix, 0, segment.getWorld(), 0, 90.0f, 1.0f, 0, 0);
        GLES20.glUniformMatrix4fv(u_ModelMatrix, 1, false, matrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, vpRotMatrix, 0, matrix, 0);
        GLES20.glUniformMatrix4fv(u_MvpMatrix, 1, false, mvpMatrix, 0);

        //法線の変換行列を作りGLESへ渡す
        float[] invertMatrix = new float[16];
        Matrix.invertM(invertMatrix, 0, matrix, 0);
        Matrix.transposeM(normalMatrix, 0, invertMatrix, 0);
        GLES20.glUniformMatrix4fv(u_NormalMatrix, 1, false, normalMatrix, 0);

        //頂点の座標データをGLESへ渡す
        int a_Position = GLES20.glGetAttribLocation(solidProgram, ATTRIB_POSITION_NAME);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, segment.getPositions());
        GLES20.glVertexAttribPointer(a_Position, segment.elementsOfPosition(), GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(a_Position);

        //頂点の色データをGLESへ渡す
        int a_Color = GLES20.glGetAttribLocation(solidProgram, ATTRIB_COLOR_NAME);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, segment.getColors());
        GLES20.glVertexAttribPointer(a_Color, segment.elementsOfColor(), GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(a_Color);

        //頂点の法線データをGLESへ渡す
        int a_Normal = GLES20.glGetAttribLocation(solidProgram, ATTRIB_NORMAL_NAME);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, segment.getNormals());
        GLES20.glVertexAttribPointer(a_Normal, 3, GLES20.GL_FLOAT, false, 0, 0);
        GLES20.glEnableVertexAttribArray(a_Normal);

        //面のインデックスデータをGLESへ渡す
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, segment.getIndices());

        //α値が設定されている場合デプスバッファへ書き込まない
        if(segment.elementsOfColor() == 4) {
            GLES20.glDepthMask(false);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, segment.numObVertices(), GLES20.GL_UNSIGNED_SHORT, 0);
            GLES20.glDepthMask(true);
        }else GLES20.glDrawElements(GLES20.GL_TRIANGLES, segment.numObVertices(), GLES20.GL_UNSIGNED_SHORT, 0);
    }

    private void initTexture() {
        int numOfTexture = Math.min(maps.length, numOfTextureUnit);
        int[] texts = new int[numOfTexture];
        GLES20.glGenTextures(numOfTexture, texts, 0);
        u_Sampler = GLES20.glGetUniformLocation(texProgram, UNIFORM_SAMPLER_NAME);
        for(int i = 0; i < numOfTexture; i++) {
            GLES20.glActiveTexture(Utils.TEXTURE_UNIT[i]);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texts[i]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, maps[i], 0);
        }
    }

    private void initLighting(int program) {
        int u_PointLight = GLES20.glGetUniformLocation(program, UNIFORM_POINT_LIGHT_NAME);
        int u_PointLightPosition = GLES20.glGetUniformLocation(program, UNIFORM_POINT_LIGHT_POSITION_NAME);
        int u_AmbientLight = GLES20.glGetUniformLocation(program, UNIFORM_AMBIENT_LIGHT_NAME);

        GLES20.glUniform3f(u_PointLight, 0.7f, 0.7f, 0.7f);
        GLES20.glUniform3f(u_PointLightPosition, 5.0f, 7.0f, 9.0f);
        GLES20.glUniform3f(u_AmbientLight, 0.3f, 0.3f, 0.3f);
    }

    void rotateView(float[] rotationMatrix) {
        if(!lockRotate) {
            this.rotationMatrix = rotationMatrix;
            Matrix.multiplyMM(vpRotMatrix, 0, vpMatrix, 0, rotationMatrix, 0);
        }
    }

    void lock(boolean b) {
        lockRotate = b;
    }
}
