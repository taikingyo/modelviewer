package com.gmail.taikingyo.modelviewer;

import android.graphics.Bitmap;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Created by taiking on 2017/10/07.
 */

public class TexturedSegment extends SolidSegment {
    private int iTexCoords;
    private FloatBuffer texCoords;
    private int textureUnit;

    public TexturedSegment(String name, FloatBuffer positions, FloatBuffer colors, FloatBuffer normals, FloatBuffer texCoords, ShortBuffer indices, int ePosition, int eColor, int textureUnit) {
        super(name, positions, colors, normals, indices, ePosition, eColor);
        this.texCoords = texCoords;
        this.textureUnit = textureUnit;
    }

    @Override
    public void init() {
        iTexCoords = initBuffer(texCoords);
        super.init();
    }

    public int getTexCoords() {
        return iTexCoords;
    }

    public int getTextureUnit() {
        return textureUnit;
    }
}
