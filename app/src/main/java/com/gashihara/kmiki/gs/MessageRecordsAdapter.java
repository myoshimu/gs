//ListViewに１つのセルの情報(message_item.xmlとMessageRecord)を結びつけるためのクラス
package com.gashihara.kmiki.gs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//<MessageRecord>はデータクラスMessageRecordのArrayAdapterであることを示している。このアダプターで管理したいデータクラスを記述されば良い。
public class MessageRecordsAdapter extends ArrayAdapter<MessageRecord> {
    private ImageLoader mImageLoader;

    //アダプターを作成する関数。コンストラクター。クラス名と同じです。
    public MessageRecordsAdapter(Context context) {
        //レイアウトのidmessage_itemのViewを親クラスに設定している
        super(context, R.layout.message_item);
        //キャッシュメモリを確保して画像を取得するクラスを作成。これを使って画像をダウンロードする。Volleyの機能
        mImageLoader = new ImageLoader(VolleyApplication.getInstance().getRequestQueue(), new BitmapLruCache());
    }
    //表示するViewを返します。これがListVewの１つのセルとして表示されます。表示されるたびに実行されます。
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //convertViewをチェックし、Viewがないときは新しくViewを作成します。convertViewがセットされている時は未使用なのでそのまま再利用します。メモリーに優しい。
        if(convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.message_item, parent, false);
        }

        //レイアウトにある画像と文字のViewを所得します。
        NetworkImageView imageView = (NetworkImageView) convertView.findViewById(R.id.image1);
        TextView textView = (TextView) convertView.findViewById(R.id.text1);
        TextView textView2 = (TextView) convertView.findViewById(R.id.text2);

        //webリンクを制御するプログラムはここから

        textView.setOnTouchListener(new ViewGroup.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //タップしたのはTextViewなのでキャスト（型の変換）する
                TextView textView = (TextView) v;

                // TextView に LinkMovementMethod を登録します
                textView.setMovementMethod(LinkMovementMethod.getInstance());

                //リンクをタップした時に処理するクラスを作成。SpannableStringを使う。
                String message = textView.getText().toString();

                LinkedList<String> ulist = new LinkedList<String>();
                ulist.add("http://google.co.jp");
                ulist.add("http://yahoo.co.jp");

                // SpannableString の取得
                SpannableString ss = createSpannableString(message, ulist);
                // SpannableString をセットし、リンクを有効化する
                textView.setText(ss);
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
        textView2.setText(imageRecord.getPdate());
        //1つのセルのViewを返します。
        return convertView;
    }


    private SpannableString createSpannableString(String message, final LinkedList<String> ulist) {

        SpannableString ss = new SpannableString(message);

        for (int i = 0; i < ulist.size(); i++) {
            int start = 0;
            int end = 0;

            // リンク化対象の文字列の start, end を算出する
            Pattern pattern = Pattern.compile(ulist.get(i));
            Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                start = matcher.start();
                end = matcher.end();
                break;
            }

            // SpannableString にクリックイベント、パラメータをセットする
            final int finalI = i;
            ss.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    String url = ulist.get(finalI);
                    Uri uri = Uri.parse(url);
                    Log.d("myurl2", uri.toString());//デバッグログを出力します。

                    Intent intent = new Intent(textView.getContext(), WebActivity.class);
                    intent.putExtra("url",uri.toString());
                    textView.getContext().startActivity(intent);
                }
            }, start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        return ss;
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
