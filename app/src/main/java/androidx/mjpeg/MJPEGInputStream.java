package androidx.mjpeg;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * 整个数据流形式：http头信息 帧头(0xFF 0xD8) 帧数据 帧尾(0xFF 0xD9)
 * 首先通过0xFF 0xD8找到帧头位置
 * 帧头位置前的数据就是http头，里面包含Content-Length，这个字段指示了整个帧数据的长度
 * 帧头位置后面的数据就是帧图像的开始位置
 */
public class MJPEGInputStream extends DataInputStream {
    private final byte[] SOI_MARKER = {(byte) 0xFF, (byte) 0xD8};
    //private final byte[] EOF_MARKER = { (byte) 0xFF, (byte) 0xD9 };
    private final String CONTENT_LENGTH = "Content-Length";
    private final static int HEADER_MAX_LENGTH = 100;
    private final static int FRAME_MAX_LENGTH = 640 * 480 * 30 + HEADER_MAX_LENGTH;
    private int mContentLength = -1;

    /**
     * 构造MJPEG输入流
     *
     * @param is 文件输入流
     */
    public MJPEGInputStream(InputStream is) {
        super(new BufferedInputStream(is, FRAME_MAX_LENGTH));
    }

    /**
     * 构造MJPEG输入流
     *
     * @param is   文件输入流
     * @param size 字节大小（帧大小 + 头文件长度 例如:640*480*30+100）
     */
    public MJPEGInputStream(InputStream is, int size) {
        super(new BufferedInputStream(is, size));
    }

    /**
     * 此方法功能是找到索引0xFF,0XD8在字符流的位置
     *
     * @param dis      数据输入流
     * @param sequence 序列
     * @return
     * @throws IOException
     */
    private int getStartOfSequence(DataInputStream dis, byte[] sequence) throws IOException {
        int end = getEndOfSequence(dis, sequence);
        return (end < 0) ? (-1) : (end - sequence.length);
    }

    /**
     * 获取结束位置
     *
     * @param dis      数据输入流
     * @param sequence 序列
     * @return
     * @throws IOException
     */
    private int getEndOfSequence(DataInputStream dis, byte[] sequence) throws IOException {
        int seqIndex = 0;
        byte c;
        for (int i = 0; i < FRAME_MAX_LENGTH; i++) {
            c = (byte) dis.readUnsignedByte();
            if (c == sequence[seqIndex]) {
                seqIndex++;
                if (seqIndex == sequence.length)
                    return i + 1;
            } else seqIndex = 0;
        }
        return -1;
    }

    /**
     * 从http的头信息中获取Content-Length，知道一帧数据的长度
     *
     * @param headerBytes
     * @return
     * @throws IOException
     * @throws NumberFormatException
     */
    private int parseContentLength(byte[] headerBytes) throws IOException, NumberFormatException {
        ByteArrayInputStream headerIn = new ByteArrayInputStream(headerBytes);
        Properties props = new Properties();
        props.load(headerIn);
        return Integer.parseInt(props.getProperty(CONTENT_LENGTH));
    }

    /**
     * 读取数据字节
     *
     * @return
     * @throws IOException
     */
    public byte[] readBytes() throws IOException {
        mark(FRAME_MAX_LENGTH);
        int headerLen = getStartOfSequence(this, SOI_MARKER);
        reset();
        byte[] header = new byte[headerLen];
        readFully(header);
        try {
            mContentLength = parseContentLength(header);
        } catch (NumberFormatException e) {
            return null;
        }
        byte[] frameData = new byte[mContentLength];
        readFully(frameData);
        return frameData;
    }

    private Bitmap bitmap;

    /**
     * 读取位图数据
     *
     * @return
     * @throws IOException
     */
    public Bitmap readBitmap() throws IOException {
        mark(FRAME_MAX_LENGTH);
        int headerLen = getStartOfSequence(this, SOI_MARKER);
        reset();
        byte[] header = new byte[headerLen];
        readFully(header);
        try {
            mContentLength = parseContentLength(header);
        } catch (NumberFormatException e) {
            return null;
        }
        byte[] frameData = new byte[mContentLength];
        readFully(frameData);
        BitmapFactory.Options options = new BitmapFactory.Options();
        if (bitmap != null) {
            options.inBitmap = bitmap;
        }
        options.inMutable = true;
        bitmap = BitmapFactory.decodeByteArray(frameData, 0, frameData.length, options);
        return bitmap;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
    }

}