package androidx.mjpeg;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatImageView;

import java.io.File;

/**
 * MJPEG-Image播放器
 */
public class MJPEGImage extends AppCompatImageView implements OnRequestListener, OnMP4EncodeListener {

    private final String TAG = MJPEGImage.class.getSimpleName();
    //重连间隔（单位秒）
    private int reconnectTime = 3000;
    //视频网络路径
    private String path;
    //编码视频保存
    private boolean encodeMP4 = false;
    //编码保存视频路径，例如：../video/video_encode.mp4
    private String encodePath;
    //编码宽度
    private int encodeWidth = 640;
    //编码高度
    private int encodeHeight = 480;
    //编码帧率
    private int frameRate = 15;
    //编码比特率
    private int bitRate = 100000;
    //编码帧间隔
    private int iFrameInterval = 60;

    //请求
    private Request request;
    //视频编码
    private MP4Encoder mp4Encoder;
    //绘制通道
    private Channel channel;
    //调试模式
    private boolean debug = false;

    public MJPEGImage(Context context) {
        super(context);
        initialize();
    }

    public MJPEGImage(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public MJPEGImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    /**
     * 打印日志
     *
     * @param msg 内容
     */
    private void print(String msg) {
        if (debug) {
            Log.i(TAG, msg);
        }
    }

    /**
     * 初始化
     */
    private void initialize() {
        print("initialize");
        channel = new Channel();
        request = new Request();
        request.debug(debug);
        request.width(encodeWidth).height(encodeHeight);
        request.frameRate(frameRate);
        request.reconnectTime(reconnectTime);
        request.addRequestListener(this);
    }

    /**
     * 设置调试模式
     *
     * @param debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
        if (request != null) {
            request.debug(debug);
        }
    }

    /**
     * 设置视频地址
     *
     * @param path 视频地址
     */
    public void setDataSource(String path) {
        this.path = path;
        request.path(path);
    }

    /**
     * 开始播放
     */
    public void start() {
        print("start");
        if (TextUtils.isEmpty(path)) {
            new RuntimeException("data source path is null").printStackTrace();
            return;
        }
        if (request != null) {
            request.start();
        }
    }

    @Override
    public void onBitmap(Bitmap bitmap) {
        channel.post(this, bitmap);
    }

    @Override
    public void onBytes(byte[] data) {
        encodeMP4(data);
    }

    /**
     * 设置是否可编码MP4
     * @param encodeMP4
     */
    public void setEncodeMP4(boolean encodeMP4){
        this.encodeMP4 = encodeMP4;
    }

    /**
     * 是否编码解码保存视频
     *
     * @return
     */
    public boolean isEncodeMP4() {
        return encodeMP4;
    }

    /**
     * 解码保存视频
     *
     * @param data 视频数据
     */
    protected void encodeMP4(byte[] data) {
        if (isEncodeMP4()) {
            if (mp4Encoder == null) {
                if (TextUtils.isEmpty(encodePath)) {
                    new RuntimeException("encode path is empty").printStackTrace();
                    return;
                }
                mp4Encoder = new MP4Encoder(encodePath, encodeWidth, encodeHeight);
                mp4Encoder.setMP4EncodeListener(this);
            }
            if (mp4Encoder != null && request != null) {
                mp4Encoder.setDebug(debug);
                mp4Encoder.encode(data);
            }
        }
    }

    @Override
    public void onMP4EncodeEnd() {
        encodeMP4 = false;
        mp4Encoder = null;
    }

    /**
     * 结束视频编码
     */
    public void endEncodeMP4() {
        if (mp4Encoder != null) {
            mp4Encoder.end();
        }
    }

    /**
     * 开始视频编码
     */
    public void startEncodeMP4() {
        encodeMP4 = true;
        mp4Encoder = null;
    }

    /**
     * 设置解码视频路径
     *
     * @param encodePath
     */
    public void setEncodePath(String encodePath) {
        this.encodePath = encodePath;
    }

    /**
     * 删除编码的视频
     */
    public void deleteEncodeVideo() {
        File file = new File(getEncodePath());
        if (file.exists()) {
            boolean flag = file.delete();
            print("delete encode video " + flag);
        }
    }

    /**
     * 设置解码视频路径
     *
     * @param project 项目名称,例如：MJPEG
     * @param dir     视频文件夹，例如：video
     * @param name    文件名称，例如：video_encode.mp4
     * @return
     */
    public void setEncodePath(String project, String dir, String name) {
        File projectDir = new File(Environment.getExternalStorageDirectory(), project);
        if (!projectDir.exists()) {
            projectDir.mkdirs();
        }
        File videoDir = new File(projectDir, dir);
        if (!videoDir.exists()) {
            videoDir.mkdirs();
        }
        setEncodePath(new File(videoDir, name).getAbsolutePath());
    }

    /**
     * 获取解码路径
     *
     * @return
     */
    public String getEncodePath() {
        return encodePath;
    }

    /**
     * 获取视频编码宽度
     *
     * @return
     */
    public int getEncodeWidth() {
        return encodeWidth;
    }

    /**
     * 设置视频编码宽度
     *
     * @param encodeWidth
     */
    public void setEncodeWidth(int encodeWidth) {
        this.encodeWidth = encodeWidth;
        if (request != null) {
            request.width(encodeWidth);
        }
    }

    /**
     * 设置视频编码高度
     *
     * @param encodeHeight
     */
    public void setEncodeHeight(int encodeHeight) {
        this.encodeHeight = encodeHeight;
        if (request != null) {
            request.width(encodeHeight);
        }
    }

    /**
     * 设置帧率
     *
     * @param frameRate
     */
    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
        if (request != null) {
            request.frameRate(frameRate);
        }
    }

    /**
     * 设置比特率
     *
     * @param bitRate
     */
    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    /**
     * 获取比特率
     *
     * @return
     */
    public int getBitRate() {
        return bitRate;
    }

    /**
     * 设置关键帧间隔
     *
     * @param iFrameInterval
     */
    public void setIFrameInterval(int iFrameInterval) {
        this.iFrameInterval = iFrameInterval;
    }

    /**
     * 获取关键帧间隔
     *
     * @return
     */
    public int getIFrameInterval() {
        return iFrameInterval;
    }

    /**
     * 设置重连间隔，单位毫秒
     *
     * @param reconnectTime
     */
    public void setReconnectTime(int reconnectTime) {
        this.reconnectTime = reconnectTime;
        if (request != null) {
            request.reconnectTime(reconnectTime);
        }
    }

    /**
     * 暂停
     */
    public void pause() {
        if (request != null) {
            request.pause();
        }
    }

    /**
     * 恢复
     */
    public void resume() {
        if (request != null) {
            request.resume();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        print("release");
        if (mp4Encoder != null) {
            mp4Encoder.end();
            mp4Encoder = null;
        }
        if (request != null) {
            request.cancel();
        }
        if (channel != null) {
            channel.release();
        }
    }


}