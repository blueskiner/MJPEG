package androidx.mjpeg;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

/**
 * MJPEG画布
 */
public class MJPEGCanvas {

    /**
     * 显示器
     */
    public SurfaceView surfaceView;
    /**
     * 容器
     */
    private SurfaceHolder holder;
    /**
     * 画布
     */
    private Canvas canvas;
    /**
     * 矩阵
     */
    private Matrix matrix;
    /**
     * 缩放类型
     */
    private ScaleType scaleType;
    /**
     * 缓冲位图
     */
    private Bitmap bufferBitmap;
    /**
     * 缓冲画布
     */
    private Canvas bufferCanvas;

    /**
     * 构造
     *
     * @param surfaceView 显示器
     */
    public MJPEGCanvas(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        holder = surfaceView.getHolder();
    }

    /**
     * @return
     */
    public ScaleType getScaleType() {
        return scaleType;
    }

    /**
     * 显示层创建
     *
     * @param view
     */
    public void surfaceCreated(SurfaceView view) {
        bufferBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        bufferCanvas = new Canvas(bufferBitmap);
    }

    /**
     * 显示层改变
     *
     * @param width  宽度
     * @param height 高度
     */
    public void surfaceChanged(int width, int height) {
        bufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bufferCanvas = new Canvas(bufferBitmap);
    }

    /**
     * 显示层销毁
     */
    public void surfaceDestroyed() {
        if (bufferBitmap != null) {
            bufferBitmap.recycle();
            bufferBitmap = null;
        }
        if (bufferCanvas != null) {
            bufferCanvas.setBitmap(null);
            bufferCanvas = null;
        }
        if (canvas != null) {
            canvas.setBitmap(null);
            canvas = null;
        }
    }

    /**
     * 绘制图像
     *
     * @param bitmap    图像
     * @param scaleType 缩放类型
     */
    public void post(Bitmap bitmap, ScaleType scaleType) {
        if (bitmap == null) {
            return;
        }
        if (getScaleType() != scaleType) {
            matrix = null;
        }
        this.scaleType = scaleType;
        if (matrix == null) {
            matrix = createMatrix(surfaceView, bitmap, scaleType);
        }
        if (bufferCanvas == null || bufferBitmap == null) {
            return;
        }
        //绘制在缓存画布上
        bufferCanvas.drawBitmap(bitmap, matrix, null);
        //用缓存位图绘制显示图层
        canvas = holder.lockCanvas();
        if (canvas != null) {
            canvas.drawBitmap(bufferBitmap, 0, 0, null);
            holder.unlockCanvasAndPost(canvas);
        } else {
            bitmap.recycle();
        }
    }

    /**
     * 缩放图像
     *
     * @param v         显示View
     * @param bitmap    位图
     * @param scaleType 类型
     * @return
     */
    public Matrix createMatrix(View v, android.graphics.Bitmap bitmap, ScaleType scaleType) {
        int vWidth = v.getWidth();
        int vHeight = v.getHeight();
        int bWidth = bitmap.getWidth();
        int bHeight = bitmap.getHeight();
        float scale = 1;
        Matrix matrix = new Matrix();
        if (scaleType != null) {
            if (scaleType == ScaleType.CENTER_RAW) {
                float dx = (vWidth - bWidth) * 0.5f;
                float dy = (vHeight - bHeight) * 0.5f;
                matrix.setScale(scale, scale);
                matrix.postTranslate(dx, dy);
            }
            if (scaleType == ScaleType.CENTER_CROP) {
                if (vWidth > vHeight) {
                    scale = (float) vWidth / (float) bWidth;
                } else {
                    scale = (float) vHeight / (float) bHeight;
                }
                matrix.setScale(scale, scale);
            }
            if (scaleType == ScaleType.CENTER_FIT) {
                scale = (float) vHeight / (float) bHeight;
                float dx = (vWidth - bWidth * scale) * 0.5f;
                matrix.setScale(scale, scale);
                matrix.postTranslate(dx, 0);
            }
        }
        return matrix;
    }

}
