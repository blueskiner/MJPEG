package androidx.mjpeg;

import android.graphics.Bitmap;

/**
 * 绘制通道内容
 */
public class ChannelBody {

    /**
     * 位图
     */
    private Bitmap bitmap;
    /**
     * 播放器
     */
    private MJPEGImage player;

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public MJPEGImage getPlayer() {
        return player;
    }

    public void setPlayer(MJPEGImage player) {
        this.player = player;
    }

}
