package com.example.voyagerbuds.views;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

public class ZoomableImageView extends AppCompatImageView implements View.OnTouchListener {

    private Matrix matrix = new Matrix();
    private Matrix savedMatrix = new Matrix();

    // We can be in one of these 3 states
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private int mode = NONE;

    // Remember some things for zooming
    private PointF start = new PointF();
    private PointF mid = new PointF();
    private float oldDist = 1f;
    private float d = 0f;
    private float[] lastEvent = null;

    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;
    private boolean initialScaleSet = false;

    public ZoomableImageView(Context context) {
        super(context);
        sharedConstructing(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sharedConstructing(context);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        sharedConstructing(context);
    }

    private void sharedConstructing(Context context) {
        super.setClickable(true);
        this.mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        this.matrix.setTranslate(1f, 1f);
        this.setImageMatrix(matrix);
        this.setScaleType(ScaleType.MATRIX);
        this.setOnTouchListener(this);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float origScale = mScaleFactor;
            mScaleFactor *= scaleFactor;
            if (mScaleFactor > 10.0f) {
                mScaleFactor = 10.0f;
                scaleFactor = 10.0f / origScale;
            } else if (mScaleFactor < 0.1f) {
                mScaleFactor = 0.1f;
                scaleFactor = 0.1f / origScale;
            }

            if (origScale * scaleFactor < 0.1f) {
                matrix.postScale(scaleFactor, scaleFactor, getWidth() / 2, getHeight() / 2);
            } else {
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
            }

            setImageMatrix(matrix);
            return true;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        PointF curr = new PointF(event.getX(), event.getY());

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                savedMatrix.set(matrix);
                start.set(curr);
                mode = DRAG;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist > 10f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event);
                    mode = ZOOM;
                }
                lastEvent = new float[4];
                lastEvent[0] = event.getX(0);
                lastEvent[1] = event.getX(1);
                lastEvent[2] = event.getY(0);
                lastEvent[3] = event.getY(1);
                d = rotation(event);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                lastEvent = null;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    matrix.set(savedMatrix);
                    float dx = curr.x - start.x;
                    float dy = curr.y - start.y;
                    matrix.postTranslate(dx, dy);
                } else if (mode == ZOOM) {
                    // Pinch zooming is handled by ScaleGestureDetector
                }
                break;
        }

        setImageMatrix(matrix);
        return true;
    }

    @Override
    public void setImageDrawable(android.graphics.drawable.Drawable drawable) {
        super.setImageDrawable(drawable);
        initialScaleSet = false;
        resetZoom();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!initialScaleSet) {
            resetZoom();
        }
    }

    private void resetZoom() {
        if (getWidth() == 0 || getHeight() == 0 || getDrawable() == null)
            return;

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float imageWidth = getDrawable().getIntrinsicWidth();
        float imageHeight = getDrawable().getIntrinsicHeight();

        if (imageWidth <= 0 || imageHeight <= 0)
            return;

        // Calculate scale to be 98% of view width
        float scale = (viewWidth * 0.98f) / imageWidth;

        matrix.reset();
        matrix.postScale(scale, scale);

        // Center it
        float scaledImageWidth = imageWidth * scale;
        float scaledImageHeight = imageHeight * scale;
        float dx = (viewWidth - scaledImageWidth) / 2;
        float dy = (viewHeight - scaledImageHeight) / 2;

        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);

        // Reset scale factor to 1.0 relative to this new base state
        mScaleFactor = 1.0f;
        initialScaleSet = true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    public void rotate() {
        if (getDrawable() == null)
            return;

        matrix.postRotate(90, getWidth() / 2f, getHeight() / 2f);
        setImageMatrix(matrix);

        // Reset scale factor logic might need adjustment if we want to keep zoom level
        // For now, just rotating the matrix is enough for visual rotation
    }
}
