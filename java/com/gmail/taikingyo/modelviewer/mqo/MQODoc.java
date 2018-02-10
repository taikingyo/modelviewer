package com.gmail.taikingyo.modelviewer.mqo;

import android.graphics.Bitmap;

import com.gmail.taikingyo.modelviewer.Group;
import com.gmail.taikingyo.modelviewer.SolidSegment;
import com.gmail.taikingyo.modelviewer.TexturedSegment;
import com.gmail.taikingyo.modelviewer.Utils;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;

/**
 * Created by taiking on 2017/09/25.
 */

public class MQODoc {
    public String name;
    public float version;
    public MQOMaterial[] materials;
    public LinkedList<MQObject> objects = new LinkedList<MQObject>();
    public Bitmap[] textures;

    public Group buildNode() {
        Group root = new Group(name);

        for(MQObject obj : objects) {
            String name = obj.name;
            int numOfVertices;
            float[] vertices;
            float[] normals;
            float[] colors;
            float[] texCoords;
            LinkedList<Short> indices = new LinkedList<Short>();
            int eColor = 3;
            float[] colors4;
            boolean textured = false;

            //フラットシェーディングなら頂点を面毎に分割
            if(obj.shading == 0) {
                numOfVertices = 0;
                for(MQOFace face : obj.faces) numOfVertices += face.vIndex.length;
                vertices = new float[numOfVertices * 3];
                normals = new float[numOfVertices * 3];
                texCoords = new float[numOfVertices * 2];
                colors4 = new float[numOfVertices * 4];

                int vIndex = 0;
                for(MQOFace face : obj.faces) {
                    if(materials[face.mIndex].textureUnit != -1) textured = true;
                    int[] idx = face.vIndex;
                    int n = idx.length;

                    //外積により法線ベクトルを計算
                    float[] v1 = new float[3];
                    float[] v2 = new float[3];
                    for(int i = 0; i < 3; i++) {
                        v1[i] = obj.vertices[idx[1] * 3 + i] - obj.vertices[idx[0] * 3 + i];
                        v2[i] = obj.vertices[idx[2] * 3 + i] - obj.vertices[idx[0] * 3 + i];
                    }

                    float[] normal = new float[3];
                    Utils.closs(normal, v2, v1);

                    //色の取得
                    float[] color = new float[4];
                    if(face.mIndex >= 0) {
                        MQOMaterial material = materials[face.mIndex];
                        //処理の重さを考慮して、不要な透過処理を回避
                        if(material.color[3] != 1.0f) eColor = 4;
                        color = material.color;
                    }else {
                        System.arraycopy(obj.color, 0, color, 0, 3);
                        color[3] = 1.0f;
                    }

                    //面毎に頂点を作成
                    for(int i = 0; i < n; i++) {
                        int index = vIndex + i;
                        System.arraycopy(obj.vertices, idx[i] * 3, vertices, index * 3, 3);
                        System.arraycopy(normal, 0, normals, index * 3, 3);
                        System.arraycopy(color, 0, colors4, index * 4, 4);
                        System.arraycopy(face.uv, i * 2, texCoords, index * 2, 2);
                    }

                    if(n == 4) {
                        indices.add((short)(vIndex + 0));
                        indices.add((short)(vIndex + 2));
                        indices.add((short)(vIndex + 1));
                        indices.add((short)(vIndex + 0));
                        indices.add((short)(vIndex + 3));
                        indices.add((short)(vIndex + 2));
                    }else {
                        indices.add((short)(vIndex + 0));
                        indices.add((short)(vIndex + 2));
                        indices.add((short)(vIndex + 1));
                    }

                    vIndex += n;
                }
            }else {
                numOfVertices = obj.numOfVertices;

                vertices = obj.vertices;
                normals = new float[numOfVertices * 3];
                texCoords = new float[numOfVertices * 2];
                int[] normalsWeight = new int[numOfVertices];
                colors4 = new float[numOfVertices * 4];

                for(MQOFace face : obj.faces) {
                    if(materials[face.mIndex].textureUnit != -1) textured = true;
                    int[] idx = face.vIndex;

                    //外積により法線ベクトルを計算
                    float[] v1 = new float[3];
                    float[] v2 = new float[3];
                    for(int i = 0; i < 3; i++) {
                        v1[i] = obj.vertices[idx[1] * 3 + i] - obj.vertices[idx[0] * 3 + i];
                        v2[i] = obj.vertices[idx[2] * 3 + i] - obj.vertices[idx[0] * 3 + i];
                    }
                    float[] normal = new float[3];
                    Utils.closs(normal, v2, v1);
                    //面を共有する頂点で法線を平均化
                    for(int index : face.vIndex) {
                        for(int i = 0; i < 3; i++) normals[index * 3 + i] = (normal[i] + (normals[index * 3 + i] * normalsWeight[index])) / (normalsWeight[index] + 1);
                        normalsWeight[index]++;
                    }

                    /**
                    System.out.println("vertex");
                    for(int i : idx) {
                        System.out.print(i + ": ");
                        for(int j = 0; j < 3; j++) System.out.print((obj.vertices[i * 3 + j]) + ", ");
                        System.out.println();
                    }
                    System.out.print("vec1: ");
                    for(float f : v1) System.out.print(f + ", ");
                    System.out.println();
                    System.out.print("vec2: ");
                    for(float f : v2) System.out.print(f + ", ");
                    System.out.println();
                    System.out.print("normal: ");
                    for(float f : normal) System.out.print(f + ", ");
                    System.out.println();
                     **/

                    //色の取得
                    float[] color = new float[4];
                    if(face.mIndex >= 0) {
                        MQOMaterial material = materials[face.mIndex];
                        //処理の重さを考慮して、不要な透過処理を回避
                        if(material.color[3] != 1.0f) eColor = 4;
                        color = material.color;
                    }else {
                        System.arraycopy(obj.color, 0, color, 0, 3);
                        color[3] = 1.0f;
                    }
                    for(int i : idx) System.arraycopy(color, 0, colors4, i * 4, 4);

                    for(int i = 0; i < idx.length; i++) System.arraycopy(face.uv, i * 2, texCoords, face.vIndex[i] * 2, 2);

                    if(idx.length == 4) {
                        indices.add((short) idx[0]);
                        indices.add((short) idx[2]);
                        indices.add((short) idx[1]);
                        indices.add((short) idx[0]);
                        indices.add((short) idx[3]);
                        indices.add((short) idx[2]);
                    }else {
                        indices.add((short) idx[0]);
                        indices.add((short) idx[2]);
                        indices.add((short) idx[1]);
                    }
                }
            }

            if(eColor == 3) {
                colors = new float[numOfVertices * 3];
                for(int i = 0; i < numOfVertices; i++) {
                    System.arraycopy(colors4, i * 4, colors, i * 3, 3);
                }
            }else colors = colors4;

            FloatBuffer fbVertices = Utils.buildFloatBuffer(vertices);
            FloatBuffer fbNormals = Utils.buildFloatBuffer(normals);
            FloatBuffer fbColors = Utils.buildFloatBuffer(colors);

            short[] shorts = new short[indices.size()];
            for(int i = 0; i < shorts.length; i++) shorts[i] = indices.pop();
            ShortBuffer sbIndices = Utils.buildShortBuffer(shorts);

            /**
            System.out.println("vertex");
            printFloats(vertices, 3);
            System.out.println("normal");
            printFloats(normals, 3);
            System.out.println("color");
            printFloats(colors, eColor);
            System.out.println("texCoord");
            printFloats(texCoords, 2);
            System.out.println("index");
            printShorts(shorts, 3);
             **/

            if(textured) {
                int textureUnit = materials[obj.faces[0].mIndex].textureUnit;
                FloatBuffer fbTexCoords = Utils.buildFloatBuffer(texCoords);
                TexturedSegment segment = new TexturedSegment(name, fbVertices, fbColors, fbNormals, fbTexCoords, sbIndices, 3, eColor, textureUnit);
                root.add(segment);
            }else {
                SolidSegment segment = new SolidSegment(name, fbVertices, fbColors, fbNormals, sbIndices, 3, eColor);
                root.add(segment);
            }
        }
        return root;
    }
}
