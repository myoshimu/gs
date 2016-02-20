//投稿するActivityです
package com.gashihara.kmiki.gs;

import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.kii.cloud.abtesting.KiiExperiment;
import com.kii.cloud.abtesting.Variation;
import com.kii.cloud.analytics.KiiEvent;
import com.kii.cloud.storage.GeoPoint;
import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiBucket;
import com.kii.cloud.storage.KiiObject;
import com.kii.cloud.storage.callback.KiiObjectCallBack;
import com.kii.cloud.storage.callback.KiiObjectPublishCallback;
import com.kii.cloud.storage.exception.CloudExecutionException;
import com.kii.cloud.storage.resumabletransfer.KiiRTransfer;
import com.kii.cloud.storage.resumabletransfer.KiiRTransferCallback;
import com.kii.cloud.storage.resumabletransfer.KiiUploader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class PostActivity extends ActionBarActivity {
    //今回使用するインテントの結果の番号。適当な値でOK.
    private static final int IMAGE_CHOOSER_RESULTCODE = 1;
    //画像のパスを保存しておく
    private String mImagePath = null;
    //UPした画像のKiiObject
    private KiiObject mKiiImageObject = null;
    //入力したコメント
    private String comment;
    //カメラで撮影した画像のuri
    private Uri mImageUri;

    KiiExperiment experiment = null;//GrowthHack(ABテスト)修正。ABテストクラス。

    //kmiki 追加部分
    private String pdate;
    private LocationManager mLocationManager;
    //kmiki ここまで

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        //画像ボタンにクリックイベントを追加しています。
        Button attachBtn = (Button) findViewById(R.id.attach_button);
        attachBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //クリックした時は画像選択
                onAttachFileButtonClicked(v);
            }
        });
        //カメラボタンにクリックイベントを追加しています。
        Button attachCameraBtn = (Button) findViewById(R.id.attach_camera_button);
        attachCameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //クリックした時はカメラ起動する
                onAttachCameraFileButtonClicked(v);
            }
        });
        //投稿ボタンにクリックイベントを追加しています。
        Button postBtn = (Button) findViewById(R.id.post_button);
        postBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //クリックした時は投稿する
                onPostButtonClicked(v);
            }
        });
/*        ImageButton geoBtn = (ImageButton) findViewById(R.id.geobutton);
        geoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBtnGpsClicked(v);
            }
        }); */

        new ABTestInfoFetchTask().execute();//GrowthHack(ABテスト)修正。ABテスト環境を非同期で設定する。


        // GPS
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsFlg = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        Log.d("GPS Enabled", gpsFlg ? "OK" : "NG");
    }

/*    public void onBtnGpsClicked(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, //LocationManager.NETWORK_PROVIDER,
                3000, // 通知のための最小時間間隔（ミリ秒）
                10, // 通知のための最小距離間隔（メートル）
                new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        String msg = "Lat=" + location.getLatitude()
                                + "\nLng=" + location.getLongitude();
                        Log.d("GPS", msg);
                        if (ActivityCompat.checkSelfPermission(PostActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(PostActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                             return;
                        }
                        mLocationManager.removeUpdates(this);
                    }

                    @Override
                    public void onProviderDisabled(String provider) {
                    }

                    @Override
                    public void onProviderEnabled(String provider) {
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                    }
                }
        );
    }*/


    //GrowthHack(ABテスト)追加ここから
    //AsyncTaskを使って非同期でABテストの情報を取得し、画面に反映する。参考：http://gihyo.jp/dev/serial/01/mbaas/0013
    public  class ABTestInfoFetchTask extends AsyncTask<Void, Void, KiiExperiment> {
        //非同期の処理。returnでイベントにいろいろな値をわたせる
        @Override
        protected KiiExperiment doInBackground(Void... params) {
            try {
                //ABテストのIDを通知。自分のIDに変更してください。
                experiment = KiiExperiment.getByID("caef4e70-d222-11e5-8f4e-22000aa79e15");
            } catch (Exception e) {
                Log.d("A/B test failed.", e.getLocalizedMessage());
            }
            return experiment;
        }
        //doInBackgroundが実行された後に自動的に実行される。
        @Override
        protected void onPostExecute(KiiExperiment experiment) {
            Variation va;//ABテストの結果のクラス
            String postText = "post";//表示する文字。
            try {
                //ABテストのテスト結果(AまたはBの情報)を得る。ユーザごとに固定。Aの結果をもらったらずっとA。
                va = experiment.getAppliedVariation();
            } catch (Exception e) {
                Log.d("A/B experiment failed.", e.getLocalizedMessage());
                //エラーの時はAの情報を利用する。
                va = experiment.getVariationByName("A");
            }
            //結果のJSONデータを得る。
            JSONObject test = va.getVariableSet();
            try {
                //ABテストで設定したpostTextの値を得る。postかsend
                postText = test.getString("postText");
                Log.d("A/B Get postText",postText);
            } catch (JSONException e) {
            }
            //postボタンを探す
            Button buttonView = (Button) findViewById(R.id.post_button);
            //ABテストの文字をセット
            buttonView.setText(postText);
            //ABテストの表示のイベントを送る。eventViewedに集計される。
            new SendABTestEventTask("eventViewed").execute();
        }
    }
    //ABテストのイベントを非同期で送信するクラス
    private class SendABTestEventTask extends AsyncTask<Void, Void, Boolean> {

        private String eventName;
        private KiiEvent event=null;

        private SendABTestEventTask(String eventName) {
            this.eventName = eventName;
            try {
                //ABテストのクラスがあれば、イベント名を送信
                if (experiment != null) {
                    Variation variation = experiment.getAppliedVariation();
                    event = variation
                            .eventForConversion(getApplicationContext(), eventName);
                    Log.d("A/B eventname Send",eventName);
                }
            } catch (Exception e) {
                // eventがセットされない(null)であることを失敗とみなす。
                Log.d("A/B eventname Send NG",eventName);
                e.printStackTrace();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (event == null) {
                return false;
            }
            try {
                //送信
                event.push();
                Log.d("A/B TestEvent Send",eventName);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        //送信後の結果
        @Override
        protected void onPostExecute(Boolean result) {
            // 成功失敗によらず、ログ出力のみで結果をユーザに通知はしない。
            if (result) {
                Log.d("A/B　send ok", eventName);
            } else {
                Log.d("A/B　send ng", eventName);
            }
        }
    }
    //GrowthHack(ABテスト)追加ここまで







    //画像の添付ボタンをおした時の処理
    public void onAttachFileButtonClicked(View v) {
        //ギャラリーを開くインテントを作成して起動する。
        Intent intent = new Intent();
        //フアイルのタイプを設定
        intent.setType("image/*");
        //画像のインテント
        intent.setAction(Intent.ACTION_GET_CONTENT);
        //Activityを起動
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), IMAGE_CHOOSER_RESULTCODE);
    }

    //カメラの添付ボタンをおした時の処理
    public void onAttachCameraFileButtonClicked(View v) {
        //カメラは機種依存が大きく、いろいろサンプルを見たほうが良い
        //コメントはXperia用に作ったもの。不要。
        //カメラのインテントを作成
        //Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //Activityを起動
        //startActivityForResult(Intent.createChooser(intent, "Camera"), IMAGE_CHOOSER_RESULTCODE);
        //現在時刻をもとに一時ファイル名を作成
        String filename = System.currentTimeMillis() + ".jpg";
        //設定を保存するパラメータを作成
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, filename);//ファイル名
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");//ファイルの種類
        //設定した一時ファイルを作成
        mImageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        //カメラのインテントを作成
        Intent intent = new Intent();
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);//カメラ
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);//画像の保存先
        //インテント起動
        startActivityForResult(intent, IMAGE_CHOOSER_RESULTCODE);
    }

    //画像を選択した後に実行されるコールバック関数。インテントの実行された後にコールバックされる。自動的に実行されます。
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //他のインテントの実行結果と区別するためstartActivityで指定した定数IMAGE_CHOOSER_RESULTCODEと一致するか確認
        if (requestCode == IMAGE_CHOOSER_RESULTCODE) {
            //失敗の時
            if (resultCode != RESULT_OK) {
                return;
            }

            //画像を取得する。Xperiaの場合はdataに画像が入っている。それ以外はintentで設定したmImageUriに入っている。
            Uri result;
            if (data != null) {
                result = data.getData();
            } else {
                result = mImageUri;
                Log.d("result", result.toString());
            }
            //画面に画像を表示
            ImageView iv = (ImageView) findViewById(R.id.image_view1);

            iv.setImageURI(result);
            mImagePath = getFilePathByUri(result);

            if (result!=null) {
            //画像のパスを設定。Uploadでつかう。
        //ファイルをUP、完了した時にpostMessagesを実行している。
                Log.d("result2", result.toString());
        //kmiki_01_25 画像から緯度軽度を取得



            }

        }
    }

    //uriからファイルのパスを取得する。バージョンによって処理が違う。KiiCloudのチュートリアルから取り込んだ。汎用的に使えます。
    private String getFilePathByUri(Uri selectedFileUri) {
        //4.2以降の時
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Workaround of retrieving file image through ContentResolver
            // for Android4.2 or later
            String filePath = null;
            FileOutputStream fos = null;
            try {
                //ビットマップを取得
                ContentResolver contentResolver = getContentResolver();
                Bitmap bmp = MediaStore.Images.Media.getBitmap(
                        contentResolver, selectedFileUri);

               /* Cursor cursor = getContentResolver().query(selectedFileUri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
                if (cursor == null)
                    return null;
                try {
                    if (!cursor.moveToFirst()){
                        return null;
                    }
                    //これがファイルのパス
                    String picturePath = cursor.getString(0);
                    Log.d("pictpath",picturePath.toString());
                } finally {
                    cursor.close();
                }
                *//*try {
                    // ExifInterfaceインスタンスを生成
                    //ExifInterface exif = new ExifInterface(filePath);
                    // TODO タグ情報を取得
                } catch (IOException e) {
                    e.printStackTrace();
                }*/





                //一時保存するディレクトリ。アプリに応じてgsappの部分を変更したほうが良い
                String cacheDir = Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + File.separator + "gsapp";
                //ディレクトリ作成
                File createDir = new File(cacheDir);
                if (!createDir.exists()) {
                    createDir.mkdir();
                }
                //一時ファイル名を作成。毎回上書き
                filePath = cacheDir + File.separator + "upload.jpg";
                File file = new File(filePath);
                //ビットマップをjpgに変換して一時的に保存する。
                fos = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                fos.flush();
                fos.getFD().sync();
            } catch (Exception e) {
                filePath = null;
            } finally {//かならず最後に実行する処理
                if (fos != null) {
                    try {
                        //ファイルを閉じる
                        fos.close();
                    } catch (Exception e) {
                        // Nothing to do
                    }
                }
            }
            return filePath;
        } else {
            //データから探す
            String[] filePathColumn = {MediaStore.MediaColumns.DATA};
            Cursor cursor = this.getContentResolver().query(
                    selectedFileUri, filePathColumn, null, null, null);

            if (cursor == null)
                return null;
            try {
                if (!cursor.moveToFirst())
                    return null;
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                if (columnIndex < 0) {
                    return null;
                }
                //これがファイルのパス
                String picturePath = cursor.getString(columnIndex);
                return picturePath;
            } finally {
                cursor.close();
            }
        }
    }


    //投稿ボタンを御した時の処理
    public void onPostButtonClicked(View v) {
        //入力文字を得る
        EditText mCommentField = (EditText) (findViewById(R.id.comment_field));
        comment = mCommentField.getText().toString();
        //Log.d("mogi comment", ":" + comment + ":");
        //未入力の時はエラー.""は文字が空
        if (comment.equals("")) {
            //ダイアログを表示
            showAlert(getString(R.string.no_data_message));
            return;
        }
        //画像をUPしてからmessagesに投稿。
        if (mImagePath != null) {

            uploadFile(mImagePath);
        } else {
            //画像がないときはcommentだけ登録
            postMessages(null);
        }




        //GrowthHack(ABテスト)追加ここから
        //ABテストのクリックのイベントを送る。eventClickedに集計される。
        new SendABTestEventTask("eventClicked").execute();
        //GrowthHack(ABテスト)追加ここまで


    }

    //投稿処理。画像のUploadがうまくいったときは、urlに公開のURLがセットされる
    public void postMessages(String url) {
        //バケット名を設定。バケット＝DBのテーブルみたいなもの。Excelのシートみたいなもの。
        KiiBucket bucket = Kii.bucket("messages");
        KiiObject object = bucket.object();
        //Json形式でKeyのcommentをセット.{"comment":"こめんとです","imageUrl":"http://xxx.com/xxxx"}
        object.set("comment", comment);


//kmiki geo
        //KiiObject object2 = KiiUser.getCurrentUser().bucket("locations").object();
        GeoPoint point = new GeoPoint(35.658603, 139.745433);
        object.set("location1", point);




        //画像があるときだけセット
        if(url != null) {
            object.set("imageUrl", url);
        }
        //データをKiiCloudに保存
        object.save(new KiiObjectCallBack() {
            //保存結果が帰ってくるコールバック関数。自動的に呼び出される。
            @Override
            public void onSaveCompleted(int token, KiiObject object, Exception exception) {
                //エラーがないとき
                if (exception == null) {
                    // Intent のインスタンスを取得する。getApplicationContext()で自分のコンテキストを取得。遷移先のアクティビティーを.classで指定
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    //Activityを終了します。なくてもいい？
                    finish();
                } else {
                    //eがKiiCloud特有のクラスを継承している時
                    if (exception instanceof CloudExecutionException)
                        //KiiCloud特有のエラーメッセージを表示。フォーマットが違う
                        showAlert(Util.generateAlertMessage((CloudExecutionException) exception));
                    else
                        //一般的なエラーを表示
                        showAlert(exception.getLocalizedMessage());
                }
            }
        });
    }
    //画像をKiiCloudのimagesにUPする。参考：チュートリアル、http://www.riaxdnp.jp/?p=6775
    private void uploadFile(String path) {
        //イメージを保存するバケット名を設定。すべてここに保存してmessageにはそのhttpパスを設定する。バケット＝DBのテーブルみたいなもの。Excelのシートみたいなもの。
        KiiBucket bucket = Kii.bucket("images");
        KiiObject object = bucket.object();
        //Up後に公開設定するので保存
        mKiiImageObject = object;
        File f = new File(path);
        //KiiCloudにUPするインスタンス
        KiiUploader uploader = object.uploader(this, f);
        //非同期でUpする。
        uploader.transferAsync(new KiiRTransferCallback() {
            //完了した時
            @Override
            public void onTransferCompleted(KiiRTransfer operator, Exception e) {
                if (e == null) {
                    //成功の時
                    //画像を一覧で表示するため、公開状態にする。参考：http://www.riaxdnp.jp/?p=6841
                    // URI指定Objをリフレッシュして、最新状態にする
                    mKiiImageObject.refresh(new KiiObjectCallBack() {
                        public void onRefreshCompleted(int token, KiiObject object, Exception e) {
                            if (e == null) {
                                // ObjectBodyの公開設定する
                                object.publishBody(new KiiObjectPublishCallback() {
                                    @Override
                                    public void onPublishCompleted(String url, KiiObject kiiObject, Exception e) {
                                        Log.d("mogiurl", url);
                                        //画像のURL付きでmessagesに投稿する。
                                        postMessages(url);
                                    }
                                });
                            }
                        }
                    });


                } else {
                    //失敗の時
                    Throwable cause = e.getCause();
                    if (cause instanceof CloudExecutionException)
                        showAlert(Util
                                .generateAlertMessage((CloudExecutionException) cause));
                    else
                        showAlert(e.getLocalizedMessage());
                }
            }
        });
    }
    //エラーダイアログを表示する
    void showAlert(String message) {
        DialogFragment newFragment = AlertDialogFragment.newInstance(R.string.operation_failed, message, null);
        newFragment.show(getFragmentManager(), "dialog");
    }


    //GrowthHackで追加ここから
    @Override
    protected void onStart() {
        super.onStart();
        Tracker t = ((VolleyApplication)getApplication()).getTracker(VolleyApplication.TrackerName.APP_TRACKER);
        t.setScreenName(this.getClass().getSimpleName());
        t.send(new HitBuilders.AppViewBuilder().build());
    }
    //GrowthHackで追加ここまで


 /*   @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_post, menu);
        return true;
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
