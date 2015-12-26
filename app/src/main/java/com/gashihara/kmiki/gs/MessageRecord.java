//1つのセルにあるデータを保存するためのデータクラスです。
package com.gashihara.kmiki.gs;

public class MessageRecord {
    //保存するデータ全てを変数で定義します。
    private String imageUrl;
    private String comment;
    private String pdate;

    //データを１つ作成する関数です。項目が増えたら増やしましょう。
    //MessageRecord=クラスメイト同じ名前の関数=newしたときに動くConstructor
    public MessageRecord(String imageUrl, String comment, String pdate) {
        this.imageUrl = imageUrl;
        this.comment = comment;
        this.pdate = pdate;
    }
    //それぞれの項目を返す関数です。項目が増えたら増やしましょう。
    public String getComment() {
        return comment;
    }
    public String getImageUrl() {return imageUrl;}
    public String getPdate() {return pdate;}
}
