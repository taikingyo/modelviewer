package com.gmail.taikingyo.modelviewer;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by taiking on 2017/09/21.
 */

public class SolidSegment extends SimpleSegment {
    private int iColors;
    private int eColor;

    private FloatBuffer colors;

    public SolidSegment(String name, FloatBuffer positions, FloatBuffer colors, FloatBuffer normals, ShortBuffer indices, int ePosition, int eColor) {
        super(name, positions, normals, indices, ePosition);

        this.colors = colors;
        this.eColor = eColor;
    }

    @Override
    public void init() {
        iColors = initBuffer(colors);
        super.init();
    }

    //カラーバッファのインデックスを取得
    public int getColors() {
        return iColors;
    }

    //カラーの要素数を取得
    public int elementsOfColor() {
        return eColor;
    }
}
