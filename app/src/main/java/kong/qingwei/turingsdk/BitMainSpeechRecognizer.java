package kong.qingwei.turingsdk;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;

/**
 * Created by kqw on 2016/6/3.
 * 在线语音识别
 */
public abstract class BitMainSpeechRecognizer {
    /**
     * 初始化的回调
     *
     * @param flag true 初始化成功 false 初始化失败
     */
    public abstract void initListener(boolean flag);

    public abstract void resultData(String data);

    public abstract void speechLog(String log);

    // 语音听写对象
    private SpeechRecognizer mIat;
    // TAG标签
    private final String TAG = BitMainSpeechRecognizer.class.getSimpleName();
    // 上下文
    private static Context mContext;

    public BitMainSpeechRecognizer(Context context) {
        // 获取上下文
        mContext = context;
        // 初始化识别对象
        mIat = SpeechRecognizer.createRecognizer(context, new InitListener() {

            @Override
            public void onInit(int code) {
                Log.d(TAG, "SpeechRecognizer init() code = " + code);
                if (code != ErrorCode.SUCCESS) {
                    initListener(false);
                    Toast.makeText(mContext, "初始化失败,错误码：" + code, Toast.LENGTH_SHORT).show();
                } else {
                    initListener(true);
                }
            }
        });
    }

    /**
     * 开始录音
     */
    public void startListening() {
        // 设置参数
        setParam();
        // 不显示听写对话框
        int ret = mIat.startListening(recognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            Toast.makeText(mContext, "听写失败,错误码：" + ret, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 停止录音并获取录入的文字
     */
    public void cancel() {
        if (null != mIat) {
            // mIat.cancel();
            mIat.stopListening();
        }
    }

    /**
     * 听写监听器。
     */
    private RecognizerListener recognizerListener = new RecognizerListener() {

        StringBuffer resultText;

        @Override
        public void onBeginOfSpeech() {
            speechLog("开始说话");
            Log.i(TAG, "开始说话");
            ToastUtil.showText(mContext, "开始说话");
            resultText = new StringBuffer();
        }

        @Override
        public void onError(SpeechError error) {
            Log.i(TAG, error.getPlainDescription(true));
            if (10118 == error.getErrorCode()) {
                ToastUtil.showText(mContext, "您好像没有说话哦.");
                resultData(null);
            }
        }

        @Override
        public void onEndOfSpeech() {
            speechLog("结束说话");
            Log.i(TAG, "结束说话");
            ToastUtil.showText(mContext, "结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            String text = JsonUtils.parseIatResult(results.getResultString());
//            String text = results.getResultString();
            resultText.append(text);
            if (isLast) {
                // 最后的结果
                resultData(resultText.toString().trim());
            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            speechLog("当前正在说话，音量大小：" + volume);
            Log.i(TAG, "当前正在说话，音量大小：" + volume);
            ToastUtil.showText(mContext, "正在录音，音量大小：" + volume);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }

    };

    /**
     * 参数设置
     *
     * @return
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);
        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");
        // 设置语言
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语言区域
        mIat.setParameter(SpeechConstant.ACCENT, "zh_cn");
        // 设置语音前端点
        mIat.setParameter(SpeechConstant.VAD_BOS, "4000");
        // 设置语音后端点
        mIat.setParameter(SpeechConstant.VAD_EOS, "1000");
        // 设置标点符号
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");
        // 设置音频保存路径
        // mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH,
        // Environment.getExternalStorageDirectory() + "/iflytek/wavaudio.pcm");
    }
}
