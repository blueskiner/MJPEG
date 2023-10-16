package androidx.mjpeg;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

/**
 * 绘制通道
 */
public class Channel extends Handler {

    /**
     * 发送绘制消息
     * @param player 播放器
     * @param bitmap 位图
     */
    public void post(MJPEGImage player, Bitmap bitmap) {
        Message message = obtainMessage();
        ChannelBody body = new ChannelBody();
        body.setPlayer(player);
        body.setBitmap(bitmap);
        message.obj = body;
        sendMessage(message);
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        super.handleMessage(msg);
        ChannelBody body = (ChannelBody) msg.obj;
        body.getPlayer().setImageBitmap(body.getBitmap());
    }

    /**
     * 释放资源
     */
    public void release() {
        removeCallbacksAndMessages(null);
    }

}
