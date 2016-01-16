package com.gashihara.kmiki.gs;

//ログイン画面

import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.facebook.LoggingBehavior;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.kii.cloud.storage.Kii;
import com.kii.cloud.storage.KiiUser;
import com.kii.cloud.storage.callback.KiiSocialCallBack;
import com.kii.cloud.storage.callback.KiiUserCallBack;
import com.kii.cloud.storage.exception.CloudExecutionException;
import com.kii.cloud.storage.social.KiiSocialConnect;
import com.kii.cloud.storage.social.connector.KiiSocialNetworkConnector;


public class UserActivity extends ActionBarActivity implements Session.StatusCallback {
    private EditText mUsernameField;
    private EditText mPasswordField;
    private Button button;
    private TextView textView;
    Session session = Session.getActiveSession();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ここからFB用準備処理
        Kii.initialize("6db58565", "10c367440de47095cf56a4ef3c7ac48b", Kii.Site.JP);
        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
        //FBセッション取得

        //自動ログインのため保存されているaccess tokenを読み出す。tokenがあればログインできる
        SharedPreferences pref = getSharedPreferences(getString(R.string.save_data_name), Context.MODE_PRIVATE);
        String token = pref.getString(getString(R.string.save_token), "");//保存されていない時は""
        //tokenがないとき。
        if(token == "") {
            //ログイン画面を作る
            CreateMyView(savedInstanceState);
        }else {
            //自動ログインをする。
            try {
                //KiiCloudのAccessTokenによるログイン処理。完了すると結果がcallback関数として実行される。
                // callback関数はJava以外でも一般的に非同期で通信結果を待つのに使われる。
                KiiUser.loginWithToken(callback, token);
            } catch (Exception e) {
                //ダイアログを表示
                showAlert(R.string.operation_failed, e.getLocalizedMessage(), null);
                //画面を作る
                CreateMyView(savedInstanceState);
            }
        }

        //もしFBセッションがないなら
        if (session == null) {
            //savedInstanceStateが残ってそうならセッション回復

            if (savedInstanceState != null) {
                session = Session.restoreSession(this, null, this, savedInstanceState);
            }//そうでないならセッション初期化
            else  {
                session = new Session(this);
                Session.setActiveSession(session);
            } //Session.setActiveSession(session);
            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
                session.openForRead(new Session.OpenRequest(this).setCallback(this));
            }
        }

        updateView(session);

    }




    //ログイン用画面を作る。いつもonCreateでやっていること
    protected void CreateMyView(Bundle savedInstanceState) {
        setContentView(R.layout.activity_user);
        mUsernameField = (EditText) findViewById(R.id.username_field);
        mPasswordField = (EditText) findViewById(R.id.password_field);
        //パスワードを隠す設定
        mPasswordField.setTransformationMethod(new PasswordTransformationMethod());
        //パスワードの入力文字を制限する。参考：http://techbooster.jpn.org/andriod/ui/3857/
        mPasswordField.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        Button signupBtn = (Button) findViewById(R.id.signup_button);
        Button loginBtn = (Button) findViewById(R.id.login_button);
        button = (Button) findViewById(R.id.button);
        textView = (TextView) findViewById(R.id.fb_text);

        //ログインボタンをクリックした時の処理を設定
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //ログイン処理
                onLoginButtonClicked(v);
            }
        });
        //登録ボタンをクリックした時の処理を設定
        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //登録処理
                onSignupButtonClicked(v);
            }
        });


    }






    //FaceBookログイン
    private void updateView(final Session session) {
        if (session.isOpened()) {

            // FB login succeeded. Login to Kii with obtained access token.
            Bundle options = new Bundle();
            String accessToken = session.getAccessToken();
            options.putString("accessToken", accessToken);
            options.putParcelable("provider", KiiSocialNetworkConnector.Provider.FACEBOOK);
            KiiSocialNetworkConnector conn = (KiiSocialNetworkConnector) Kii.socialConnect(KiiSocialConnect.SocialNetwork.SOCIALNETWORK_CONNECTOR);
            conn.logIn(this, options, new KiiSocialCallBack() {
                @Override
                public void onLoginCompleted(KiiSocialConnect.SocialNetwork network, KiiUser user, Exception exception) {
                    if (exception != null) {
                        textView.setText("Failed to Login to Kii! " + exception
                                .getMessage());
                        //return;
                    } else {
                        SharedPreferences pref = getSharedPreferences(getString(R.string.save_data_name), Context.MODE_PRIVATE);
                        pref.edit().putString(getString(R.string.save_token), user.getAccessToken()).apply();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });

        } else {
            textView.setText("Login to FB");
            button.setText("LOGIN WITH FACEBOOK");
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Session session = Session.getActiveSession();
                    if (!session.isOpened() && !session.isClosed()) {
                        session.openForRead(new Session.OpenRequest(UserActivity.this)
                                .setCallback(UserActivity.this));
                    } else {
                        Session.openActiveSession(UserActivity.this, true, UserActivity.this);
                    }
                }
            });
        }
    }






    //ログイン処理：参考　http://documentation.kii.com/ja/guides/android/managing-users/sign-in/
    public void onLoginButtonClicked(View v) {
        //IMEを閉じる
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        //入力文字を得る
        String username = mUsernameField.getText().toString();
        String password = mPasswordField.getText().toString();
        try {
            //KiiCloudのログイン処理。完了すると結果がcallback関数として実行される。
            KiiUser.logIn(callback, username, password);
        } catch (Exception e) {
            //ダイアログを表示
            showAlert(R.string.operation_failed, e.getLocalizedMessage(), null);
        }
    }


    //ログインに失敗した時のダイアログ表示
    void showAlert(int titleId, String message, com.gashihara.kmiki.gs.AlertDialogFragment.AlertDialogListener listener ) {
        DialogFragment newFragment = com.gashihara.kmiki.gs.AlertDialogFragment.newInstance(titleId, message, listener);
        newFragment.show(getFragmentManager(), "dialog");
    }
    //登録処理
    public void onSignupButtonClicked(View v) {
        //IMEを閉じる
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

        //入力文字を得る。クラスの冒頭で宣言されてるので個々の関数で使い回しできる
        String username = mUsernameField.getText().toString();
        String password = mPasswordField.getText().toString();
        try {
            //KiiCloudのユーザ登録処理
            KiiUser user = KiiUser.createWithUsername(username);
            user.register(callback, password);
        } catch (Exception e) {
            showAlert(R.string.operation_failed, e.getLocalizedMessage(), null);
        }
    }




    //新規登録、ログインの時に呼び出されるコールバッククラス
    KiiUserCallBack callback = new KiiUserCallBack() {
        //ログインが完了した時に自動的に呼び出される。自動ログインの時も呼び出される
        @Override
        public void onLoginCompleted(int token, KiiUser user, Exception e) {
            // setFragmentProgress(View.INVISIBLE);
            if (e == null) {
                //自動ログインのためにSharedPreferenceに保存。アプリのストレージ。参考：http://qiita.com/Yuki_Yamada/items/f8ea90a7538234add288
                //DataSaveがファイル名みたいなものでsave_tokenは変数みたいなもの
                SharedPreferences pref = getSharedPreferences(getString(R.string.save_data_name), Context.MODE_PRIVATE);

                //prefを保存
                pref.edit().putString(getString(R.string.save_token), user.getAccessToken()).apply();

                // Intent のインスタンスを取得する。getApplicationContext()で自分のコンテキストを取得。遷移先のアクティビティーを.classで指定
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                // 遷移先の画面を呼び出す
                startActivity(intent);
                //戻るボタンでログイン画面に戻れないようにActivityを終了します。よくやる処理
                finish();
            } else {
                //エラーの状態やメッセージeがKiiCloud特有のクラスを継承している時
                if (e instanceof CloudExecutionException)
                    //KiiCloud特有のエラーメッセージを表示。フォーマットが違う
                    showAlert(R.string.operation_failed, Util.generateAlertMessage((CloudExecutionException) e), null);
                else
                    //一般的なエラーを表示
                    showAlert(R.string.operation_failed, e.getLocalizedMessage(), null);
            }
        }

        //新規登録の時に自動的に呼び出される
        @Override
        public void onRegisterCompleted(int token, KiiUser user, Exception e) {
            if (e == null) {
                //自動ログインのためにSharedPreferenceに保存。アプリのストレージ。参考：http://qiita.com/Yuki_Yamada/items/f8ea90a7538234add288
                SharedPreferences pref = getSharedPreferences(getString(R.string.save_data_name), Context.MODE_PRIVATE);
                pref.edit().putString(getString(R.string.save_token), user.getAccessToken()).apply();

                // Intent のインスタンスを取得する。getApplicationContext()で自分のコンテキストを取得。遷移先のアクティビティーを.classで指定
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                // 遷移先の画面を呼び出す
                startActivity(intent);
                //戻れないようにActivityを終了します。
                finish();
            } else {
                //eがKiiCloud特有のクラスを継承している時
                if (e instanceof CloudExecutionException)
                    //KiiCloud特有のエラーメッセージを表示
                    showAlert(R.string.operation_failed, com.gashihara.kmiki.gs.Util.generateAlertMessage((CloudExecutionException) e), null);
                else
                    //一般的なエラーを表示
                    showAlert(R.string.operation_failed, e.getLocalizedMessage(), null);
            }
        }
    };








    //Facebook 認証を完了するためのメソッドを onActivityResult に追加。お決まりの処理
    @Override
    public void call(Session session, SessionState sessionState, Exception e) {
        updateView(session);

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        if (requestCode == KiiSocialNetworkConnector.REQUEST_CODE) {
            Kii.socialConnect(KiiSocialConnect.SocialNetwork.SOCIALNETWORK_CONNECTOR)
                    .respondAuthOnActivityResult(requestCode, resultCode, data);
        }
    }

//FB用処理


    //メニュー関係：未使用
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_user, menu);
        return true;
    }

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
