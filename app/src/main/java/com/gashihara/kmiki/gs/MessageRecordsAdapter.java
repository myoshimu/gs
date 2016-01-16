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
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

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
        textView2.setText(imageRecord.getPdate());
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
