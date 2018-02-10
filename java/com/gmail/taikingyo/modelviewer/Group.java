package com.gmail.taikingyo.modelviewer;

import android.opengl.Matrix;

import java.util.LinkedList;

/**
 * Created by taiking on 2017/09/28.
 */

public class Group implements Node {
    protected String name = new String();
    protected float[] localMatrix = new float[16];
    protected float[] worldMatrix = new float[16];
    protected LinkedList<Node> children = new LinkedList<Node>();

    public Group(String name) {
        this.name = name;
        Matrix.setIdentityM(localMatrix, 0);
        Matrix.setIdentityM(worldMatrix, 0);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float[] getLocal() {
        return localMatrix.clone();
    }

    @Override
    public float[] getWorld() {
        return worldMatrix.clone();
    }

    @Override
    public void translate(float x, float y, float z) {
        Matrix.translateM(localMatrix, 0, x, y, z);
    }

    @Override
    public void rotate(float a, float x, float y, float z) {
        Matrix.rotateM(localMatrix, 0, a, x, y, z);
    }

    @Override
    public void toWorld(float[] matrix) {
        Matrix.multiplyMM(worldMatrix, 0, matrix, 0, localMatrix, 0);
        for(Node child : children) child.toWorld(worldMatrix);
    }

    @Override
    public void init() {
        System.out.println(name + ":init");
        for(Node child : children) child.init();
    }

    //子ノードの追加
    public void add(Node node) {
        children.add(node);
    }

    //子ノードリストを取得
    public LinkedList<Node> getChildren() {
        return children;
    }
}
