package androidx.mjpeg;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import java.io.File;


/**
 * MJPEG-Surface播放器
 */
public class MJPEGSurface extends SurfaceView implements SurfaceHolder.Callback, OnRequestListener, OnMP4EncodeListener {

    private final String TAG = MJPEGSurface.class.getSimpleName();
    //视频网络路径
    private String path;
    //重连间隔（单位毫秒）
    private int reconnectTime = 3000;
    //MP4视频编码
    private MP4Encoder mp4Encoder;
    //编码MP4视频保存
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
    //画布
    private MJPEGCanvas canvas;
    //缩放类型
    private ScaleType scaleType = ScaleType.CENTER_FIT;
    //调试模式
    private boolean debug = false;
    //渲染层已创建
    private boolean surfaceCreated;

    public MJPEGSurface(Context context) {
        super(context);
        initialize();
    }

    public MJPEGSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public MJPEGSurface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        print("initialize");
        getHolder().addCallback(this);
        canvas = new MJPEGCanvas(this);
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
     * 设置缩放类型
     *
     * @param scaleType
     */
    public void setScaleType(ScaleType scaleType) {
        this.scaleType = scaleType;
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        print("surface created");
        surfaceCreated = true;
        canvas.surfaceCreated(this);
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        canvas.surfaceChanged(width, height);
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        print("surface destroyed");
        canvas.surfaceDestroyed();
        release();
    }

    /**
     * 获取位图
     *
     * @return
     */
    public Bitmap getBitmap() {
        if (request == null) {
            return null;
        }
        return request.getBitmap();
    }

    /**
     * 设置视频地址
     *
     * @param path 视频地址
     */
    public void setDataSource(String path) {
        this.path = path;
        if (request != null) {
            request.path(path);
        }
    }

    /**
     * 开始播放
     */
    public void start() {
        if (TextUtils.isEmpty(path)) {
            new RuntimeException("data source path is null").printStackTrace();
            return;
        }
        request.start(surfaceCreated ? 0 : 200);
    }

    @Override
    public void onBytes(byte[] data) {
        encodeMP4(data);
    }

    @Override
    public void onBitmap(Bitmap bitmap) {
        canvas.post(bitmap, scaleType);
    }

    /**
     * 调试
     *
     * @param msg 信息
     */
    private void print(String msg) {
        if (debug) {
            Log.d(TAG, msg);
        }
    }

    /**
     * 解码保存视频
     *
     * @param data 视频数据
     */
    protected void encodeMP4(byte[] data) {
        if (encodeMP4) {
            if (mp4Encoder == null) {
                if (TextUtils.isEmpty(encodePath)) {
                    new RuntimeException("encode path is empty").printStackTrace();
                    return;
                }
                mp4Encoder = new MP4Encoder(encodePath, encodeWidth, encodeHeight, frameRate, bitRate, iFrameInterval);
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
    public void deleteEncodeMP4() {
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
        encodePath = new File(videoDir, name).getAbsolutePath();
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
     * 是否编码MP4
     *
     * @return
     */
    public boolean isEncodeMP4() {
        return encodeMP4;
    }

    /**
     * 是否编码解码保存视频
     *
     * @param encodeMP4 true:开始视频编码保存视频，false:结束视频编码
     */
    public void setEncodeMP4(boolean encodeMP4) {
        this.encodeMP4 = encodeMP4;
        if (encodeMP4 == false && request != null) {
            endEncodeMP4();
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
     * 编码帧率
     *
     * @return
     */
    public int getFrameRate() {
        return frameRate;
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
            request.height(encodeHeight);
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
        if (request != null) {
            request.cancel();
        }
        if (mp4Encoder != null) {
            mp4Encoder.end();
            mp4Encoder = null;
        }
    }

}