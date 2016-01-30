//1つのセルにあるデータを保存するためのデータクラスです。
package com.gashihara.kmiki.gs;

import java.text.SimpleDateFormat;

public class MessageRecord {
    //保存するデータ全てを変数で定義します。
    private String imageUrl;
    private String comment;
    private String id;
    private Long created;
    private double lat;
    private double lon;

    //データを１つ作成する関数です。項目が増えたら増やしましょう。
    //MessageRecord=クラスメイト同じ名前の関数=newしたときに動くConstructor
    public MessageRecord(String id, String imageUrl, String comment, Long created, Double lat,Double lon) {
        this.imageUrl = imageUrl;
        this.comment = comment;
        this.id = id;
        this.created = created;
        this.lat = lat;
        this.lon = lon;
    }
    //それぞれの項目を返す関数です。項目が増えたら増やしましょう。
    public String getComment() {
        return comment;
    }
    public String getImageUrl() {return imageUrl;}
    public String getId() {
        return id;
    }

    //kmiki epoch形式を日付型に変換
    public String getCreated() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        return sdf.format(created).toString();
    }
    public double getLat() {return lat;}
    public double getLon() {return lon;}
}
