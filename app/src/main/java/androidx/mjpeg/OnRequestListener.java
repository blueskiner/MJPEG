package androidx.mjpeg;

import android.graphics.Bitmap;

/**
 * 请求监听
 */
public interface OnRequestListener {

    /**
     * MJPEG位图
     *
     * @param bitmap 位图
     */
    void onBitmap(Bitmap bitmap);

    /**
     * MJPEG字节数据
     *
     * @param data
     */
    void onBytes(byte[] data);

}
