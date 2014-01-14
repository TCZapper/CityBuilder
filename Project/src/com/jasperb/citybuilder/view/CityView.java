/**
 * 
 */
package com.jasperb.citybuilder.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.jasperb.citybuilder.CityModel;
import com.jasperb.citybuilder.util.Common;
import com.jasperb.citybuilder.util.Constant;
import com.jasperb.citybuilder.util.PerfTools;
import com.jasperb.citybuilder.util.TileBitmaps;

/**
 * Custom view for handling rendering the city model. Includes the ability to pan, fling and scale within the view.
 * 
 * @author Jasper
 * 
 */
public class CityView extends View {

    /**
     * Identifier string for debug messages originating from this class
     */
    public static final String TAG = "CityView";

    public static final boolean LOG_FPS = false;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mDetector;
    private float mFocusRow = 0, mFocusCol = 0;
    private int mWidth = 0, mHeight = 0;
    private float mScaleFactor = Constant.MAXIMUM_SCALE_FACTOR;
    private Bitmap mGroundCanvasBitmap = null;
    private Canvas mGroundCanvas = null;
    private boolean mRedrawGround = true;
    private CityModel mCityModel = null;
    private boolean mDrawGridLines = true;
    private TileBitmaps mTileBitmaps = new TileBitmaps();
    private Paint mGridPaint;

    public void setCityModel(CityModel model) {
        mCityModel = model;
    }

    public float getFocusRow() {
        return mFocusRow;
    }

    public float getFocusCol() {
        return mFocusCol;
    }

    public void setFocusCoords(float row, float col) {
        this.mFocusRow = row;
        this.mFocusCol = col;
        invalidateAll();
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    public void setScaleFactor(float scaleFactor) {
        this.mScaleFactor = scaleFactor;
        invalidateAll();
    }

    public void invalidateAll() {
        Log.d(TAG, "INVALIDATE ALL");
        mRedrawGround = true;
        invalidate();
    }

    /**
     * Class constructor taking a context and an attribute set. This constructor is used by the layout engine to construct a
     * {@link CityView} from a set of XML attributes.
     * 
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param attrs
     *            An attribute set which can contain attributes inherited from {@link android.view.View}.
     */
    public CityView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    /**
     * Class constructor taking a context. This constructor is used by the layout engine to construct a {@link CityView}.
     * 
     * @param context
     *            The Context the view is running in, through which it can access the current theme, resources, etc.
     */
    public CityView(Context context) {
        super(context);
        init();
    }

    /*
     * @Override protected void onLayout(boolean changed, int l, int t, int r, int b) { // Do nothing. Do not call the superclass
     * method--that would start a layout pass // on this view's children. }
     */
    @Override
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "ON DRAW");

        canvas.drawColor(Color.BLACK);// Blank the canvas

        if (mGroundCanvas == null || mCityModel == null)
            return;
        if (mRedrawGround) {
            drawGround();
        }
        // Push everything onto screen
        canvas.drawBitmap(mGroundCanvasBitmap, 0, 0, null);

        mRedrawGround = false;
        if (LOG_FPS)
            invalidateAll();
        super.onDraw(canvas);
    }

    private void drawGround() {
        Log.d(TAG, "DRAW GROUND");
        if (LOG_FPS) {
            PerfTools.CalcAverageTick(System.currentTimeMillis());
        }

        mGroundCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);// Clear the canvas (making it transparent)

        // Calculate real coordinates for the top-most tile based off the focus iso coordinates being the centre of the screen
        float realTopX = mWidth / 2 - Common.isoToRealX(mFocusRow, mFocusCol);
        float realTopY = mHeight / 2 - Common.isoToRealY(mFocusRow, mFocusCol);

        // Shift the bitmap left to horizontally center the tile around 0,0 and subtract 1 because the image isn't drawn perfectly centered
        float bitmapOffsetX = -Constant.TILE_WIDTH / 2 - 1;

        for (int col = 0; col < mCityModel.getWidth(); col++) {
            for (int row = 0; row < mCityModel.getHeight(); row++) {
                // Log.d(TAG,"Paint Tile: " + row + " : " + col);
                mGroundCanvas.drawBitmap(mTileBitmaps.getBitmap(mCityModel.getTerrain(row, col)),
                        Common.isoToRealX(row, col) + realTopX + bitmapOffsetX, Common.isoToRealY(row, col) + realTopY, null);
            }
        }
        if (mDrawGridLines) {
            for (int col = 0; col <= mCityModel.getWidth(); col++) {
                mGroundCanvas.drawLine(Common.isoToRealX(0, col) + realTopX, Common.isoToRealY(0, col) + realTopY,
                        Common.isoToRealX(mCityModel.getHeight(), col) + realTopX,
                        Common.isoToRealY(mCityModel.getHeight(), col) + realTopY, mGridPaint);
            }
            for (int row = 0; row <= mCityModel.getHeight(); row++) {
                mGroundCanvas.drawLine(Common.isoToRealX(row, 0) + realTopX, Common.isoToRealY(row, 0) + realTopY,
                        Common.isoToRealX(row, mCityModel.getWidth()) + realTopX,
                        Common.isoToRealY(row, mCityModel.getWidth()) + realTopY, mGridPaint);
            }
        }

        // Center line
        // mGroundCanvas.drawLine(mWidth / 2, 0, mWidth / 2, mHeight, mGridPaint);
        // mGroundCanvas.drawLine(0, mHeight / 2, mWidth, mHeight / 2, mGridPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Log.d(TAG, "ON SIZE CHANGED");
        super.onSizeChanged(w, h, oldw, oldh);

        // Account for padding
        float xpad = (float) (getPaddingLeft() + getPaddingRight());
        float ypad = (float) (getPaddingTop() + getPaddingBottom());

        mWidth = (int) Math.ceil((float) w - xpad);
        mHeight = (int) Math.ceil((float) h - ypad);

        initCanvas();
        invalidateAll();
    }

    /**
     * Initialize and allocate the necessary components of the view, except those that depend on the view size or city size
     */
    private void init() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Create a gesture detector to handle onTouch messages
        mDetector = new GestureDetector(this.getContext(), new GestureListener());

        // Turn off long press--this control doesn't use it, and if long press is enabled, you can't scroll for a bit, pause, then scroll
        // some more (the pause is interpreted as a long press, apparently)
        mDetector.setIsLongpressEnabled(false);

        mScaleDetector = new ScaleGestureDetector(this.getContext(), new ScaleListener());

        mGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGridPaint.setStyle(Paint.Style.STROKE);
        mGridPaint.setStrokeWidth(0);
        mGridPaint.setColor(Color.WHITE);
    }

    /**
     * Initialize and allocate the necessary components of the view that relate to the size of the canvas
     */
    private void initCanvas() {
        // mGroundCanvasBitmap = Bitmap.createBitmap((int) (mWidth / MINIMUM_SCALE_FACTOR), (int) (mHeight / MINIMUM_SCALE_FACTOR),
        // Bitmap.Config.ARGB_8888);
        mGroundCanvasBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mGroundCanvas = new Canvas(mGroundCanvasBitmap);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Currently we do not properly try to handle these events, rather we use them as to aid debugging by forcing a complete redraw

        // Let the GestureDetectors interpret this event
        boolean result = mDetector.onTouchEvent(event);
        result = mScaleDetector.onTouchEvent(event);

        // If the GestureDetector doesn't want this event, do some custom processing.
        // This code just tries to detect when the user is done scrolling by looking
        // for ACTION_UP events.
        if (!result) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // User is done scrolling, it's now safe to do things like autocenter
                // stopScrolling();
                result = true;
            }
        }
        invalidateAll();// debug force redraw
        return result;
    }

    /**
     * Extends {@link GestureDetector.SimpleOnGestureListener} to provide custom gesture processing.
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            invalidateAll();
            return true;
        }
    }

    /**
     * The scale listener, used for handling multi-finger scale gestures.
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        /**
         * This is the active focal point in terms of the viewport. Could be a local variable but kept here to minimize per-frame
         * allocations.
         */
        private PointF viewportFocus = new PointF();
        private float lastSpanX;
        private float lastSpanY;

        // Detects that new pointers are going down.
        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            lastSpanX = scaleGestureDetector.getCurrentSpanX();
            lastSpanY = scaleGestureDetector.getCurrentSpanY();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {

            float spanX = scaleGestureDetector.getCurrentSpanX();
            float spanY = scaleGestureDetector.getCurrentSpanY();

            float scaleFactor = mScaleFactor * scaleGestureDetector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f));

            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();
            // Makes sure that the chart point is within the chart region.
            // See the sample for the implementation of hitTest().

            // constrainViewport();

            lastSpanX = spanX;
            lastSpanY = spanY;
            return true;
        }

    };

}
