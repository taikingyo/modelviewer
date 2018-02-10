package com.gmail.taikingyo.modelviewer.pmx;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gmail.taikingyo.modelviewer.TGAImage;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * Created by taiking on 2017/10/17.
 */

public class PMXParser {
    private static final String UTF_16LE = "UTF-16LE";
    private static final String UTF_8 = "UTF-8";

    private Context context;
    private String path = "";
    private CharsetDecoder decoder;
    private byte[] attribute;

    public PMXDoc doc;

    public PMXParser(Context context, String file) {
        this.context = context;
        doc = new PMXDoc();

        try {
            BufferedInputStream bis = new BufferedInputStream(context.getAssets().open(file));
            int index = file.lastIndexOf("/");
            if(index != -1) {
                path = file.substring(0, index + 1);
            }
            attribute = readHeader(bis);
            if(attribute[0] == 0) {
                decoder = Charset.forName(UTF_16LE).newDecoder();
            }else decoder = Charset.forName(UTF_8).newDecoder();
            readModelInfo(bis);
            readData(bis);
            bis.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Nullable
    private byte[] readHeader(BufferedInputStream bis) {
        try {
            byte[] b4 = new byte[4];
            bis.read(b4);
            String pmx = new String(b4);
            bis.read(b4);
            float[] ver = new float[1];
            byte2float(b4).get(ver);
            if((pmx.equals("PMX ") && (ver[0] == 2.0f || ver[0] == 2.1f))) {
                int n = bis.read();
                byte[] attr = new byte[n];
                bis.read(attr);
                return attr;
            }else {
                System.out.println("file format error: " + pmx + "ver: " + ver[0]);
                return null;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private void readModelInfo(BufferedInputStream bis) {
        try {
            byte[] b4 = new byte[4];
            String [] info = new String[4];

            for(int i = 0; i < 4; i++) {
                bis.read(b4);
                int n = byte2int(b4, 4)[0];
                byte[] data = new byte[n];
                bis.read(data);
                info[i] = decodeString(data);
            }
            doc.name = info[0];
            doc.comment = info[2];
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void readData(BufferedInputStream bis) {
        byte[] b1 = new byte[1];
        byte[] b4 = new byte[4];
        int size = 3 + 3 + 2 + (4 * attribute[1]);	//座標3、法線3、UV2　＋　追加UV
        try {
            bis.read(b4);
            int numOfVertices = byte2int(b4, 4)[0];
            float[] fv = new float[size];
            float[] positions = new float[numOfVertices * 3];
            float[] normals = new float[numOfVertices * 3];
            float[] uvs = new float[numOfVertices * 2];

            for(int i = 0; i < numOfVertices; i++) {
                byte[] vertexData = new byte[size * 4];
                bis.read(vertexData);
                byte2float(vertexData).get(fv);

                System.arraycopy(fv, 0, positions, i * 3, 3);
                System.arraycopy(fv, 3, normals, i * 3, 3);
                System.arraycopy(fv, 6, uvs, i * 2, 2);

                bis.read(b1);
                //ウェイト情報の取得
                if(b1[0] == 0) {
                    byte[] weight = new byte[attribute[5]];
                    bis.read(weight);
                }else if(b1[0] == 1) {
                    byte[] weight = new byte[(attribute[5] * 2) + 4];
                    bis.read(weight);
                }else if(b1[0] == 2) {
                    byte[] weight = new byte[(attribute[5] * 4) + 16];
                    bis.read(weight);
                }else if(b1[0] == 3) {
                    byte[] weight = new byte[(attribute[5] * 2) + 40];
                    bis.read(weight);
                }else {
                    System.out.println();
                    System.out.println("unknown bone type");
                    break;
                }

                //エッジ倍率の取得
                bis.read(b4);
                float[] edge = new float[1];
                byte2float(b4).get(edge);
            }

            doc.positions = positions;
            doc.normals = normals;
            doc.uvs = uvs;

            //面情報の取得
            bis.read(b4);
            int numOfIndices = byte2int(b4, 4)[0];
            byte[] indexData = new byte[numOfIndices * attribute[2]];
            bis.read(indexData);
            int[] indices = byte2int(indexData, attribute[2]);
            doc.indices = indices;

            //テクスチャ情報の取得
            bis.read(b4);
            int numOfTextures = byte2int(b4, 4)[0];
            Bitmap[] textures = new Bitmap[numOfTextures];
            for(int i = 0; i < numOfTextures; i++) {
                bis.read(b4);
                int length = byte2int(b4, 4)[0];
                byte[] textureFile = new byte[length];
                bis.read(textureFile);
                String texture = path + decodeString(textureFile);
                StringBuffer temp = new StringBuffer();
                int index = texture.indexOf("\\");
                while(index != -1) {
                    temp.append(texture.substring(0, index) + File.separator);
                    texture = texture.substring(index + 1);
                    index = texture.indexOf("\\");
                }
                temp.append(texture);
                texture = temp.toString();

                try {
                    InputStream in = context.getAssets().open(texture);
                    if(texture.endsWith(".tga")) {
                        TGAImage image = new TGAImage(in);
                        textures[i] = image.getBitmap();
                    }else textures[i] = BitmapFactory.decodeStream(in);
                    if(textures[i] == null) {
                        System.out.println(texture + " set dummy");
                        textures[i] = BitmapFactory.decodeStream(context.getAssets().open("dummy.png"));
                    }
                }catch(FileNotFoundException e) {
                    System.out.println(texture + " set dummy");
                    textures[i] = BitmapFactory.decodeStream(context.getAssets().open("dummy.png"));
                }


            }
            doc.textures = textures;

            //材質情報の取得
            bis.read(b4);
            int numOfMaterials = byte2int(b4, 4)[0];
            PMXMaterial[] materials = new PMXMaterial[numOfMaterials];
            int index = 0;
            for(int i = 0; i < numOfMaterials; i++) {
                PMXMaterial material = new PMXMaterial();
                bis.read(b4);
                int length = byte2int(b4, 4)[0];
                byte[] materialName_JP = new byte[length];
                bis.read(materialName_JP);
                String name_JP = decodeString(materialName_JP);
                bis.read(b4);
                length = byte2int(b4, 4)[0];
                byte[] materialName_EN = new byte[length];
                bis.read(materialName_EN);
                String name_EN = decodeString(materialName_EN);

                float[] lights = new float[11];		//diffuse 4 + speculer 4 + ambient 3
                byte[] data = new byte[lights.length * 4];
                bis.read(data);
                byte2float(data).get(lights);

                bis.read(b1);
                byte flag = b1[0];

                float[] edge = new float[5];
                data = new byte[20];	//edge r,g,b,a,size
                bis.read(data);
                byte2float(data).get(edge);

                data = new byte[attribute[3]];
                bis.read(data);
                int textureIndex = byte2int(data, attribute[3])[0];
                bis.read(data);
                int sphereIndex = byte2int(data, attribute[3])[0];
                int sphereMode = bis.read();

                int toon;
                int toonFlag = bis.read();
                if(toonFlag == 0) {
                    data = new byte[attribute[3]];
                    bis.read(data);
                    toon = byte2int(data, attribute[3])[0];
                }else toon = bis.read();

                bis.read(b4);
                length = byte2int(b4, 4)[0];
                data = new byte[length];
                bis.read(data);
                String memo = decodeString(data);

                bis.read(b4);
                int face = byte2int(b4, 4)[0];
                index += face;

                material.name_JP = name_JP;
                material.name_EN = name_EN;
                material.memo = memo;
                System.arraycopy(lights, 0, material.diffuse, 0, 4);
                System.arraycopy(lights, 4, material.speculer, 0, 4);
                System.arraycopy(lights, 8, material.ambient, 0, 3);
                material.edge = edge;
                material.textureIndex = textureIndex;
                material.sphereIndex = sphereIndex;
                material.index = face;
                material.flag = flag;

                //material.printMaterial();
                materials[i] = material;
            }
            doc.materials = materials;

            //ボーン情報の取得
            bis.read(b4);
            int numOfBone = byte2int(b4, 4)[0];
            System.out.println("bone " + numOfBone);
            for(int i = 0; i < 0; i++) {
                bis.read(b4);
                int length = byte2int(b4, 4)[0];
                byte[] boneName_JP = new byte[length];
                String name_JP = decodeString(boneName_JP);
                bis.read(b4);
                length = byte2int(b4, 4)[0];
                byte[] boneName_EN = new byte[length];
                String name_EN = decodeString(boneName_EN);

                System.out.println(name_JP + "(" + name_EN + ")");
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Nullable
    private String decodeString(byte[] b) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(b.length);
        byteBuffer.put(b);
        byteBuffer.position(0);
        try {
            CharBuffer charBuffer = decoder.decode(byteBuffer);
            char[] str = new char[b.length / 2];
            charBuffer.get(str);
            return new String(str);
        } catch (CharacterCodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    private static int[] byte2int(byte[] b, int size) {
        if(size == 1) {
            int[] i = new int[b.length];
            for(int j = 0; j < b.length; j++) i[j] = b[j];
            return i;
        }else {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(b.length);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.put(b);
            byteBuffer.position(0);

            if(size == 2) {
                ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
                short[] s = new short[b.length / 2];
                shortBuffer.get(s);
                int[] i = new int[s.length];
                for(int j = 0; j < s.length; j++) i[j] = s[j];
                return i;
            }else if(size == 4) {
                IntBuffer intBuffer = byteBuffer.asIntBuffer();
                int[] i = new int[b.length / 4];
                intBuffer.get(i);
                return i;
            }else return null;
        }
    }

    @NonNull
    private static FloatBuffer byte2float(byte[] b) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(b.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(b);
        byteBuffer.position(0);
        return byteBuffer.asFloatBuffer();
    }
}
