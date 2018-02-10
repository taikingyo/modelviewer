package com.gmail.taikingyo.modelviewer;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by taiking on 2017/09/21.
 */

public class SimpleSegment extends Group {
    protected int iPosition;
    protected int iNormals;
    protected int iIndices;
    protected int vertices;
    protected int ePosition;
    protected boolean visible = true;

    protected FloatBuffer positions, normals;
    protected ShortBuffer indices;

    public SimpleSegment(String name, FloatBuffer positions, FloatBuffer normals, ShortBuffer indices, int ePosition) {
        super(name);
        this.positions = positions;
        this.normals = normals;
        this.indices = indices;
        this.ePosition = ePosition;

        vertices = indices.limit();
    }

    @Override
    public void init() {
        iPosition = initBuffer(positions);
        iNormals = initBuffer(normals);

        int[] indexBuffer = new int[1];
        GLES20.glGenBuffers(1, indexBuffer, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexBuffer[0]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, indices.limit() * Utils.SSIZE, indices, GLES20.GL_STATIC_DRAW);
        iIndices = indexBuffer[0];

        Log.i("Segment", name + ":init");
        for(Node child : children) child.init();
    }

    //座標バッファのインデックスを取得
    public int getPositions() {
        return iPosition;
    }

    //法線バッファのインデックスを取得
    public int getNormals() {
        return iNormals;
    }

    //面インデックスバッファのインデックスを取得
    public int getIndices() {
        return iIndices;
    }

    //頂点数を取得
    public int numObVertices() {
        return vertices;
    }

    //座標の要素数を取得
    public int elementsOfPosition() {
        return ePosition;
    }

    //表示の切り替え
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    //表示の確認
    public boolean isVisible() {
        return visible;
    }

    //バッファの準備
    protected int initBuffer(FloatBuffer data) {
        int[] buffer = new int[1];
        GLES20.glGenBuffers(1, buffer, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, buffer[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, Utils.FSIZE * data.limit(), data, GLES20.GL_STATIC_DRAW);
        return buffer[0];
    }
}
