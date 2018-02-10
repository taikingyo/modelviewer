package com.gmail.taikingyo.modelviewer;

/**
 * Created by taiking on 2017/09/28.
 */

public interface Node {
    //名前を取得
    public String getName();

    //ローカル座標を取得
    public float[] getLocal();

    //ワールド座標を取得
    public float[] getWorld();

    //ローカル座標で移動
    public void translate(float x, float y, float z);

    //ローカル座標で回転
    public void rotate(float a, float x, float y, float z);

    //ローカル座標に変換行列を掛けてワールド座標を計算
    public void toWorld(float[] matrix);

    //初期化（バッファの準備等）
    public void init();
}
