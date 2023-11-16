package androidx.mjpeg;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * MJPEG编解码器
 */
public class MP4Encoder {

    private final String TAG = MP4Encoder.class.getSimpleName();
    //以微秒为单位的超时，负超时表示“无限”
    private final long DEFAULT_TIMEOUT_US = 30 * 1000;
    //编码媒体类型
    private final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    //编码颜色格式
    private final int CODE_CAPABILITIES = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
    private int colorFormat;
    //媒体编解码器
    private MediaCodec mediaCodec;
    //媒体复用器
    private MediaMuxer mediaMuxer;
    //缓冲区信息
    private MediaCodec.BufferInfo bufferInfo;
    //编码视频宽度 - 640
    private int width;
    //编码视频高度 - 480
    private int height;
    //比特率
    //1080p 分辨率（全高清）：3000-8000 kbps
    //720p 分辨率（高清）：1500-5000 kbps
    //480p 分辨率（标清）：500-2500 kbps
    private int bitRate = 100000;
    //关键帧间隔
    //希望每一帧都是关键帧,1
    //视频质量和文件大小之间取得良好的平衡,10 到 30 之间
    //主要目标是减小文件大小,比如 60 或更高
    private int iFrameInterval = 60;
    //编码开始时间
    private long startTime;
    //trackIndex
    private int trackIndex;
    //线程池
    private ExecutorService service;
    //线程操作
    private Future future;
    //编码结束标志
    private boolean endFlag = false;
    //编码停止标志
    private boolean stopFlag = false;
    //调试模式
    private boolean debug = false;
    //MP4编码监听
    private OnMP4EncodeListener onMP4EncodeListener;
    private int frameIndex = -1;
    private int frameRate;

    /**
     * 设置调试模式
     *
     * @param debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * 初始化,默认帧率15，比特率100kbps,关键帧间隔60
     *
     * @param path   视频编码保存路径
     * @param width  视频编码宽度
     * @param height 视频编码高度
     */
    public MP4Encoder(String path, int width, int height) {
        this(path, width, height, 25, 1000000, 1);
    }

    /**
     * 初始化
     *
     * @param path           视频编码保存路径
     * @param width          视频编码宽度
     * @param height         视频编码高度
     * @param frameRate      帧率
     * @param bitRate        比特率<br/>
     *                       1080p 分辨率（全高清）：3000-8000 kbps<br/>
     *                       720p 分辨率（高清）：1500-5000 kbps<br/>
     *                       480p 分辨率（标清）：500-2500 kbps
     * @param iFrameInterval 关键帧间隔<br/>
     *                       希望每一帧都是关键帧,1<br/>
     *                       视频质量和文件大小之间取得良好的平衡,10 到 30 之间<br/>
     *                       主要目标是减小文件大小,比如 60 或更高
     */
    public MP4Encoder(String path, int width, int height, int frameRate, int bitRate, int iFrameInterval) {
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        service = Executors.newCachedThreadPool();
        try {
            mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
            mediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        bufferInfo = new MediaCodec.BufferInfo();
        //媒体格式设置
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        //指定解码后的帧格式
        colorFormat = YUVCodec.getColorFormat(MIME_TYPE, CODE_CAPABILITIES);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        //指定比特率
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        //指定比特率
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        //指定帧间隔
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
    }

    /**
     * 编码
     *
     * @param data 编码字节
     */
    public void encode(byte[] data) {
        future = service.submit(() -> {
            while (!stopFlag) {
                //队列取出
                int ibIndex = mediaCodec.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                startTime = System.currentTimeMillis();
                if (ibIndex >= 0) {
                    frameIndex++;
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(ibIndex);
                    inputBuffer.clear();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,options);
                    byte[] buffer = YUVCodec.getNV12(colorFormat, width, height, bitmap);
                    inputBuffer.put(buffer);
                    long presentationTimeUs = (long) (frameIndex * 1000000L / frameRate);
                    int flags = endFlag ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0;
                    //放回ByteBuffer队列
                    mediaCodec.queueInputBuffer(ibIndex, 0, buffer.length, presentationTimeUs, flags);
                    //媒体复用器写入数据
                    addTrackWrite();
                }
            }
            if (stopFlag) {
                release();
            }
        });
    }

    /**
     * 媒体复用器写入数据
     */
    private void addTrackWrite() {
        if (endFlag) {
            mediaCodec.signalEndOfInputStream();
            Log.d(TAG, "signal end of input stream");
        }
        while (true) {
            int obIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, DEFAULT_TIMEOUT_US);
            if (obIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat mediaFormat = mediaCodec.getOutputFormat();
                trackIndex = mediaMuxer.addTrack(mediaFormat);
                mediaMuxer.start();
                if (debug) {
                    Log.d(TAG, "media muxer start");
                }
            }
            //无输出可用，旋转等待EOS
            if (obIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            }
            if (obIndex >= 0) {
                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(obIndex);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    //忽略BUFFER_FLAG_CODEC_CONFIG
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0) {
                    //调整字节缓冲区值以匹配缓冲区信息
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    mediaMuxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                    if (debug) {
                        Log.d(TAG, "media muxer write sample data");
                    }
                }
                //释放写入缓冲区
                mediaCodec.releaseOutputBuffer(obIndex, false);
                //结束文件流标识
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (endFlag) {
                        if (debug) {
                            Log.i(TAG, "buffer flag end of stream");
                        }
                        stopFlag = true;
                        if (onMP4EncodeListener != null) {
                            onMP4EncodeListener.onMP4EncodeEnd();
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * 结束编码
     */
    public void end() {
        Log.i(TAG, "end");
        endFlag = true;
    }

    public void setMP4EncodeListener(OnMP4EncodeListener onMP4EncodeListener) {
        this.onMP4EncodeListener = onMP4EncodeListener;
    }

    /**
     * 释放编码
     */
    private void release() {
        Log.i(TAG, "release");
        if (future != null) {
            future.cancel(true);
        }
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }
        if (mediaMuxer != null) {
            try {
                mediaMuxer.stop();
                mediaMuxer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
