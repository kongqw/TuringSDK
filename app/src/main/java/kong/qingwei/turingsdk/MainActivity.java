package kong.qingwei.turingsdk;

import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.turing.androidsdk.InitListener;
import com.turing.androidsdk.SDKInit;
import com.turing.androidsdk.SDKInitBuilder;
import com.turing.androidsdk.TuringApiManager;

import org.json.JSONException;
import org.json.JSONObject;

import turing.os.http.core.ErrorMessage;
import turing.os.http.core.HttpConnectionListener;
import turing.os.http.core.RequestResult;

public class MainActivity extends AppCompatActivity {


    private final String TURING_APIKEY = "填写你的API Key";
    private final String TURING_SECRET = "填写你的secret";
    // 填写一个任意的标示，没有具体要求，但一定要写，
    private final String UNIQUEID = "4878802";

    private final String TAG = MainActivity.class.getSimpleName();
    private TuringApiManager mTuringApiManager;

    private BitMainSpeechRecognizer mBitMainSpeechRecognizer;
    private BitMainSpeechCompound mBitMainSpeechCompound;
    private TextView mTv1;
    private TextView mTv2;
    private int mMaxVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTv1 = (TextView) findViewById(R.id.text_view1);
        mTv2 = (TextView) findViewById(R.id.text_view2);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTv1.setText("");
                mTv2.setText("");
                mBitMainSpeechRecognizer.startListening();
            }
        });


        // turingSDK初始化
        SDKInitBuilder builder = new SDKInitBuilder(this).setSecret(TURING_SECRET).setTuringKey(TURING_APIKEY).setUniqueId(UNIQUEID);
        SDKInit.init(builder, new InitListener() {
            @Override
            public void onFail(String error) {
                Log.d(TAG, error);
            }

            @Override
            public void onComplete() {
                // 获取userid成功后，才可以请求Turing服务器，需要请求必须在此回调成功，才可正确请求
                mTuringApiManager = new TuringApiManager(MainActivity.this);
                mTuringApiManager.setHttpListener(myHttpConnectionListener);
            }
        });

        mBitMainSpeechRecognizer = new BitMainSpeechRecognizer(this) {
            @Override
            public void initListener(boolean flag) {
                Log.i(TAG, "flag = " + flag);
            }

            @Override
            public void resultData(String data) {
                Log.i(TAG, "data = " + data);
                mTv1.setText(data);
                if (null != data && 0 < data.length()) {
                    mTuringApiManager.requestTuringAPI(data);
                } else {
                    mBitMainSpeechCompound.speaking("您好像没有说话哦.");
                }
            }

            @Override
            public void speechLog(String log) {
                Log.i(TAG, "log = " + log);
            }
        };

        // 初始化语音合成对象
        mBitMainSpeechCompound = new BitMainSpeechCompound(this);

        // 提示设置音量
        Dialog dialog = new AlertDialog.Builder(this)
                .setTitle("音量调整")
                .setMessage("声音太小听不到声音哦，是否调整音量？")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "确定", Toast.LENGTH_SHORT).show();
                        AudioManager audio = (AudioManager) getSystemService(Service.AUDIO_SERVICE);
                        audio.setStreamVolume(AudioManager.STREAM_MUSIC, mMaxVolume / 2, AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "取消", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("不再提醒", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "不再提醒", Toast.LENGTH_SHORT).show();
                    }
                })
                .create();

        // 获取系统音量
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (mCurrentVolume < mMaxVolume / 2) {
            dialog.show();
        }
    }

    /**
     * 网络请求回调
     */
    HttpConnectionListener myHttpConnectionListener = new HttpConnectionListener() {

        @Override
        public void onSuccess(RequestResult result) {
            if (result != null) {
                try {
                    Log.d(TAG, result.getContent().toString());
                    mTv2.setText(result.getContent().toString());
                    JSONObject result_obj = new JSONObject(result.getContent().toString());
                    if (result_obj.has("text")) {
                        Log.d(TAG, result_obj.get("text").toString());
                        mBitMainSpeechCompound.speaking(result_obj.get("text").toString());
                    }
                } catch (JSONException e) {
                    Log.d(TAG, "JSONException:" + e.getMessage());
                }
            }
        }

        @Override
        public void onError(ErrorMessage errorMessage) {
            Log.d(TAG, errorMessage.getMessage());
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            mBitMainSpeechCompound.stopSpeaking();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
