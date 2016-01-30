//ListViewに１つのセルの情報(message_item.xmlとMessageRecord)を結びつけるためのクラス
package com.gashihara.kmiki.gs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.callback.KiiObjectCallBack;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//<MessageRecord>はデータクラスMessageRecordのArrayAdapterであることを示している。このアダプターで管理したいデータクラスを記述されば良い。
public class MessageRecordsAdapter extends ArrayAdapter<MessageRecord> {
    private ImageLoader mImageLoader;


    //アダプターを作成する関数。コンストラクター。クラス名と同じです。
    public MessageRecordsAdapter(Context context) {
        //message_itemのViewを親クラスに設定している
        super(context, R.layout.message_item);
        //キャッシュメモリを確保して画像を取得するクラスを作成。これを使って画像をダウンロードする。Volleyの機能
        mImageLoader = new ImageLoader(VolleyApplication.getInstance().getRequestQueue(), new BitmapLruCache());
    }
    //表示するViewを返します。これがListVewの１つのセルとして表示されます。表示されるたびに実行されます。
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        //convertViewをチェックし、Viewがないときは新しくViewを作成します。convertViewがセットされている時は未使用なのでそのまま再利用します。メモリーに優しい。
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.message_item, parent, false);
        }

        //レイアウトにある画像と文字のViewを所得します。
        NetworkImageView imageView = (NetworkImageView) convertView.findViewById(R.id.image1);
        TextView textView = (TextView) convertView.findViewById(R.id.text1);
        //kmiki追加,1.23
        TextView created = (TextView) convertView.findViewById(R.id.created);
        //TextView location = (TextView) convertView.findViewById(R.id.location);
        ImageButton location = (ImageButton) convertView.findViewById(R.id.location);


        //webリンクを制御するプログラムはここから
        textView.setOnTouchListener(new ViewGroup.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //タップしたのはTextViewなのでキャストする
                TextView textView = (TextView) v;
                String message = textView.getText().toString();
                // SpannableString の取得
                SpannableString ss2 = new SpannableString(message);

                final Pattern STANDARD_URL_MATCH_PATTERN = Pattern.compile("(http://|https://){1}[\\w\\.\\-/:\\#\\?\\=\\&\\;\\%\\~\\+]+", Pattern.CASE_INSENSITIVE);
                final Matcher m = STANDARD_URL_MATCH_PATTERN.matcher(message);
                while(m.find()) {
                    final String t = m.group();
                    ss2.setSpan(new URLSpan(t) {
                        @Override
                        public void onClick(View textView) {

                            Uri uri = Uri.parse(t);
                            Intent intent = new Intent(textView.getContext(), WebActivity.class);
                            intent.putExtra("url",uri.toString());
                            textView.getContext().startActivity(intent);
                        }
                    }, m.start(), m.end(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                }

                textView.setText(ss2);
                //SpannableString使うにはMovementMethod を登録しないといけないらしい
                textView.setMovementMethod(LinkMovementMethod.getInstance());

                return false;
            }
        });
        //webリンクを制御するプログラムはここまで

        //表示するセルの位置からデータをMessageRecordのデータを取得します。
        MessageRecord imageRecord = getItem(position);

        //mImageLoaderを使って画像をダウンロードし、Viewにセットします。
        imageView.setImageUrl(imageRecord.getImageUrl(), mImageLoader);
        //Viewに文字をセットします。
        textView.setText(imageRecord.getComment());
        //kmiki追加
        created.setText(imageRecord.getCreated());
        String lat = String.valueOf(imageRecord.getLat());
        String lon = String.valueOf(imageRecord.getLon());

        if (lat != "0.0" || lon !="0.0"){
            location.setVisibility(View.VISIBLE);
            final String gcode = "geo:"+lat+","+lon;
            location.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(gcode));
//                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=Osaka"));
                    v.getContext().startActivity(intent);
                }
            });
        }



        //Goodで追加ここから　
        //いいねボタンを得る
        Button buttonView = (Button) convertView.findViewById(R.id.button1);
        //ボタンの文字にいいねの数を追加します。
        buttonView.setText(getContext().getString(R.string.good)+":"+imageRecord.getGoodCount());

        //ボタンを押した時のクリックイベントを定義
        buttonView.setOnClickListener(new View.OnClickListener() {
            //クリックした時
            @Override
            public void onClick(View view) {
                //いいねボタンを得る
                Button buttonView = (Button) view;
                ////タグからどの位置のボタンかを得る
                //int position = (Integer)buttonView.getTag();
                //MessageRecordsAdapterの位置からMessageRecordのデータを得る
                MessageRecord messageRecord =  getItem(position);
                //messagesのバケット名と_idの値からKiiObjectのuri(データの場所)を得る。参考：http://documentation.kii.com/ja/starts/cloudsdk/cloudoverview/idanduri/
                Uri objUri = Uri.parse("kiicloud://buckets/" + "messages" + "/objects/" + messageRecord.getId());
                //uriから空のデータを作成
                KiiObject object = KiiObject.createByUri(objUri);
                //いいねを＋１する。
                object.set("goodCount", messageRecord.getGoodCount()+ 1);
                //既存の他のデータ(_id,comment,imageUrlなど)はそのままに、goodCountだけが更新される。参考：http://documentation.kii.com/ja/guides/android/managing-data/object-storages/updating/#full_update
                object.save(new KiiObjectCallBack() {
                    //KiiCloudの更新が完了した時
                    @Override
                    public void onSaveCompleted(int token, KiiObject object, Exception exception) {
                        if (exception != null) {
                            //エラーの時
                            return;
                        }
                        //MessageRecordsAdapterの位置からMessageRecordのデータを得る
                        MessageRecord messageRecord =  getItem(position);
                        //messageRecordのいいねの数を+1する。これでKiiCloudの値とListViewのデータが一致する。
                        messageRecord.setGoodCount(messageRecord.getGoodCount()+1);
                        //データの変更を通知します。
                        notifyDataSetChanged();
                        //トーストを表示.Activityのコンテキストが必要なのでgetContext()してる。
                        Toast.makeText(getContext(), getContext().getString(R.string.good_done), Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
        //Goodで追加ここまで





        //1つのセルのViewを返します。
        return convertView;
    }




    //データをセットしなおす関数
    public void setMessageRecords(List<MessageRecord> objects) {
        //ArrayAdapterを空にする。
        clear();
        //テータの数だけMessageRecordを追加します。
        for(MessageRecord object : objects) {
            add(object);
        }
        //データの変更を通知します。
        notifyDataSetChanged();
    }
}
