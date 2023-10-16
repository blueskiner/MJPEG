package androidx.mjpeg;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 请求
 */
public class Request {
    private String TAG = Request.class.getSimpleName();
    /**
     * 路径
     */
    private String path;
    /**
     * 开始
     */
    private boolean start = true;
    /**
     * 恢复
     */
    private boolean resume = true;
    /**
     * 重连时间
     */
    private long reconnectTime = 3000;
    /**
     * 帧率
     */
    private int frameRate = 30;
    /**
     * 视频宽度
     */
    private int width = 640;
    /**
     * 视频高度
     */
    private int height = 480;
    /**
     * 头文件长度
     */
    private int headerLength = 100;
    /**
     * 时间
     */
    private long decodeTime = 0;
    /**
     * 帧下标
     */
    private int index = 0;
    /**
     * 调试
     */
    private boolean debug;
    /**
     * 位图
     */
    private Bitmap bitmap;
    /**
     * MJPEG数据流
     */
    private MJPEGInputStream mis;
    /**
     * 缓存文件流
     */
    private BufferedInputStream bis;
    /**
     * 连接对象
     */
    private HttpURLConnection connection;
    /**
     * 连接池
     */
    private ScheduledExecutorService service;
    /**
     * 连接对象
     */
    private Future future;
    /**
     * 请求监听
     */
    private OnRequestListener onRequestListener;

    public Request() {
        initialize();
    }

    public Request(String path) {
        this.path = path;
        initialize();
    }

    protected void initialize() {
        service = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * 设置路径
     *
     * @param path
     * @return
     */
    public static Request from(String path) {
        return new Request(path);
    }

    /**
     * 设置路径
     *
     * @param path
     */
    public void path(String path) {
        this.path = path;
    }

    /**
     * 设置调试模式
     *
     * @param debug
     * @return
     */
    public Request debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    /**
     * 视频宽度
     *
     * @param width
     * @return
     */
    public Request width(int width) {
        this.width = width;
        return this;
    }

    /**
     * 设置视频高度
     *
     * @param height
     * @return
     */
    public Request height(int height) {
        this.height = height;
        return this;
    }

    /**
     * 设置帧率
     *
     * @param frameRate
     * @return
     */
    public Request frameRate(int frameRate) {
        this.frameRate = frameRate;
        return this;
    }

    /**
     * 设置头文件长度
     *
     * @param headerLength
     * @return
     */
    public Request headerLength(int headerLength) {
        this.headerLength = headerLength;
        return this;
    }

    /**
     * 设置重连时间
     *
     * @param reconnectTime
     * @return
     */
    public Request reconnectTime(long reconnectTime) {
        this.reconnectTime = reconnectTime;
        return this;
    }

    /**
     * 添加请求监听
     *
     * @param onRequestListener
     * @return
     */
    public Request addRequestListener(OnRequestListener onRequestListener) {
        this.onRequestListener = onRequestListener;
        return this;
    }

    /**
     * 暂停
     */
    public Request pause() {
        resume = false;
        return this;
    }

    /**
     * 恢复
     */
    public Request resume() {
        resume = true;
        return this;
    }

    public Request start() {
        return start(0);
    }

    /**
     * 开始
     *
     * @param delay
     */
    public Request start(long delay) {
        resume = true;
        if (service == null) {
            service = Executors.newSingleThreadScheduledExecutor();
        }
        if (future != null) {
            future.cancel(true);
        }
        future = service.schedule(() -> {
            request();
        }, delay, TimeUnit.MILLISECONDS);
        return this;
    }

    /**
     * 重试
     *
     * @param delay
     */
    public Request retry(long delay) {
        cancel();
        if (service == null) {
            service = Executors.newSingleThreadScheduledExecutor();
        }
        if (future != null) {
            future.cancel(true);
        }
        future = service.schedule(() -> {
            Log.i(TAG, "retry request...");
            request();
        }, delay, TimeUnit.MILLISECONDS);
        return this;
    }

    /**
     * 请求开始
     */
    protected Request request() {
        if (TextUtils.isEmpty(path)) {
            Log.e(TAG, "path is empty");
            return this;
        }
        Log.d(TAG, "path:" + path);
        if (path.toUpperCase().startsWith("HTTP")) {
            http();
        }
        return this;
    }


    /**
     * Http请求
     */
    private void http() {
        try {
            URL url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.i(TAG, "connect successful");
                start = true;
                bis = new BufferedInputStream(connection.getInputStream());
                read(bis);
            } else {
                Log.d(TAG, "response code:" + responseCode);
                retry(reconnectTime);
            }
        } catch (Exception e) {
            Log.d(TAG, "exception:" + e);
            retry(reconnectTime);
        }
    }

    /**
     * 读取输入流
     *
     * @param bis
     * @throws Exception
     */
    protected void read(BufferedInputStream bis) throws IOException {
        if (debug) {
            decodeTime = System.currentTimeMillis();
        }
        while (start) {
            if (resume) {
                if (mis == null) {
                    mis = new MJPEGInputStream(bis, width * height * frameRate + headerLength);
                }
                byte[] data = mis.readBytes();
                if (onRequestListener != null) {
                    onRequestListener.onBytes(data);
                }
                BitmapFactory.Options options = new BitmapFactory.Options();
                if (bitmap != null) {
                    options.inBitmap = bitmap;
                }
                options.inMutable = true;
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                if (debug) {
                    index++;
                    long duration = System.currentTimeMillis() - decodeTime;
                    if (index == frameRate) {
                        Log.d(TAG, "frame index:" + index + ",duration:" + duration);
                        decodeTime = System.currentTimeMillis();
                        index = 0;
                    }
                }
                if (onRequestListener != null) {
                    onRequestListener.onBitmap(bitmap);
                }
            }
        }
    }

    /**
     * 获取实时位图
     *
     * @return
     */
    public Bitmap getBitmap() {
        return bitmap;
    }

    /**
     * 释放资源
     */
    public void cancel() {
        Log.d(TAG, "cancel");
        start = false;
        resume = true;
        if (future != null) {
            future.cancel(true);
            future = null;
        }
        if (service != null) {
            service.shutdown();
            service = null;
        }
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
        try {
            if (bis != null) {
                bis.close();
            }
            if (mis != null) {
                mis.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            bis = null;
            mis = null;
        }
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

}
