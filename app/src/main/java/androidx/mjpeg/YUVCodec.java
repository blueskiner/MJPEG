package androidx.mjpeg;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * YUV编码
 */
public class YUVCodec {

    /**
     * 获取支持的媒体的颜色格式
     *
     * @param mimeType 媒体类型 {@link MediaFormat#MIMETYPE_VIDEO_AVC}
     * @return {@link MediaCodecInfo.CodecCapabilities#COLOR_FormatYUV420Flexible}
     */
    public static int[] getSupportColorFormats(String mimeType) {
        MediaCodecInfo[] info = new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();
        for (int i = 0; i < info.length; i++) {
            String[] types = info[i].getSupportedTypes();
            for (String name : types) {
                if (name.equals(mimeType)) {
                    MediaCodecInfo.CodecCapabilities capabilities = info[i].getCapabilitiesForType(name);
                    return capabilities.colorFormats;
                }
            }
        }
        return null;
    }

    /**
     * 获取所有支持的媒体的颜色格式
     *
     * @return {@link MediaCodecInfo.CodecCapabilities#COLOR_FormatYUV420Flexible}
     */
    public static int[] getSupportColorFormats() {
        List<Integer> list = new ArrayList<>();
        MediaCodecInfo[] infos = new MediaCodecList(MediaCodecList.ALL_CODECS).getCodecInfos();
        for (int i = 0; i < infos.length; i++) {
            String[] types = infos[i].getSupportedTypes();
            for (String name : types) {
                MediaCodecInfo.CodecCapabilities capabilities = infos[i].getCapabilitiesForType(name);
                int[] colorFormats = capabilities.colorFormats;
                for (int j = 0; j < colorFormats.length; j++) {
                    list.add(colorFormats[j]);
                }
            }
        }
        int[] formats = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            formats[i] = list.get(i);
        }
        return formats;
    }

    /**
     * 媒体的颜色格式
     *
     * @param mimeType     媒体类型 {@link MediaFormat#MIMETYPE_VIDEO_AVC}
     * @param capabilities 编码能力 {@link MediaCodecInfo.CodecCapabilities#COLOR_FormatYUV420Flexible}
     * @return
     */
    public static int getColorFormat(String mimeType, int capabilities) {
        int[] formats = getSupportColorFormats(mimeType);
        int colorFormat = 0;
        for (int format : formats) {
            if (format == capabilities) {
                colorFormat = format;
            }
        }
        if (colorFormat == 0) {
            colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
        }
        return colorFormat;
    }

    /**
     * 获取NV12编码数据
     *
     * @param colorFormat 媒体颜色格式
     * @param inputWidth  输入宽度
     * @param inputHeight 输入高度
     * @param bitmap      位图
     * @return
     */
    public static byte[] getNV12(int colorFormat, int inputWidth, int inputHeight, Bitmap bitmap) {
        int[] argb = new int[inputWidth * inputHeight];
        bitmap.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);
        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];
        switch (colorFormat) {
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible:
                encodeYUV420P(yuv, argb, inputWidth, inputHeight);
                break;
        }
        return yuv;
    }

    /**
     * YUV420P编码
     *
     * @param yuv420sp yuv420sp字节
     * @param argb     argb字节
     * @param width    图像宽度
     * @param height   图像高度
     */
    public static void encodeYUV420P(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;
        int yIndex = 0;
        int uIndex = frameSize;
        int vIndex = frameSize + width * height / 4;
        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;
                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                V = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128; // Previously U
                U = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128; // Previously V

                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[vIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                    yuv420sp[uIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                }
                index++;
            }
        }
    }

    /**
     * YUV420SP编码
     *
     * @param yuv420sp yuv420sp字节
     * @param argb     argb字节
     * @param width    图像宽度
     * @param height   图像高度
     */
    public static void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff) >> 0;
                // well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                V = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128; // Previously U
                U = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128; // Previously V
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }
                index++;
            }
        }
    }

}
