package com.jacob.zoom;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Package : com.jacob.zoom
 * Author : jacob
 * Date : 15-3-31
 * Description : 这个类是用来xxx
 */
@SuppressWarnings("ALL")
public class ZoomImageView extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener, View.OnTouchListener, ScaleGestureDetector.OnScaleGestureListener {

    /**
     * 判断是否已经初始化
     */
    private boolean hasInit = false;

    /**
     * 缩放比例，默认最小缩放比例是1.0f
     */
    private float mInitScale = 1.0f;
    /**
     * 中间缩放比例，双击放大的比例
     */
    private float mMidScale = 2.0f;
    /**
     * 最大缩放比例
     */
    private float mMaxScale = 4.0f;

    private Matrix mMatrix;

    /**
     * 捕获用户多点手势操作
     */
    private ScaleGestureDetector mScaleGestureDetetor;

    public ZoomImageView(Context context) {
        this(context, null);
    }

    public ZoomImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setScaleType(ScaleType.MATRIX);
        mMatrix = new Matrix();
        mScaleGestureDetetor = new ScaleGestureDetector(context, this);
        setOnTouchListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getDrawable() == null) return;
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (getDrawable() == null) return;
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }


    @Override
    public void onGlobalLayout() {
        if (!hasInit) {

            Drawable drawable = getDrawable();
            if (drawable == null) return;

            int dw = drawable.getIntrinsicWidth();
            int dh = drawable.getIntrinsicHeight();

            int width = getWidth();
            int height = getHeight();

            //根据图片的尺寸和屏幕尺寸的比较，判断缩放的比例
            float scale = 1.0f;
            if (dw > width && dh < height) {
                scale = width * 1.0f / dw;
            }

            if (dw < width && dh > height) {
                scale = height * 1.0f / dh;
            }

            //如果图片整体小于屏幕，需要放大
            if (dw > width && dh > height) {
                scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
            }

            //如果图片整体大于屏幕，需要缩小
            if (dw < width && dh < height) {
                scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
            }

            //初始化缩放比例
            mInitScale = scale;
            mMidScale = mInitScale * 2;
            mMaxScale = mInitScale * 4;

            //将图片移动到屏幕中心的位置
            float dx = width / 2f - dw / 2f;
            float dy = height / 2f - dh / 2f;
            mMatrix.postTranslate(dx, dy);
            mMatrix.postScale(mInitScale, mInitScale, width / 2, height / 2);
            setImageMatrix(mMatrix);
            hasInit = true;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mScaleGestureDetetor.onTouchEvent(event);
        return true;
    }


    /**
     * 获得当前的缩放比例
     */
    private float getScale() {
        float[] value = new float[9];
        mMatrix.getValues(value);
        return value[Matrix.MSCALE_X];
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scale = getScale();

        //收拾的缩放比例，>1 表示放大，<1 表示缩小
        float factor = detector.getScaleFactor();

        if (getDrawable() == null) return true;

        //触摸的xy坐标位置
        float dx = detector.getFocusX();
        float dy = detector.getFocusY();


        //缩放范围的控制,手势操作就2种，放大和缩小
        //如果当前的缩放比例小于maxScale，是允许放大的
        //如果当前的缩放比例大于initScale，是允许缩小的
        if ((scale < mMaxScale && factor > 1.0) || (scale > mInitScale && factor < 1.0f)) {
            if (scale * factor < mInitScale) {
                factor = mInitScale / scale;
            }

            if (scale * factor > mMaxScale) {
                factor = mMaxScale / scale;
            }

            mMatrix.postScale(factor, factor, dx, dy);

            checkBorderAndCenterWhenScale();

            setImageMatrix(mMatrix);
        }
        return true;
    }

    /**
     * 在缩放的时候进行边界的和位置的控制
     */
    private void checkBorderAndCenterWhenScale() {
        RectF rectF = getMatrixRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        //缩放时进行边界检查，防止出现白边
        if (rectF.width() >= width) {
            if (rectF.left > 0) {
                deltaX = -rectF.left;
            }

            if (rectF.right < width) {
                deltaX = width - rectF.right;
            }
        }

        if (rectF.height() >= height) {
            if (rectF.top > 0) {
                deltaY = -rectF.top;
            }

            if (rectF.bottom < height) {
                deltaY = height - rectF.bottom;
            }
        }

        //如果宽度或者高度小于控件的宽或者高，让其居中，由于矩形不一定是从0，0点开始的
        if (rectF.width() < width) {
            deltaX = width / 2f - rectF.right + rectF.width() / 2f;
        }

        if (rectF.height() < height) {
            deltaY = height / 2f - rectF.bottom + rectF.height() / 2f;
        }
        mMatrix.postTranslate(deltaX, deltaY);
        setImageMatrix(mMatrix);
    }

    /**
     * 获取当前图片的矩形位置的坐标
     */
    private RectF getMatrixRectF() {
        Matrix matrix = mMatrix;
        RectF rectF = new RectF();

        Drawable drawable = getDrawable();
        if (drawable != null) {
            rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            matrix.mapRect(rectF);
        }
        return rectF;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }
}
