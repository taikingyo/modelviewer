package com.gmail.taikingyo.modelviewer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Created by taiking on 2017/10/22.
 */

public class TGAImage {
    private int x, y, width, height;
    private int size;
    private byte[] data;

    public TGAImage(String path) {
        try {
            BufferedInputStream bi = new BufferedInputStream(new FileInputStream(path));
            readImage(bi);
            bi.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public TGAImage(File file) {
        try {
            BufferedInputStream bi = new BufferedInputStream(new FileInputStream(file));
            readImage(bi);
            bi.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public TGAImage(InputStream in) {
        try {
            BufferedInputStream bi = new BufferedInputStream(in);
            readImage(bi);
            bi.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    public Bitmap getBitmap() {
        byte[] argb = new byte[data.length / size * 4];
        for(int i = 0; i < width * height; i++) {
            argb[i * 4] = (byte)255;
            argb[i * 4 + 1] = data[i * 3 + 0];
            argb[i * 4 + 2] = data[i * 3 + 1];
            argb[i * 4 + 3] = data[i * 3 + 2];
        }
        int[] iData = new int[width * height];

        for(int i = 0; i < width * height; i++) {
            iData[i] = ByteBuffer.wrap(argb, i * 4, 4).getInt();
        }

        return Bitmap.createBitmap(iData, width, height, Bitmap.Config.ARGB_8888);
    }

    private void readImage(BufferedInputStream bi) {
        try {
            //ヘッダー
            int idLength = bi.read();
            int colormap = bi.read();
            int dataFormat = bi.read();
            int colormapPosition = readIntOf(bi, 2);
            int colormapLength = readIntOf(bi, 2);
            int colormapEntry = bi.read();
            x = readIntOf(bi, 2);
            y = readIntOf(bi, 2);
            width = readIntOf(bi, 2);
            height = readIntOf(bi, 2);
            int pixelBits = bi.read();
            int attribute = bi.read();

            //ID
            byte[] id = new byte[idLength];
            bi.read(id);

            //データ
            if(dataFormat == 2) {
                size = pixelBits / 8;
                data = new byte[width * height * size];
                bi.read(data);
                bi.close();
                data = bgr2rgb(data);
                data = invertY(data, width * size);
            }

            //System.out.println("dataformat: " + dataFormat + " x=" + x + " y=" + y + " width=" + width + " height=" + height + " pixel: " + pixelBits);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static byte[] bgr2rgb(byte[] src) {
        byte[] dest = src;

        for(int i = 0; i < dest.length; i += 3) {
            byte temp = dest[i];
            dest[i] = dest[i + 2];
            dest[i + 2] = temp;
        }

        return dest;
    }

    private static byte[] invertY(byte[] src, int scanlineStride) {
        byte[] dest = new byte[src.length];
        int row = src.length / scanlineStride;
        for(int i = 0; i < row; i++) {
            System.arraycopy(src, i * scanlineStride, dest, (row - 1 - i) * scanlineStride, scanlineStride);
        }

        return dest;
    }

    private static int readIntOf(BufferedInputStream bi, int size) {
        return readIntOf(bi, size, 1)[0];
    }

    private static int[] readIntOf(BufferedInputStream bi, int size, int num) {
        byte[] src = new byte[size * num];
        int[] dest = new int[num];

        try {
            bi.read(src);

            if(size == 1) {
                for(int i = 0; i < src.length; i++) dest[i] = src[i];
            }else {
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(src.length);
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                byteBuffer.put(src);
                byteBuffer.position(0);

                if(size == 2) {
                    ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
                    short[] s = new short[src.length / 2];
                    shortBuffer.get(s);
                    for(int i = 0; i < s.length; i++) dest[i] = s[i];
                }else if(size == 4) {
                    IntBuffer intBuffer = byteBuffer.asIntBuffer();
                    intBuffer.get(dest);
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return dest;
    }
}
