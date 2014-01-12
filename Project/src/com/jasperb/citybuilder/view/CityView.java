/**
 * 
 */
package com.jasperb.citybuilder.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.jasperb.citybuilder.CityModel;

/**
 * Custom view for handling rendering the city model. Includes the ability to pan, fling and scale within the view.
 * @author Jasper
 * 
 */
public class CityView extends View {

    /**
     * Identifier string for debug messages originating from this class
     */
    public static final String TAG = "CityView";

    /**
     * Maximum amount of zoom for the contents of the view
     */
    public static final float MAXIMUM_SCALE_FACTOR = 1.f;

    /**
     * Minimum amount of zoom for the contents of the view
     */
    public static final float MINIMUM_SCALE_FACTOR = 0.25f;

    /**
     * Width of a single tile in pixels
     */
    public static final float TILE_WIDTH = 96;

    /**
     * Height of a single tile in pixels
     */
    public static final float TILE_HEIGHT = TILE_WIDTH / 2;

    public enum PERSPECTIVE {
        LEFT, RIGHT
    }

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mDetector;
    private int mFocusX = 0, mFocusY = 0;
    private int mWidth = 0, mHeight = 0;
    private float mScaleFactor = MAXIMUM_SCALE_FACTOR;
    private Bitmap mGroundCanvasBitmap = null;
    private Canvas mGroundCanvas = null;
    private boolean mRedrawGround = true;
    private CityModel mCityModel = null;
    private Paint mTilePaint;
    private boolean drawGridLines = false;
    private Path mTilePath;
    private Matrix mTranslateRowMatrix, mTranslateColMatrix, mMatrix;

    public void setCityModel(CityModel model) {
        mCityModel = model;
        mTranslateRowMatrix.setTranslate(-TILE_WIDTH / 2, TILE_HEIGHT / 2);
        mTranslateColMatrix.setTranslate((TILE_WIDTH / 2) * (mCityModel.getHeight() + 1),
                (-TILE_HEIGHT / 2) * (mCityModel.getHeight() - 1));
    }

    public int getFocusX() {
        return mFocusX;
    }

    public int getFocusY() {
        return mFocusY;
    }

    public void setFocusCoords(int x, int y) {
        this.mFocusX = x;
        this.mFocusY = y;
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
        super.onDraw(canvas);
    }

    private void drawGround() {
        Log.d(TAG, "DRAW GROUND");

        mGroundCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);// Clear the canvas (making it transparent)

        // For the moment we're going to draw every tile, even those not in view
        mMatrix.setTranslate(mWidth / 2, 0);// don't account for mFocus yet
        mTilePath.transform(mMatrix);
        for (int col = 0; col < mCityModel.getWidth(); col++) {
            for (int row = 0; row < mCityModel.getHeight(); row++) {
                // Log.d(TAG,"Paint Tile: " + row + " : " + col);
                switch (mCityModel.getTerrain(row, col)) {
                case GRASS:
                    mTilePaint.setColor(Color.GREEN);
                    break;
                case DIRT:
                    mTilePaint.setColor(Color.DKGRAY);
                    break;
                }
                mGroundCanvas.drawPath(mTilePath, mTilePaint);
                mTilePath.transform(mTranslateRowMatrix);
            }
            mTilePath.transform(mTranslateColMatrix);
        }
        mMatrix.setTranslate((-TILE_WIDTH / 2) * mCityModel.getWidth() - mWidth / 2, (-TILE_HEIGHT / 2) * mCityModel.getWidth());
        mTilePath.transform(mMatrix);// return mTilePath to origin (0,0)
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

    // Rough conversion
    private Point isoToReal(Point pt) {
        Point tempPt = new Point(0, 0);
        tempPt.x = (2 * pt.y + pt.x) / 2;
        tempPt.y = (2 * pt.y - pt.x) / 2;
        return tempPt;
    }

    private Point realToIso(Point pt) {
        Point tempPt = new Point(0, 0);
        tempPt.x = pt.x - pt.y;
        tempPt.y = (pt.x + pt.y) / 2;
        return tempPt;
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

        // Create the paint, geometry and matrices used for drawing the tiles
        mTilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTilePaint.setStyle(Paint.Style.FILL);
        mTilePath = new Path();
        mTilePath.lineTo(TILE_WIDTH / 2, TILE_HEIGHT / 2);
        mTilePath.lineTo(0, TILE_HEIGHT);
        mTilePath.lineTo(-TILE_WIDTH / 2, TILE_HEIGHT / 2);
        mMatrix = new Matrix();
        mTranslateRowMatrix = new Matrix();
        mTranslateColMatrix = new Matrix();
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
        //Currently we do not properly try to handle these events, rather we use them as to aid debugging by forcing a complete redraw
        
        // Let the GestureDetector interpret this event
        boolean result = mDetector.onTouchEvent(event);

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
        invalidateAll();//debug force redraw
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
