package com.jacob.zoom;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Package : com.jacob.zoom
 * Author : jacob
 * Date : 15-3-31
 * Description : 这个类是用来xxx
 */
@SuppressWarnings("ALL")
public class ZoomImageView extends ImageView implements ViewTreeObserver.OnGlobalLayoutListener {

    /**
     * 判断是否已经初始化
     */
    private boolean hasInit = false;

    private float mInitScale = 1.0f;
    private float mMidScale = 2.0f;
    private float mMaxScale = 4.0f;

    private Matrix mMatrix;

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
}
