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
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.jasperb.citybuilder.World;

/**
 * @author Jasper
 * 
 */
public class CityView extends View {

    /**
     * The initial fling velocity is divided by this amount.
     */

    public static final float MAXIMUM_SCALE_FACTOR = 1.f;
    public static final float MINIMUM_SCALE_FACTOR = 0.25f;
    public static final float BLOCK_WIDTH = 96;
    public static final float BLOCK_HEIGHT = BLOCK_WIDTH / 2;

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
    private World mWorld = null;
    private Paint mTilePaint;
    private boolean drawGridLines = false;
    private Path mTilePath;
    private Matrix mTranslateRowMatrix, mTranslateColMatrix, mMatrix;

    public void setWorld(World world) {
        mWorld = world;
        mTranslateRowMatrix.setTranslate(-BLOCK_WIDTH / 2, BLOCK_HEIGHT / 2);
        mTranslateColMatrix.setTranslate((BLOCK_WIDTH / 2) * (mWorld.getHeight() + 1), (-BLOCK_HEIGHT / 2) * (mWorld.getHeight() - 1));
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
     * {@link PieChart} from a set of XML attributes.
     * 
     * @param context
     * @param attrs
     *            An attribute set which can contain attributes from {@link com.example.android.customviews.R.styleable.PieChart} as well as
     *            attributes inherited from {@link android.view.View}.
     */
    public CityView(Context context, AttributeSet attrs) {
        super(context, attrs);

        init();
    }

    /**
     * Class constructor taking a context. This constructor is used by the layout engine to construct a {@link PieChart}.
     * 
     * @param context
     */
    public CityView(Context context) {
        super(context);

        init();
    }
/*
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // Do nothing. Do not call the superclass method--that would start a layout pass
        // on this view's children. 
    }
*/
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.BLACK);
        Log.d("CITYVIEW", "ONDRAW");
        if (mGroundCanvas == null || mWorld == null)
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
        mGroundCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);// Clear

        Log.d("CITYVIEW", "DRAW GROUND");
        //For the moment we're going to draw every tile, even those not in view
        mMatrix.setTranslate(mWidth / 2, 0);//don't account for mFocus yet
        mTilePath.transform(mMatrix);
        for (int col = 0; col < mWorld.getWidth(); col++) {
            for (int row = 0; row < mWorld.getHeight(); row++) {
                //Log.d("TEST","RC: " + row + " : " + col);
                switch (mWorld.getTerrain(row, col)) {
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
        mMatrix.setTranslate((-BLOCK_WIDTH / 2) * mWorld.getWidth() - mWidth / 2, (-BLOCK_HEIGHT / 2) * mWorld.getWidth());
        mTilePath.transform(mMatrix);//return mTilePath to origin (0,0)
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
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
     * Initialize the control. This code is in a separate method so that it can be called from both constructors.
     */
    private void init() {
        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        // Create a gesture detector to handle onTouch messages
        mDetector = new GestureDetector(this.getContext(), new GestureListener());

        // Turn off long press--this control doesn't use it, and if long press is enabled, you can't scroll for a bit, pause, then scroll
        // some more (the pause is interpreted as a long press, apparently)
        mDetector.setIsLongpressEnabled(false);

        mScaleDetector = new ScaleGestureDetector(this.getContext(), new ScaleListener());

        mTilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTilePaint.setStyle(Paint.Style.FILL);
        mTilePath = new Path();
        mTilePath.lineTo(BLOCK_WIDTH / 2, BLOCK_HEIGHT / 2);
        mTilePath.lineTo(0, BLOCK_HEIGHT);
        mTilePath.lineTo(-BLOCK_WIDTH / 2, BLOCK_HEIGHT / 2);
        new PathShape(mTilePath, BLOCK_WIDTH, BLOCK_HEIGHT);
        mMatrix = new Matrix();
        mTranslateRowMatrix = new Matrix();
        mTranslateColMatrix = new Matrix();
    }

    private void initCanvas() {
        // mGroundCanvasBitmap = Bitmap.createBitmap((int) (mWidth / MINIMUM_SCALE_FACTOR), (int) (mHeight / MINIMUM_SCALE_FACTOR),
        // Bitmap.Config.ARGB_8888);
        mGroundCanvasBitmap = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        mGroundCanvas = new Canvas(mGroundCanvasBitmap);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
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
        invalidateAll();
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

            constrainViewport();

            lastSpanX = spanX;
            lastSpanY = spanY;
            return true;
        }

        /**
         * Ensures that current viewport is inside the viewport extremes defined by {@link #AXIS_X_MIN}, {@link #AXIS_X_MAX},
         * {@link #AXIS_Y_MIN} and {@link #AXIS_Y_MAX}.
         */
        private void constrainViewport() {

        }
    };
}
