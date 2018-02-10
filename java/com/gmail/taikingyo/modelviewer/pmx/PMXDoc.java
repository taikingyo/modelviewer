package com.gmail.taikingyo.modelviewer.pmx;

import android.graphics.Bitmap;

import com.gmail.taikingyo.modelviewer.Group;
import com.gmail.taikingyo.modelviewer.SolidSegment;
import com.gmail.taikingyo.modelviewer.TexturedSegment;
import com.gmail.taikingyo.modelviewer.Utils;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by taiking on 2017/10/17.
 */

public class PMXDoc {
    public String name;
    public String comment;
    public float[] positions;
    public float[] normals;
    public float[] uvs;
    public int[] indices;
    public Bitmap[] textures;
    public PMXMaterial[] materials;

    // 簡易版
    public Group buildNode() {
        Group root = new Group(name);
        int numOfMaterials = materials.length;
        int index = 0;

        for(int i = 0; i < numOfMaterials; i++) {
            String name = materials[i].name_JP;
            int length = materials[i].index;
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for(int j = index; j < index + length; j++) {
                min = Math.min(min, indices[j]);
                max = Math.max(max, indices[j]);
            }

            short[] sIndices = new short[length];
            for(int j = 0; j < length; j++) {
                sIndices[j] = (short)(indices[j + index] - min);
            }

            int numOfVertices = max - min + 1;

            if(materials[i].textureIndex != -1) {
                float[] mPositions = new float[numOfVertices * 3];
                float[] mNormals = new float[numOfVertices * 3];
                float[] mColors = new float[numOfVertices * 3];
                float[] mUvs = new float[numOfVertices * 2];

                System.arraycopy(positions, min * 3, mPositions, 0, mPositions.length);
                System.arraycopy(normals, min * 3, mNormals, 0, mNormals.length);
                System.arraycopy(uvs, min * 2, mUvs, 0, mUvs.length);
                Arrays.fill(mColors, 1.0f);

                FloatBuffer posBuffer = Utils.buildFloatBuffer(mPositions);
                FloatBuffer colBuffer = Utils.buildFloatBuffer(mColors);
                FloatBuffer norBuffer = Utils.buildFloatBuffer(mNormals);
                FloatBuffer uvBuffer = Utils.buildFloatBuffer(mUvs);
                ShortBuffer iBuffer = Utils.buildShortBuffer(sIndices);
                // SolidSegment segment = new SolidSegment(name, posBuffer, colBuffer, norBuffer, iBuffer, 3, 3);
                TexturedSegment segment = new TexturedSegment(name, posBuffer, colBuffer, norBuffer, uvBuffer, iBuffer, 3, 3, materials[i].textureIndex);
                root.add(segment);

            }else {
                float[] mPositions = new float[numOfVertices * 3];
                float[] mNormals = new float[numOfVertices * 3];
                float[] mColors = new float[numOfVertices * 3];

                System.arraycopy(positions, min * 3, mPositions, 0, mPositions.length);
                System.arraycopy(normals, min * 3, mNormals, 0, mNormals.length);
                Arrays.fill(mColors, 1.0f);

                FloatBuffer posBuffer = Utils.buildFloatBuffer(mPositions);
                FloatBuffer colBuffer = Utils.buildFloatBuffer(mColors);
                FloatBuffer norBuffer = Utils.buildFloatBuffer(mNormals);
                ShortBuffer iBuffer = Utils.buildShortBuffer(sIndices);
                SolidSegment segment = new SolidSegment(name, posBuffer, colBuffer, norBuffer, iBuffer, 3, 3);
                root.add(segment);
            }

            index += materials[i].index;
        }
        return root;
    }

    // 最適化判（未完成）
    public Group _buildNode() {
        Group root = new Group(name);
        int index = 0;
        int numOfMaterials = materials.length;

        //マテリアル毎に頂点情報を分割変換
        for(int i = 0; i < numOfMaterials; i++) {
            String name = materials[i].name_JP;
            int length = materials[i].index;
            int[] materialIndices = new int[length];
            System.arraycopy(indices, index, materialIndices, 0, length);
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for(int j : materialIndices) {
                min = Math.min(j, min);
                max = Math.max(j, max);
            }
            int[] transTable = new int[max - min + 1];  //頂点インデックスの辞書式変換テーブル
            Arrays.fill(transTable, -1);

            if(materials[i].textureIndex != -1) {
                LinkedList<Float> pos = new LinkedList<Float>();
                LinkedList<Float> nor = new LinkedList<Float>();
                LinkedList<Float> uv = new LinkedList<Float>();

                int listPos = 0;
                for(int j : materialIndices) {
                    if(transTable[j - min] == -1) {
                        transTable[j - min] = listPos;
                        pos.add(positions[j * 3 + 0]);
                        pos.add(positions[j * 3 + 1]);
                        pos.add(positions[j * 3 + 2]);
                        nor.add(normals[j * 3 + 0]);
                        nor.add(normals[j * 3 + 1]);
                        nor.add(normals[j * 3 + 2]);
                        uv.add(uvs[j * 2 + 0]);
                        uv.add(uvs[j * 2 + 1]);
                        listPos++;
                    }
                }

                short[] sIndices = new short[length];
                for(int j = 0; j < length; j++) {
                    sIndices[j] = (short)transTable[materialIndices[j] - min];
                }

                float[] colors = new float[pos.size()];
                Arrays.fill(colors, 1.0f);
                TexturedSegment segment = new TexturedSegment(name, list2buffer(pos), Utils.buildFloatBuffer(colors), list2buffer(nor), list2buffer(uv), Utils.buildShortBuffer(sIndices), 3, 3, materials[i].textureIndex);
                root.add(segment);
            }else {
                LinkedList<Float> pos = new LinkedList<Float>();
                LinkedList<Float> nor = new LinkedList<Float>();

                int listPos = 0;
                for(int j : materialIndices) {
                    if(transTable[j - min] == -1) {
                        transTable[j - min] = listPos;
                        pos.add(positions[j * 3 + 0]);
                        pos.add(positions[j * 3 + 1]);
                        pos.add(positions[j * 3 + 2]);
                        nor.add(normals[j * 3 + 0]);
                        nor.add(normals[j * 3 + 1]);
                        nor.add(normals[j * 3 + 2]);
                        listPos++;
                    }
                }

                short[] sIndices = new short[length];
                for(int j = 0; j < length; j++) {
                    sIndices[j] = (short)transTable[materialIndices[j] - min];
                }

                float[] colors = new float[pos.size()];
                Arrays.fill(colors, 1.0f);
                SolidSegment segment = new SolidSegment(name, list2buffer(pos), Utils.buildFloatBuffer(colors), list2buffer(nor), Utils.buildShortBuffer(sIndices), 3, 3);
                root.add(segment);
            }

            index += materials[i].index;
        }
        return root;
    }

    public void printFaces() {
        System.out.print("faces");
        for(int i = 0; i < indices.length; i++) {
            if(i % 3 == 0) {
                System.out.print("\n" + i / 3 + ":");
            }
            for(int j = 0; j < 3; j++) System.out.printf(" %4.2f, ", positions[indices[i] * 3 + j]);
            System.out.println();
        }
    }

    private void printVertex(LinkedList<Float> pos, LinkedList<Float> nor, short[] sIndices, int n) {
        int size = Math.min(sIndices.length / 3, n);

        for(int i = 0; i < size; i++) {
            int[] idx = new int[3];
            idx[0] = sIndices[i * 3 + 0];
            idx[1] = sIndices[i * 3 + 1];
            idx[2] = sIndices[i * 3 + 2];
            System.out.print(i + ":" + idx[0] + ", " + idx[1] + ", " + idx[2] + " pos: ");
            for(int j : idx) System.out.print(pos.get(j * 3 + 0) + ", " + pos.get(j * 3 + 1) + ", " + pos.get(j * 3 + 2) + ", ");
            System.out.print(" nor: ");
            for(int j : idx) System.out.print(nor.get(j * 3 + 0) + ", " + nor.get(j * 3 + 1) + ", " + nor.get(j * 3 + 2) + ", ");
            System.out.println();
        }
    }

    private static FloatBuffer list2buffer(LinkedList<Float> list) {
        float[] f = new float[list.size()];
        for(int i = 0; i < list.size(); i++) f[i] = list.pollFirst();

        return Utils.buildFloatBuffer(f);
    }
}
