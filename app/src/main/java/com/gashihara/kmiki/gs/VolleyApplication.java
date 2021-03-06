//Vollyの通信クラスです。毎回このまま使います。
//packageはこのクラスの階層を示します。このクラスの場合はcom.gashfara.mogi.gsapp.VolleyApplicationになります。

package com.gashihara.kmiki.gs;

//importはすでに用意されているAndroidSDKのクラスなどを取り込んで使用するための宣言です。
//プログラムでpackage以外のクラスを使用するとエラーになるのでそのクラスごとに必要に応じて追加すればOKです。

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.kii.cloud.storage.Kii;

//Applicationクラスを継承extend（コピーみたいなもの）しています。Applicationの機能がそのまま使えます。{}までがクラスです。
//これはクラスの定義です。このクラスを使うにはnewなどをしてインスタンス化（実態を作る）してから使います。
//VolleyApplicationが新しいクラス名になります。
//publicが付いていると他のクラスから使えます。privateだと使えません。
public class VolleyApplication extends Application {
    //クラスはこのようにintのような型として使えます。
    private static VolleyApplication sInstance;

    private RequestQueue mRequestQueue;
    //overrideは継承元のクラスApplicationの機能を引き継ぐのではなく上書きすることを宣言しています。
    //onCreateはアプリを起動した時にOSから呼び出される関数です。よく使います。
    @Override
    public void onCreate() {
        //superは親クラスです。ですのでApplicationです。このonCreate()関数を実行しています。overrideしていますが、親のonCreate()も実行しているわけです。
        //クラス内の関数（メソッド）はクラス名.関数名()というかんじに記載します。
        super.onCreate();
        //お約束的な書き方。親のonCreateを実行している（全部Overrideしちゃうと何かあったときこわいので）

        //Volleyの通信用のクラスを初期化してキューを準備。Volleyは初期化されたとき作られるオブジェクト
        mRequestQueue = Volley.newRequestQueue(this);
        //自分自身のインスタンス（newなどでクラスを実体化したもの）を代入しています。
        sInstance = this;
        //Userで追加ここから
        //KiiCloudの初期化。Applicationクラスで実行してください。キーは自分の値にかえる。
        Kii.initialize(getApplicationContext(), "edeae4ad", "4a4001a7f3f6db03c118475cf236a75a", Kii.Site.JP);
        //Userで追加ここまで
    }
    //インスタンスを返す関数（メソッドです）。クラスの中にある変数はこのように関数を通じて返すようにするのが一般的です。
    //synchronizedは同時に動作すると不具合が起きるときに宣言します。Volleyの仕様です。
    public synchronized static VolleyApplication getInstance() {
        return sInstance;
    }

    //通信クラスを返す関数
    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }
}
