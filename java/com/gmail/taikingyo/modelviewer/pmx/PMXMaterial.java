package com.gmail.taikingyo.modelviewer.pmx;

/**
 * Created by taiking on 2017/10/22.
 */

public class PMXMaterial {
    public String name_JP;
    public String name_EN;
    public String memo;
    public float[] diffuse = new float[4];	//R,G,B,A
    public float[] speculer = new float[4];	//R,G,B,係数
    public float[] ambient = new float[3];	//R,G,B
    public float[] edge = new float[5];		//R,G,B,A,サイズ
    public int textureIndex;
    public int sphereIndex;
    public int index;
    public byte flag;

    public void printMaterial() {
        System.out.println(name_JP + "(" + name_EN + ")");
        System.out.println("index: " + index);
        System.out.print("diffuse: ");
        for(float f: diffuse) System.out.printf(" %.02f", f);
        System.out.println();
        System.out.print("speculer: ");
        for(float f: speculer) System.out.printf(" %.02f", f);
        System.out.println();
        System.out.print("ambient: ");
        for(float f: ambient) System.out.printf(" %.02f", f);
        System.out.println();
        System.out.print("edge: ");
        for(float f: edge) System.out.printf(" %.02f", f);
        System.out.println();
        System.out.println("texture: " + textureIndex);
        System.out.println("sphere: " + sphereIndex);
        System.out.println("memo");
        System.out.println(memo);
    }
}
