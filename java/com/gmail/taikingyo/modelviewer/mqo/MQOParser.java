package com.gmail.taikingyo.modelviewer.mqo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.gmail.taikingyo.modelviewer.R;
import com.gmail.taikingyo.modelviewer.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;

/**
 * Created by taiking on 2017/09/25.
 */

public class MQOParser {
    private Scanner scanner;
    public MQODoc doc = new MQODoc();
    private LinkedList<String> textures = new LinkedList<String>();
    private Bitmap[] maps;
    private Context context;
    private String path = "";

    public MQOParser(File file, String modelName) {
        doc.name = modelName;
        path = file.getParent();
        try {
            scanner = new Scanner(file);
            parseDoc();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public MQOParser(Context context, String file) {
        this.context = context;
        int index = file.lastIndexOf("/");
        if(index != -1) {
            path = file.substring(0, index + 1);
        }
        doc.name = file.substring(index + 1);
        try {
            scanner = new Scanner(Utils.loadFromAsset(context, file));
            parseDoc();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap[] getTextures() {
        Bitmap[] maps = new Bitmap[textures.size()];
        try {
            for (int i = 0; i < maps.length; i++) {
                maps[i] = BitmapFactory.decodeStream(context.getAssets().open(textures.get(i)));
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        return maps;
    }

    private void parseDoc() {
        String str;

        str = scanner.nextLine();
        if(!str.equalsIgnoreCase("Metasequoia Document")) {
            System.out.println("not MQODocument: " + str);
            return;
        }
        scanner.next();
        str = scanner.next();
        if(!str.equalsIgnoreCase("Text")) {
            System.out.println("not TextFormal: " + str);
            return;
        }
        str = scanner.next();
        if(str.equalsIgnoreCase("Ver")) {
            doc.version = scanner.nextFloat();
        }else {
            System.out.println("nothing version: " + str);
            return;
        }

        do {
            str = scanner.next();
            if(str.equalsIgnoreCase("Scene")) {
                parseScene();
            }else if(str.equalsIgnoreCase("Material")) {
                parseMaterial(scanner.nextInt());
            }else if(str.equalsIgnoreCase("Object")) {
                parseObject();
            }else if(str.equalsIgnoreCase("Thumbnail")) {
                skipEndOfChunk();
            }else {
                System.out.println("chunk: " + str);
            }
        }while(!str.equalsIgnoreCase("EoF"));

        loadTextures();
    }

    private void parseScene() {
        System.out.println("scene");
        skipEndOfChunk();
    }

    private void parseMaterial(int n) {
        MQOMaterial[] material = new MQOMaterial[n];
        String str;
        do {
            str = scanner.next();
        }while(!str.equalsIgnoreCase("{"));

        for(int i = 0; i < n; i++) {
            material[i] = new MQOMaterial();
            String line = scanner.nextLine();
            while(line.equalsIgnoreCase("")) line = scanner.nextLine();
            line = line.trim();
            int index = line.indexOf(" ");
            String name = line.substring(0, index - 1);
            material[i].name = name.replaceAll("\"", "");
            line = line.substring(index).trim();

            index = line.indexOf(")");
            while(index != -1) {
                str = line.substring(0, index + 1);
                line = line.substring(index + 1).trim();

                index = str.indexOf("(");
                String paramName = str.substring(0, index);
                String param = str.substring(index + 1, str.length() - 1);

                if(paramName.equalsIgnoreCase("shader")) {
                    material[i].shader = Integer.parseInt(param);
                }else if(paramName.equalsIgnoreCase("col")) {
                    String[] sColor = param.split(" ");
                    float[] fColor = new float[sColor.length];
                    for(int j = 0; j < sColor.length; j++) fColor[j] = Float.parseFloat(sColor[j]);
                    material[i].color = fColor;
                }else if(paramName.equalsIgnoreCase("dif")) {
                    material[i].dif = Float.parseFloat(param);
                }else if(paramName.equalsIgnoreCase("amb")) {
                    material[i].amb = Float.parseFloat(param);
                }else if(paramName.equalsIgnoreCase("emi")) {
                    material[i].emi = Float.parseFloat(param);
                }else if(paramName.equalsIgnoreCase("spc")) {
                    material[i].spc = Float.parseFloat(param);
                }else if(paramName.equalsIgnoreCase("power")) {
                    material[i].power = Float.parseFloat(param);
                }else if(paramName.equalsIgnoreCase("tex")) {
                    String fileName = param.replaceAll("\"", "");
                    int unit = addTexture(path + fileName);
                    material[i].textureUnit = unit;
                }else {
                    System.out.println(paramName + " is not support");
                }

                index = line.indexOf(")");
            }
        }
        doc.materials = material;

        do {
            str = scanner.next();
        }while(!str.equalsIgnoreCase("}"));
    }

    private void parseObject() {
        MQObject obj = new MQObject();
        String name = scanner.next();
        obj.name = name.replaceAll("\"", "");
        String str;
        do {
            str = scanner.next();
        }while(!str.equalsIgnoreCase("{"));

        str = scanner.next();
        while(!str.equalsIgnoreCase("}")) {
            if(str.equalsIgnoreCase("shading")) {
                obj.shading = scanner.nextInt();
            }else if(str.equalsIgnoreCase("color")) {
                String[] sColor = scanner.nextLine().trim().split(" ");
                float[] fColor = new float[sColor.length];
                for (int i = 0; i < fColor.length; i++) fColor[i] = Float.parseFloat(sColor[i]);
                obj.color = fColor;
            }else if(str.equalsIgnoreCase("vertex")) {
                int numOfVertices = scanner.nextInt();
                obj.numOfVertices = numOfVertices;
                float[] vertices = new float[numOfVertices * 3];

                do {
                    str = scanner.next();
                }while(!str.equalsIgnoreCase("{"));

                for(int i = 0; i < vertices.length; i++) vertices[i] = scanner.nextFloat();

                do {
                    str = scanner.next();
                }while(!str.equalsIgnoreCase("}"));

                obj.vertices = vertices;
            }else if(str.equalsIgnoreCase("face")) {
                int numOfFaces = scanner.nextInt();
                MQOFace[] faces = new MQOFace[numOfFaces];

                do {
                    str = scanner.next();
                }while(!str.equalsIgnoreCase("{"));

                for(int i = 0; i < numOfFaces; i++) {
                    faces[i] = new MQOFace();
                    int numOfIndex = scanner.nextInt();
                    String line =  scanner.nextLine().trim();
                    int index = line.indexOf(")");
                    while(index != -1) {
                        str = line.substring(0, index + 1);
                        line = line.substring(index + 1).trim();

                        if(str.startsWith("V")) {
                            int[] indices = new int[numOfIndex];
                            str = str.substring(2, str.length() - 1);
                            String[] sIndices = str.split(" ");
                            for(int j = 0; j < indices.length; j++) indices[j] = Integer.parseInt(sIndices[j]);
                            faces[i].vIndex = indices;
                        }else if(str.startsWith("M")) {
                            str = str.substring(2, str.length() - 1);
                            faces[i].mIndex = Integer.parseInt(str);
                        }else if(str.startsWith("UV")) {
                            float[] uvs = new float[numOfIndex * 2];
                            str = str.substring(3, str.length() - 1);
                            String[] sUvs = str.split(" ");
                            for(int j = 0; j < uvs.length; j++) uvs[j] = Float.parseFloat(sUvs[j]);
                            faces[i].uv = uvs;
                        }
                        index = line.indexOf(")");
                    }
                }

                do {
                    str = scanner.next();
                }while(!str.equalsIgnoreCase("}"));

                obj.faces = faces;
                skipEndOfChunk();
            }else if(str.equalsIgnoreCase("BVertex")) {
                skipEndOfChunk();
            }else {
                scanner.nextLine();
            }

            str = scanner.next();
        }

        doc.objects.add(obj);
    }

    private int addTexture(String str) {
        int i = textures.indexOf(str);
        if(i == -1) {
            textures.add(str);
            i = textures.size() - 1;
            System.out.println("texture " + i + ": " + str);
        }
        return i;
    }

    private void loadTextures() {
        Bitmap[] maps = new Bitmap[textures.size()];
        try {
            for (int i = 0; i < maps.length; i++) {
                maps[i] = BitmapFactory.decodeStream(context.getAssets().open(textures.get(i)));
            }
        }catch(Exception e) {
            e.printStackTrace();
        }
        doc.textures = maps;
    }

    private void skipEndOfChunk() {
        int q = 0;
        String line = scanner.nextLine();
        if(line.endsWith("{")) q++;
        while(q != 0) {
            line = scanner.nextLine();
            if(line.endsWith("{")) {
                q++;
            }else if(line.endsWith("}")) q--;
        }
    }
}
