package com.jacob.zoom;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
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
    //--------自由移动

    /**
     * 记录上一次多点触控的个数
     */
    private int mLastPointCount;

    /**
     * 记录上次触摸的中心点的坐标
     */
    private float mLastX;
    private float mLastY;

    private int mTouchSlop;
    private boolean isCanDrag;

    //------双击放大与缩小
    private GestureDetector mGestureDetector;

    private boolean isAutoScale;

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

        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                float x = e.getX();
                float y = e.getY();
                if (isAutoScale) return true;
                if (getScale() < mMidScale) {
//                    mMatrix.postScale(mMidScale / getScale(), mMidScale / getScale(), x, y);
//                    setImageMatrix(mMatrix);
                    postDelayed(new AutoScalerunnable(mMidScale, x, y), 16);
                    isAutoScale = true;
                } else {
//                    mMatrix.postScale(mInitScale / getScale(), mInitScale / getScale(), x, y);
//                    setImageMatrix(mMatrix);
                    postDelayed(new AutoScalerunnable(mInitScale, x, y), 16);
                    isAutoScale = true;
                }
                return true;
            }
        });
    }


    /**
     * 自动缩放
     */
    private class AutoScalerunnable implements Runnable {
        private float targetScale; //缩放的目标值
        private float x;  //缩放的中心点
        private float y;

        private final float BIGGER = 1.07f;
        private final float SMALL = 0.93f;

        private float tmpScale;


        private AutoScalerunnable(float targetScale, float x, float y) {
            this.targetScale = targetScale;
            this.x = x;
            this.y = y;

            if (getScale() < targetScale) {
                tmpScale = BIGGER;
            }

            if (getScale() >= targetScale) {
                tmpScale = SMALL;
            }
        }

        @Override
        public void run() {
            mMatrix.postScale(tmpScale, tmpScale, x, y);
            checkBorderAndCenterWhenScale();
            setImageMatrix(mMatrix);

            float currentScale = getScale();
            if ((tmpScale > 1.0f && currentScale < targetScale) || (tmpScale < 1.0f && currentScale > targetScale)) {
                postDelayed(this, 15);
                isAutoScale = true;
            } else {
                float scale = targetScale / currentScale;
                mMatrix.postScale(scale, scale, x, y);
                checkBorderAndCenterWhenScale();
                setImageMatrix(mMatrix);
                isAutoScale = false;
            }
        }
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
        if (mGestureDetector.onTouchEvent(event)) return true;

        mScaleGestureDetetor.onTouchEvent(event);

        float x = 0;
        float y = 0;

        //多点触摸的数量
        int pointCount = event.getPointerCount();
        for (int i = 0; i < pointCount; i++) {
            x = x + event.getX(i);
            y = y + event.getY(i);
        }

        x /= pointCount;
        y /= pointCount;

        if (mLastPointCount != pointCount) {
            isCanDrag = false;
            mLastX = x;
            mLastY = y;
        }
        mLastPointCount = pointCount;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:

                break;
            case MotionEvent.ACTION_MOVE:
                float dx = x - mLastX;
                float dy = y - mLastY;

                if (!isCanDrag) {
                    isCanDrag = isMoveAction(dx, dy);
                }

                if (isCanDrag) {
                    RectF rectF = getMatrixRectF();
                    if (getDrawable() != null) {
                        if (rectF.width() < getWidth()) {
                            dx = 0;
                        }

                        if (rectF.height() < getHeight()) {
                            dy = 0;
                        }

                        mMatrix.postTranslate(dx, dy);
                        checkBorderAndCenterWhenScale();
                        setImageMatrix(mMatrix);
                    }
                }
                mLastX = x;
                mLastY = y;
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mLastPointCount = 0;
                break;
        }
        return true;
    }

    /**
     * 判断是否是move
     */
    private boolean isMoveAction(float dx, float dy) {
        return Math.sqrt(dx * dx + dy * dy) > mTouchSlop;
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
