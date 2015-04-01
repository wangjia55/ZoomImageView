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
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Package : com.jacob.zoom
 * Author : jacob
 * Date : 15-4-1
 * Description : 这个类是用来xxx
 */
public class ZoomImageViewTest extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener,
        ScaleGestureDetector.OnScaleGestureListener,
        View.OnTouchListener {

    private boolean hasInit = false;
    private float mInitScale = 1.0f;
    private float mMidScale = 2.0f;
    private float mMaxScale = 4.0f;
    private Matrix mMatrix;

    public static final float BIGGER = 1.07f;
    public static final float SMALL = 0.93f;


    //---手势操作
    private ScaleGestureDetector mScaleDetector;

    //---点击放大缩小操作
    private GestureDetector mGestureDetector;
    private boolean isAutoScale;


    public ZoomImageViewTest(Context context) {
        this(context, null);
    }

    public ZoomImageViewTest(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomImageViewTest(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setScaleType(ScaleType.MATRIX);

        mScaleDetector = new ScaleGestureDetector(context, this);
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Drawable drawable = getDrawable();
                if (drawable != null) {
                    float x = e.getX();
                    float y = e.getY();
                    if (isAutoScale) return true;
                    if (getScale() < mMidScale) {
//                        mMatrix.postScale(mMidScale / getScale(), mMidScale / getScale(), x, y);
//                        checkBorderWhenScale();
//                        setImageMatrix(mMatrix);
                        postDelayed(new AutoScaleRunnable(x, y, mMidScale), 16);
                        isAutoScale = true;
                    } else {
//                        mMatrix.postScale(mInitScale / getScale(), mInitScale / getScale(), x, y);
//                        checkBorderWhenScale();
//                        setImageMatrix(mMatrix);
                        postDelayed(new AutoScaleRunnable(x, y, mInitScale), 16);
                        isAutoScale = true;
                    }
                }
                return true;
            }
        });
        setOnTouchListener(this);

    }

    private class AutoScaleRunnable implements Runnable {
        float x;
        float y;
        float targetScale;
        float tempScale;

        private AutoScaleRunnable(float x, float y, float targetScale) {
            this.x = x;
            this.y = y;
            this.targetScale = targetScale;

            if (getScale() > targetScale) {
                tempScale = SMALL;
            }
            if (getScale() < targetScale) {
                tempScale = BIGGER;
            }
        }

        @Override
        public void run() {
            mMatrix.postScale(tempScale, tempScale, x, y);
            checkBorderWhenScale();
            setImageMatrix(mMatrix);

            float currentScale = getScale();
            if ((tempScale > 1 && currentScale < targetScale) || (tempScale < 1 && currentScale > targetScale)) {
                postDelayed(this, 16);
                isAutoScale = true;
            } else {
                float scale = targetScale / currentScale;
                mMatrix.postScale(scale, scale, x, y);
                checkBorderWhenScale();
                setImageMatrix(mMatrix);
                isAutoScale = false;
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeGlobalOnLayoutListener(this);
    }

    @Override
    public void onGlobalLayout() {
        if (!hasInit) {
            Drawable drawable = getDrawable();
            if (drawable != null) {
                //图片的尺寸
                int dw = drawable.getIntrinsicWidth();
                int dh = drawable.getIntrinsicHeight();

                //整个控件的尺寸
                int width = getWidth();
                int height = getHeight();

                float scale = 0;
                if (dw > width && dh < height) {
                    scale = width * 1.0f / dw;
                }

                if (dw < width && dh > height) {
                    scale = height * 1.0f / dh;
                }

                if (dw > width && dh > height) {
                    scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
                }

                if (dw < width && dh < height) {
                    scale = Math.min(width * 1.0f / dw, height * 1.0f / dh);
                }

                mInitScale = scale;
                mMidScale = mInitScale * 2;
                mMaxScale = mInitScale * 4;
                mMatrix = getImageMatrix();

                float deltaX = width / 2 - dw / 2;
                float deltaY = height / 2 - dh / 2;

                mMatrix.postTranslate(deltaX, deltaY);
                mMatrix.postScale(mInitScale, mInitScale, width / 2, height / 2);
                setImageMatrix(mMatrix);
            }
            hasInit = true;
        }
    }


    /**
     * 获取当前缩放比例
     */
    private float getScale() {
        float[] values = new float[9];
        mMatrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        Drawable drawable = getDrawable();
        if (drawable != null) {
            float factor = detector.getScaleFactor();
            float scale = getScale();

            float x = detector.getFocusX();
            float y = detector.getFocusY();

            if ((factor > 1 && scale < mMaxScale) || (factor < 1 && scale > mInitScale)) {
                if (factor * scale < mInitScale) {
                    factor = mInitScale / scale;
                }

                if (factor * scale > mMaxScale) {
                    factor = mMaxScale / scale;
                }

                mMatrix.postScale(factor, factor, x, y);
                checkBorderWhenScale();
                setImageMatrix(mMatrix);
            }
        }
        return true;
    }

    private void checkBorderWhenScale() {
        RectF rectF = getImageRectF();
        float deltaX = 0;
        float deltaY = 0;

        int width = getWidth();
        int height = getHeight();

        if (rectF.width() >= width) {
            if (rectF.left >= 0) {
                deltaX = -rectF.left;
            }

            if (rectF.right <= width) {
                deltaX = width - rectF.right;
            }
        }

        if (rectF.height() >= height) {
            if (rectF.top >= 0) {
                deltaY = -rectF.top;
            }

            if (rectF.bottom <= height) {
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

    private RectF getImageRectF() {
        RectF rectF = new RectF();
        Drawable drawable = getDrawable();
        if (drawable != null) {
            rectF.set(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            mMatrix.mapRect(rectF);
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (mGestureDetector.onTouchEvent(event)) return true;
        mScaleDetector.onTouchEvent(event);
        return true;
    }
}
